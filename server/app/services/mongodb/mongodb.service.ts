import DifferenceLocator from '@app/class/algorithms/difference-locator/difference-locator';
import { DifferenceImageData } from '@app/class/algorithms/difference-locator/difference-locator.constants';
import FileSystemManager from '@app/class/diverse/file-system-manager/file-system-manager';
import { getRandomBestTimes } from '@app/gateways/game.gateway.constants';
import { CardDocument } from '@app/model/database-schema/card.schema';
import { ChatDocument } from '@app/model/database-schema/chat.schema';
import { RecordDocument } from '@app/model/database-schema/history.schema';
import { User } from '@app/model/database-schema/user.schema';
import { PaddingRadius } from '@common/enums/game-creation/padding-radius';
import { Difficulty } from '@common/enums/game-play/difficulty';
import { Source } from '@common/enums/source';
import { Card } from '@common/interfaces/game-card/card';
import { CardPreview } from '@common/interfaces/game-card/card-preview';
import { Record } from '@common/interfaces/records/record';
import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import * as fs from 'fs';
import { Model } from 'mongoose';
import { User as UserInterface } from '@common/interfaces/user';

@Injectable()
export default class MongoDBService {
    constructor(
        @InjectModel(CardDocument.name) public cardModel: Model<CardDocument>,
        @InjectModel(RecordDocument.name) public recordModel: Model<RecordDocument>,
        @InjectModel(ChatDocument.name) private chatModel: Model<ChatDocument>,
    ) {
        this.start();
    }

    async start() {
        const localCardIds = fs.readdirSync('./assets/cards');
        const allIds = await this.cardModel.find({}).distinct<string>('_id');
        let validLocalCards = 0;
        loop: for (const localId of localCardIds) {
            for (const externalId of allIds)
                if (externalId.toString() === localId) {
                    validLocalCards++;
                    continue loop;
                }
            FileSystemManager.removeDirectory('./assets/cards/' + localId);
        }
        if (validLocalCards === 0) {
            FileSystemManager.removeDirectory('./assets/cards');
            FileSystemManager.createDirectory('./assets', 'cards');
            await this.populateDB();
        }
    }

    async populateDB() {
        const initialCardAmount = 5;
        const imagesPath = './assets/differencesPictures/';

        for (let i = 1; i <= initialCardAmount; i++) {
            const card: Card = {
                name: 'Game ' + i,
                classicSoloBestTimes: getRandomBestTimes(),
                classic1v1BestTimes: getRandomBestTimes(),
            } as Card;
            try {
                const differenceLocator = new DifferenceLocator(imagesPath + i + 'original.bmp', imagesPath + i + 'modified.bmp', Source.Path);
                if (!(await differenceLocator.findDifferences(PaddingRadius.THREE))) throw new Error('Error while generating differences');
                const data = differenceLocator.getCreationData();
                card.differenceNbr = data.differenceNbr;
                card.difficulty = data.difficulty;
                card.differences = data.finalDifferences;
                await this.addCard(card, data.imageData);
            } catch (e) {
                Logger.error(`Failed to generate a card : ${e}`);
            }
        }
    }

    async getAllCardIds(): Promise<string[]> {
        try {
            const cardIds = await this.cardModel.find({}, { id: 1 });
            for (let i = 0; i < cardIds.length; i++) {
                // eslint-disable-next-line no-underscore-dangle
                cardIds[i] = cardIds[i]._id.toString();
            }
            return cardIds as unknown as string[];
        } catch (e) {
            return Promise.reject(`Failed to get all card Ids : ${e}`);
        }
    }

    async getCardPreviewById(id: string): Promise<CardPreview> {
        try {
            const card = await this.cardModel.find({ id }, { name: 1, difficulty: 1, classicSoloBestTimes: 1, classic1v1BestTimes: 1, id: 1 });
            const finalCard: CardPreview = this.generateCardPreview(card as unknown as Card);
            return finalCard;
        } catch (e) {
            return Promise.reject(`Failed to get all frontend cards : ${e}`);
        }
    }

    async getAllCardPreviews(): Promise<CardPreview[]> {
        try {
            const allCards = await this.cardModel.find({}, { name: 1, difficulty: 1, classicSoloBestTimes: 1, classic1v1BestTimes: 1, id: 1 });
            const finalCards: CardPreview[] = [];
            for (const card of allCards) {
                try {
                    const finalCard: CardPreview = this.generateCardPreview(card as unknown as Card);
                    finalCards.push(finalCard);
                } catch (e) {
                    console.log(e);
                    //
                }
            }
            return finalCards;
        } catch (e) {
            console.log(e);
            return Promise.reject(`Failed to get all frontend cards : ${e}`);
        }
    }

    async getCardById(id: string): Promise<Card> {
        try {
            return await this.cardModel.findById(id);
        } catch (e) {
            return Promise.reject(`Failed to get a card with the id <${id}> : ${e}`);
        }
    }

    async addCard(cardToAdd: Card, imageData: DifferenceImageData): Promise<string> {
        try {
            if (cardToAdd.difficulty === Difficulty.None) return undefined;
            const newCard = await this.cardModel.create(cardToAdd);
            if (!FileSystemManager.storeCards(newCard.id, imageData)) this.removeCardById(newCard.id);
            else return newCard.id;
            return undefined;
        } catch (e) {
            return Promise.reject(`Failed to create a card : ${e}`);
        }
    }

