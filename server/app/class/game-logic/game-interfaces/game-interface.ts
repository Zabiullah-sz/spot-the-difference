import Player from '@app/class/game-logic/player/player';
import PlayerGroup from '@app/class/player-groups/default-player-group/player-group';
import Watch from '@app/class/watch/watch/watch';
import UserSessionService from '@app/controller/users/user-session.service';
import { UsersService } from '@app/controller/users/users.service';
import { GameConnectionData, UserGame } from '@app/gateways/game.gateway.constants';
import OutputFilterGateway from '@app/gateways/output-filters.gateway';
import { PlayerRecordDocument } from '@app/model/database-schema/player-record.schema';
import MongoDBService from '@app/services/mongodb/mongodb.service';
import { GameMode } from '@common/enums/game-play/game-mode';
import { Hint } from '@common/interfaces/difference-locator-algorithm/hint';
import { Card } from '@common/interfaces/game-card/card';
import { CardBase64Files } from '@common/interfaces/game-card/card-base64-files';
import { GameDifferenceImages } from '@common/interfaces/game-play/game-click.dto';
import { GameConnectionObserverResponseTypeDto } from '@common/interfaces/game-play/game-connection-request.dto';
import { GameValues } from '@common/interfaces/game-play/game-values';
import { Coordinates } from '@common/interfaces/general/coordinates';
export default abstract class Game {
    voteRecord: { [cheater: string]: { yes: number; no: number; votedBy: Set<string> } } = {};
    gameValues: GameValues;
    isSettingDone = false;
    playerGroup: PlayerGroup;
    protected gameMode: GameMode;
    protected id: string;
    protected isOngoing = false;
    protected gameWatch: Watch;
    protected startTime: string = Date();
    protected cardId: string;
    protected card: Card;
    protected cardFiles: CardBase64Files;

    constructor(protected mongodbService: MongoDBService, protected userService: UsersService, protected userSessionService: UserSessionService) {}

    get host() {
        if (this.playerGroup) return this.playerGroup.host;
        return undefined;
    }

    get getCardId() {
        return this.cardId;
    }

    get getGameMode(): GameMode {
        return this.gameMode;
    }
    get getId() {
        return this.id;
    }

    get getIsOngoing() {
        return this.isOngoing;
    }
    get getPlayers() {
        return this.playerGroup.getPlayersAsUser;
    }

    get getPlayersAsPlayer() {
        return this.playerGroup.getPlayers;
    }

    get getObservers() {
        return this.playerGroup.getObservers;
    }

    async initialize?(data: GameConnectionData): Promise<boolean>;

    startGame?(clientIds?: string[]): void;
    addObserver(user: UserGame): boolean {
        this.playerGroup.joinObserver(user);
        return true;
    }
    removeObserver(playerId: string): boolean {
        return this.playerGroup.removeObserver(playerId);
    }
    async endGame?(winner?: Player): Promise<void>;

    join?(user: UserGame): boolean;

    async removePlayer?(playerId: string, deserter?: boolean): Promise<boolean>;

    async processVote(cheaterName: string, voter: string, playerVote: string): Promise<boolean> {
        const players = this.getPlayersAsPlayer;
        const playerNbr = players.length;

        // Initialize the vote record for this cheater if it doesn't exist
        if (!this.voteRecord[cheaterName]) {
            this.voteRecord[cheaterName] = { yes: 0, no: 0, votedBy: new Set() };
        }

        // Check if the player has already voted
        if (this.voteRecord[cheaterName].votedBy.has(voter)) {
            return false;
        }

        // Record the vote
        this.voteRecord[cheaterName][playerVote]++;
        this.voteRecord[cheaterName].votedBy.add(voter);

        // Find the cheater's ID
        const cheater = players.find((player) => player.name === cheaterName);
        const cheaterId = cheater ? cheater.client.id : null;

        // If cheater is not found, exit the function
        if (!cheaterId) return false;

        // Check vote results and take action
        let cheaterRemoved = false;
        const yesVoteNb = this.voteRecord[cheaterName].yes;
        const noVoteNb = this.voteRecord[cheaterName].no;
        const totalVote = noVoteNb + yesVoteNb;
        if (yesVoteNb >= (playerNbr - 1) / 2) {
            // Remove the cheater from the game
            await this.removePlayer(cheaterId, false);
            OutputFilterGateway.sendCheaterBackToLobby.toClient(cheater.client, 'back_to_lobby');

            cheaterRemoved = true;
            delete this.voteRecord[cheaterName];
        } else if (noVoteNb >= (playerNbr - 1) / 2 && totalVote === playerNbr - 1) {
            cheaterRemoved = false;
            delete this.voteRecord[cheaterName];
        }

        return cheaterRemoved;
    }

