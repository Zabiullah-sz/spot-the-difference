/* eslint-disable no-underscore-dangle */
/* eslint-disable @typescript-eslint/member-ordering */
import UserSessionService from '@app/controller/users/user-session.service';
import GameAuthorityService from '@app/services/game-authority/game-authority.service';
import MongoDBService from '@app/services/mongodb/mongodb.service';

import FileSystemManager from '@app/class/diverse/file-system-manager/file-system-manager';
import { UsersService } from '@app/controller/users/users.service';
import { Chat, ChatMessage } from '@common/interfaces/chat';
import { User } from '@common/interfaces/user';
import * as Events from '@common/socket-event-constants';
import { Logger } from '@nestjs/common';
import { OnGatewayConnection, OnGatewayDisconnect, SubscribeMessage, WebSocketGateway, WebSocketServer } from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { GATEWAY_PORT } from './game.gateway.constants';
import OutputFilterGateway from './output-filters.gateway';

@WebSocketGateway(GATEWAY_PORT)
export default class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
    @WebSocketServer() server: Server;

    playersChatRooms: { [playerId: string]: string[] } = {}; // Store each player's chat rooms
    userDictionary: { [id: string]: string } = {};

    constructor(private mongoDBService: MongoDBService, private userSessionService: UserSessionService, private userService: UsersService) {
        GameAuthorityService.mongoDBService = this.mongoDBService;
        GameAuthorityService.userSessionService = this.userSessionService;
        GameAuthorityService.userService = this.userService;
    }

    // Join multiple chats (based on games the player is in) when the player logs in

    // When the player sends a chat message, broadcast to the appropriate room
    @SubscribeMessage(Events.ToServer.CREATE_CHAT)
    async handleStartChat(client: Socket, payload: { usernames: string[]; name: string }) {
        const userIds: string[] = [];
        for (const username of payload.usernames) {
            const user = await this.userService.getUserByUsername(username);
            if (!user) {
                OutputFilterGateway.sendNotification.toClient(client, { msg: 'User does not exist' });
                return;
            } else {
                userIds.push(user._id.toString());
            }
        }

        const chat = await this.mongoDBService.createChat(userIds, payload.name);
        // if chat was not created
        if (!chat) {
            OutputFilterGateway.sendNotification.toClient(client, { msg: 'Chat could not be created' });
            return;
        } else {
            const chatMsg = await this.createChatDocuement(chat);
            OutputFilterGateway.sendLoadChat.toClient(client, chatMsg);
        }
    }

    @SubscribeMessage(Events.ToServer.GET_ALL_CHATS)
    async handleGetAllChats(client: Socket, payload: { name: string }) {
        const user = this.userSessionService.getUser(client.id);

        // Fetch all chats based on the name search criteria (if provided)
        const allChats = payload.name ? await this.mongoDBService.findChatsByName(payload.name) : await this.mongoDBService.findAllChats();
        // Fetch all chats that the current user is part of
        const userChats = await this.mongoDBService.findAllChatByUserId(user);
        const userChatIds = userChats.map((chat) => chat._id.toString());

        // Filter out chats that the user is part of
        const chatsToReturn = allChats.filter((chat) => !userChatIds.includes(chat._id.toString()));
        // Return only the first 10 chats if no name was provided
        if (!payload.name && chatsToReturn.length > 10) {
            chatsToReturn.length = 10;
        }
        const chatMsgtoReturn = await Promise.all(
            chatsToReturn.map(async (chat) => {
                const chatMsg = await this.createChatDocuement(chat);
                return chatMsg;
            }),
        );
        OutputFilterGateway.sendAllChats.toClient(client, chatMsgtoReturn);
    }
    @SubscribeMessage(Events.ToServer.SEND_MESSAGE)
    async handleSendMessage(client: Socket, payload: { chatId: string; message: string; sender: string }) {
        if (typeof payload === 'string') {
            payload = JSON.parse(payload);
        }
        const chat = await this.mongoDBService.addMessageToChat(payload.chatId, payload.sender, payload.message);
        const chatMessage = chat.messages[chat.messages.length - 1];
        (chatMessage as ChatMessage).chatId = chat._id;
        chat.userIds.forEach((userId) => {
            const clientId = this.userSessionService.getClientIdbyUserId(userId);
            if (clientId) {
                const clientReceiver = this.server.sockets.sockets.get(clientId);
                if (clientReceiver) {
                    console.log('Sending message to client', clientReceiver.id);
                    OutputFilterGateway.sendNewMessage.toClient(clientReceiver, chatMessage);
                }
            }
        });
    }

    @SubscribeMessage(Events.ToServer.SEND_VOICE_MESSAGE)
    async handleVoiceMessage(client: Socket, payload: { chatId: string; message: Buffer; sender: string }) {
        console.log('Received voice message', payload.message);
        const audioId =
            new Date().getTime().toString() +
            Math.random()
                .toString()
                .match(/\.(\d+)/)[1];

        const saveSuccesful = await FileSystemManager.storeAudioBuffer(audioId, payload.message);
        if (!saveSuccesful) return;
        const chat = await this.mongoDBService.addVoiceMessageToChat(payload.chatId, payload.sender, audioId);
        const chatMessage = chat.messages[chat.messages.length - 1];
        (chatMessage as ChatMessage).chatId = chat._id;
        chat.userIds.forEach((userId) => {
            const clientId = this.userSessionService.getClientIdbyUserId(userId);
            if (clientId) {
                const clientReceiver = this.server.sockets.sockets.get(clientId);
                if (clientReceiver) {
                    OutputFilterGateway.sendNewMessage.toClient(clientReceiver, chatMessage);
                }
            }
        });
    }

    @SubscribeMessage(Events.ToServer.JOIN_CHAT)
    async handleJoinChat(client: Socket, payload: { chatId: string }) {
        const user = this.userSessionService.getUser(client.id);
        const userDoc = await this.userService.getUserbyId(user.userId);
        const chat = await this.mongoDBService.addUserToChat(payload.chatId, userDoc._id.toString());
        if (chat) {
            OutputFilterGateway.sendNotification.toClient(client, { msg: 'Chat joined' });
            const chatMsg = await this.createChatDocuement(chat);
            OutputFilterGateway.sendLoadChat.toClient(client, chatMsg);
            this.handleGetAllChats(client, { name: '' });
        }
    }
    @SubscribeMessage(Events.ToServer.REMOVE_CHAT)
    async handleRemoveChat(client: Socket, payload: { chatId: string }) {
        const user = this.userSessionService.getUser(client.id);
        await this.mongoDBService.removeUserFromChat(payload.chatId, user.userId);
    }

    @SubscribeMessage(Events.ToServer.LOAD_CHAT)
    async handleJoinChats(client: Socket) {
        const user = this.userSessionService.getUser(client.id);
        if (!user) return;
        const chats = await this.mongoDBService.findAllChatByUserId(user);
        chats.forEach(async (chat) => {
            const chatMsg = await this.createChatDocuement(chat);
            OutputFilterGateway.sendLoadChat.toClient(client, chatMsg);
        });
    }
    handleConnection(client: Socket) {
        Logger.log('A client has connected: ' + client.id);

        const chatRooms = this.playersChatRooms[client.id] || [];
        chatRooms.forEach((roomId) => {
            client.join(roomId);
        });
    }
    async createChatDocuement(chat) {
        const chatUsersPromises = chat.userIds.map(async (userId) => {
            const userChat = await this.userService.getUserbyId(userId);
            if (!userChat) return;
            return { userId, username: userChat.username } as User;
        });

        const chatUsers = await Promise.all(chatUsersPromises);
        chatUsers.forEach((user, index) => {
            if (!user) {
                chatUsers.splice(index, 1);
            }
        });
        const chatMsg = {
            _id: chat._id.toString(),
            users: chatUsers,
            messages: chat.messages,
            private: chat.private,
            name: chat.name,
        } as Chat;
        return chatMsg;
    }
    handleDisconnect(client: Socket) {
        GameAuthorityService.removePlayer(client.id);
        this.userSessionService.removePlayer(this.userDictionary[client.id]);
        OutputFilterGateway.sendAllActiveUsers.toServer(this.userSessionService.getAllActiveUsers());
        Logger.log('A client has disconnected: ' + client.id);
    }
}