    async removeCardById(id: string) {
        try {
            await this.cardModel.findByIdAndRemove(id);
            FileSystemManager.removeDirectory('./assets/cards/' + id);
        } catch (e) {
            return Promise.reject(`Failed to delete card: ${e}`);
        }
    }

    async removeAllCards() {
        try {
            await this.cardModel.deleteMany({});
            FileSystemManager.removeDirectory('./assets/cards');
            FileSystemManager.createDirectory('./assets', 'cards');
        } catch (e) {
            return Promise.reject(`Failed to delete cards : ${e}`);
        }
    }

    async removeRecordById(id: string) {
        try {
            await this.recordModel.remove(id);
        } catch (e) {
            return Promise.reject(`Failed to delete records : ${e}`);
        }
    }

    async removeAllRecords() {
        try {
            await this.recordModel.remove({});
        } catch (e) {
            return Promise.reject(`Failed to delete records : ${e}`);
        }
    }

    async modifyCard(cardId: string, cardUpdatedInfo: Card) {
        try {
            await this.cardModel.updateOne({ _id: cardId }, cardUpdatedInfo);
        } catch (e) {
            return Promise.reject(`Failed to update document : ${e}`);
        }
    }

    async addPlayerRecord(record: Record) {
        try {
            await this.recordModel.create(record);
        } catch (e) {
            return Promise.reject(`Failed to create a record : ${e}`);
        }
    }

    async getAllRecords(): Promise<Record[]> {
        try {
            return await this.recordModel.find({}).sort({ id: 1 });
        } catch (e) {
            return Promise.reject(`Failed to find all records : ${e}`);
        }
    }

    async resetAllBestTimes(): Promise<Card[]> {
        try {
            const updatedCardTimes: Card[] = [];
            const cardIds = await this.getAllCardIds();
            for (const cardId of cardIds) {
                const updateValues = {
                    classic1v1BestTimes: getRandomBestTimes(),
                    classicSoloBestTimes: getRandomBestTimes(),
                } as Card;
                this.modifyCard(cardId, updateValues);
                updateValues.id = cardId;
                updatedCardTimes.push(updateValues);
            }
            return updatedCardTimes;
        } catch (e) {
            return Promise.reject(`Failed to find all records : ${e}`);
        }
    }

    async resetBestTimesByCardId(cardId: string): Promise<Card> {
        try {
            const updateValues = {
                classic1v1BestTimes: getRandomBestTimes(),
                classicSoloBestTimes: getRandomBestTimes(),
            } as Card;
            await this.modifyCard(cardId, updateValues);
            return updateValues;
        } catch (e) {
            return Promise.reject(`Failed to find all records : ${e}`);
        }
    }
    async createChat(userIds: string[], name: string): Promise<ChatDocument> {
        const chat = new this.chatModel({
            name,
            userIds,
            messages: [],
        });
        return await chat.save();
    }
    async createPrivateChat(users: string[], name: string): Promise<ChatDocument> {
        const chat = new this.chatModel({
            name,
            userIds: users,
            private: true,
            messages: [],
        });
        return await chat.save();
    }
    async addUserToChat(chatId: string, userId: string): Promise<ChatDocument> {
        // add userId to userIds array
        return await this.chatModel.findByIdAndUpdate(chatId, { $addToSet: { userIds: userId } }, { new: true });
    }
    async removeUserFromChat(chatId: string, userId: string): Promise<ChatDocument> {
        return await this.chatModel.findByIdAndUpdate(chatId, { $pull: { userIds: userId } }, { new: true });
    }

    async findChatsByName(name: string): Promise<ChatDocument[]> {
        return await this.chatModel.find({ name });
    }
    async findAllChats(): Promise<ChatDocument[]> {
        return await this.chatModel.find({});
    }

    async addMessageToChat(chatId: string, sender: string, message: string): Promise<ChatDocument> {
        const chat = await this.chatModel.findById(chatId);
        console.log(chatId);
        if (!chat) {
            throw new Error('Chat not found');
        }

        chat.messages.push({
            sender,
            message,
            timestamp: new Date(),
            type: 'text',
        });
        return await chat.save();
    }
    async addVoiceMessageToChat(chatId: string, sender: string, audioId: string): Promise<ChatDocument> {
        const chat = await this.chatModel.findById(chatId);
        if (!chat) {
            throw new Error('Chat not found');
        }

        chat.messages.push({
            sender,
            message: audioId,
            timestamp: new Date(),
            type: 'audio',
        });
        return await chat.save();
    }

    async getChatById(chatId: string): Promise<ChatDocument> {
        return await this.chatModel.findById(chatId);
    }
    async findAllChatByUsername(username: string): Promise<ChatDocument[]> {
        return await this.chatModel.find({ users: { $elemMatch: { username } } });
    }
    async findAllChatByUserId(user: UserInterface): Promise<ChatDocument[]> {
        console.log(user);
        return await this.chatModel.find({ userIds: { $elemMatch: { $eq: user.userId.toString() } } });
    }

    private generateCardPreview(card: Card): CardPreview {
        return {
            name: card.name,
            classicSoloBestTimes: card.classicSoloBestTimes,
            classic1v1BestTimes: card.classic1v1BestTimes,
            difficulty: card.difficulty,
            originalImage: fs.readFileSync('./assets/cards/' + card.id + '/original-image.bmp', 'base64'),
            id: card.id,
        };
    }
}