    verifyClick(playerId: string, clickCoordinates: Coordinates, cb?: (found: GameDifferenceImages, player?: Player) => boolean): boolean {
        if (!this.isOngoing) return false;
        return this.playerGroup.forEachPlayer((player: Player) => {
            const matchingId = player.client.id === playerId;
            if (matchingId && !player.downTime) {
                const foundDifferenceValues = player.differenceManager.findDifference(clickCoordinates);
                if (cb(foundDifferenceValues, player)) return true;
            }
        });
    }

    findPlayer(playerId: string): Player | undefined {
        return this.playerGroup.getPlayer(playerId);
    }

    waitingLobbyIds?(): string[];

    findWaitingPlayer?(playerId: string): Player | undefined;

    findObserver(playerId: string): Player | undefined {
        return this.playerGroup.getObserver(playerId);
    }

    getLobbyIds(): string[] {
        return [this.playerGroup.getLobbyId];
    }

    getPlayerList(winners: Player[]): PlayerRecordDocument[] {
        const players = [];
        const existingWinners = winners.length !== 0;
        this.playerGroup.forEachPlayer((player: Player) => {
            let isAWinner = false;
            if (existingWinners)
                for (const winner of winners) {
                    if (winner)
                        if (winner.client.id === player.client.id) {
                            isAWinner = true;
                            break;
                        }
                }
            else isAWinner = true;
            players.push({
                name: player.name,
                winner: isAWinner,
                userId: player.userId,
                deserter: false,
                noDifferenceFound: player.differencesFound,
            } as PlayerRecordDocument);
            return false;
        });
        this.playerGroup.getDeserters.forEach((deserter: Player) => {
            players.push({
                name: deserter.name,
                userId: deserter.userId,
                winner: false,
                deserter: true,
            } as PlayerRecordDocument);
        });
        return players;
    }

    getHint(playerId: string): Hint {
        const player = this.findPlayer(playerId);
        if (player) {
            const hint = player.differenceManager?.hint;
            if (hint) {
                switch (this.gameMode) {
                    case GameMode.ClassicSolo:
                        this.gameWatch.add(this.gameValues.penaltyTime);
                        break;
                    case GameMode.Classic1v1:
                        break;
                    case GameMode.LimitedTimeCoop:
                        break;
                    case GameMode.LimitedTimeSolo:
                        this.gameWatch.remove(this.gameValues.penaltyTime);
                        break;
                }
            }
            return hint;
        }
    }
    // all getData to create: GameConnectionRequestOutputMessageDto
    getDataForObserver(): GameConnectionObserverResponseTypeDto {
        const message = {
            gameMode: this.gameMode,
            responseType: 'success',
            gameName: this.card.name,
            playerNbr: this.playerGroup.getPlayerNbr,
            startingIn: 0,
            originalImage: this.cardFiles.originalImage,
            modifiedImage: this.cardFiles.modifiedImage,
            time: 0,
            gameId: this.id,
            difficulty: this.card.difficulty,
            differenceNbr: this.card.differenceNbr,
            hostName: this.host.name,
            gameValues: this.gameValues,
            players: this.playerGroup.getPlayersAsUser.map((player) => player.username),
        };
        return message;
    }
    newObserver?(): void;
}
