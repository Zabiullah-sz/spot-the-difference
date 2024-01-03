/* eslint-disable max-lines */
/* eslint-disable no-console */
import BestTimesModifier from '@app/class/diverse/best-times-modifier/best-times-modifier';
import FileSystemManager from '@app/class/diverse/file-system-manager/file-system-manager';
import DifferenceManager from '@app/class/game-logic/difference-manager/difference-manager';
import Game from '@app/class/game-logic/game-interfaces/game-interface';
import Player from '@app/class/game-logic/player/player';
import PlayerGroup from '@app/class/player-groups/default-player-group/player-group';
import DuoPlayerGroup from '@app/class/player-groups/duo-player-group/duo-player-group';
import StopWatch from '@app/class/watch/stopwatch/stopwatch';
import Timer from '@app/class/watch/timer/timer';
import { oneSecond } from '@app/class/watch/watch/watch.constants';
import UserSessionService from '@app/controller/users/user-session.service';
import { UsersService } from '@app/controller/users/users.service';
import { GameConnectionData, UserGame } from '@app/gateways/game.gateway.constants';
import OutputFilterGateway from '@app/gateways/output-filters.gateway';
import { INGAME_MAX_PLAYER } from '@app/model/gateway-dto/game/game.constants';
import GameAuthorityService from '@app/services/game-authority/game-authority.service';
import MongoDBService from '@app/services/mongodb/mongodb.service';
import { GameConnectionAttemptResponseType } from '@common/enums/game-play/game-connection-attempt-response-type';
import { GameMode } from '@common/enums/game-play/game-mode';
import { PlayerConnectionStatus } from '@common/enums/game-play/player-connection-status';
import { BestTime } from '@common/interfaces/game-card/best-time';
import { BestTimes } from '@common/interfaces/game-card/best-times';
import { Card } from '@common/interfaces/game-card/card';
import { GameClickOutputDto, GameDifferenceImages } from '@common/interfaces/game-play/game-click.dto';
import { GameConnectionRequestOutputMessageDto } from '@common/interfaces/game-play/game-connection-request.dto';
import { EndgameOutputDto } from '@common/interfaces/game-play/game-endgame.dto';
import { SimpleUser } from '@common/interfaces/game-play/simple-user';
import { Coordinates } from '@common/interfaces/general/coordinates';
import { Logger } from '@nestjs/common';

export default class Classic1v1 extends Game {
    protected waitingLobby: PlayerGroup;
    protected stopwatch: StopWatch;

    constructor(mongodbService: MongoDBService, userService: UsersService, userSessionService: UserSessionService) {
        super(mongodbService, userService, userSessionService);
        this.gameValues = JSON.parse(JSON.stringify(GameAuthorityService.gameValues));
        this.gameMode = GameMode.Classic1v1;
    }

    waitingLobbyIds(): string[] {
        return [this.waitingLobby.getLobbyId];
    }

    findWaitingPlayer(playerId: string): Player | undefined {
        return this.waitingLobby.getPlayer(playerId);
    }

    async initialize(data: GameConnectionData): Promise<boolean> {
        const maxWaitingPlayers = 10;
        try {
            this.card = await this.mongodbService.getCardById(data.cardId);
        } catch (e) {
            this.card = undefined;
            Logger.error(e);
        }
        onCardReception: if (this.card) {
            this.cardId = data.cardId;
            this.id = data.user.client.id;
            this.cardFiles = FileSystemManager.getImages(this.card);
            if (!this.cardFiles) break onCardReception;
            this.playerGroup = new PlayerGroup(2, INGAME_MAX_PLAYER);
            this.playerGroup.joinUser(data.user);
            this.waitingLobby = new PlayerGroup(0, maxWaitingPlayers);
            console.log('helololo', this.playerGroup);

            const waitingMessage: GameConnectionRequestOutputMessageDto = {
                responseType: GameConnectionAttemptResponseType.Pending,
                gameId: this.id,
                playerNbr: 1,
                isSettingSpecified: this.isSettingDone,
            } as GameConnectionRequestOutputMessageDto;
            OutputFilterGateway.sendConnectionAttemptResponseMessage.toClient(data.user.client, waitingMessage);
            GameAuthorityService.getPendingGames.addGame(this);
            console.log('these are the pending games', GameAuthorityService.getPendingGames);
            return true;
        }
        OutputFilterGateway.sendConnectionAttemptResponseMessage.toClient(data.user.client, {
            responseType: GameConnectionAttemptResponseType.Cancelled,
            gameId: this.id,
        } as GameConnectionRequestOutputMessageDto);
        return false;
    }

    async initializeToChat(data: GameConnectionData): Promise<boolean> {
        const maxWaitingPlayers = 10;
        try {
            this.card = await this.mongodbService.getCardById(data.cardId);
        } catch (e) {
            this.card = undefined;
            Logger.error(e);
        }
        // eslint-disable-next-line no-unused-labels
        onCardReception: if (this.card) {
            this.cardId = data.cardId;
            this.id = data.user.client.id;
            this.playerGroup = new DuoPlayerGroup(data.user);
            this.waitingLobby = new PlayerGroup(0, maxWaitingPlayers);
            GameAuthorityService.getPendingGames.addGame(this);
            return true;
        }
        return false;
    }

    startGame(clientIds: string[]) {
        for (const clientId of clientIds) {
            if (!this.waitingLobby.transferPlayerTo(clientId, this.playerGroup)) return;
        }

        this.waitingLobby.forEachPlayer((player: Player) => {
            OutputFilterGateway.sendConnectionAttemptResponseMessage.toClient(player.client, {
                responseType: GameConnectionAttemptResponseType.Rejected,
                gameId: this.id,
            } as GameConnectionRequestOutputMessageDto);
            return false;
        });
        this.waitingLobby.empty();
        GameAuthorityService.startGame(this.id, this.cardId);
        this.isOngoing = true;
        const startMessage: GameConnectionRequestOutputMessageDto = {
            responseType: GameConnectionAttemptResponseType.Starting,
            gameName: this.card.name,
            startingIn: 0,
            modifiedImage: this.cardFiles.modifiedImage,
            originalImage: this.cardFiles.originalImage,
            gameId: this.id,
            difficulty: this.card.difficulty,
            time: 0,
            differenceNbr: this.card.differenceNbr,
            playerNbr: this.playerGroup.getPlayerNbr,
            hostName: this.playerGroup.host.name,
            gameValues: this.gameValues,
            players: [],
            isSettingSpecified: this.isSettingDone,
        };
        this.playerGroup.forEachPlayer((player: Player) => {
            player.differenceManager = new DifferenceManager(this.card, this.cardFiles);
            startMessage.players.push(player.name);
            return false;
        });
        OutputFilterGateway.sendConnectionAttemptResponseMessage.toLobby(this.playerGroup.getLobbyId, startMessage);
        this.gameWatch = new Timer();
        this.stopwatch = new StopWatch();
        this.stopwatch.start();
        this.gameWatch.eachInterval = () => OutputFilterGateway.sendTime.toLobby(this.playerGroup.getLobbyId, this.gameWatch.getSeconds);
        this.gameWatch.start();
        this.gameWatch.onEnd = async () => {
            const playerWithMostDifferences = this.playerGroup.getPlayerWithMostDifferences();
            this.endGame(playerWithMostDifferences);
        };
        this.gameWatch.set(this.gameValues.timerTime);
    }

    join(user: UserGame): boolean {
        if (!this.isOngoing) {
            const joinedWaitingLobby = this.waitingLobby.joinUser(user);
            if (joinedWaitingLobby) {
                OutputFilterGateway.sendConnectionAttemptResponseMessage.toClient(user.client, {
                    responseType: GameConnectionAttemptResponseType.Pending,
                    gameId: this.id,
                    isSettingSpecified: true,
                } as GameConnectionRequestOutputMessageDto);
                OutputFilterGateway.sendPlayerConnectionMessage.toClient(this.playerGroup.host.client, {
                    playerConnectionStatus: PlayerConnectionStatus.AttemptingToJoin,
                    user: {
                        name: user.name,
                        id: user.client.id,
                    } as SimpleUser,
                });
            }
            return joinedWaitingLobby;
        }
        return false;
    }

    async endGame(winner?: Player) {
        if (this.isOngoing) {
            GameAuthorityService.getOngoingGames.removeGame(this.id);
            this.isOngoing = false;
            this.gameWatch.pause();
            this.stopwatch.pause();
            const endMessage: EndgameOutputDto = {
                finalTime: this.stopwatch.getSeconds,
                players: this.getPlayerList(winner ? [winner] : []),
            };
            if (winner) {
                const newBestTime: BestTime = {
                    name: winner.name,
                    time: this.stopwatch.getTime,
                };
                const playersGroup = this.playerGroup.getPlayers;
                for (const player of playersGroup) {
                    const clientId = player.client.id;
                    const userId = this.userSessionService.getUser(clientId).userId;
                    await this.userService.addPoints(userId, player.differencesFound);
                }
                const card = await this.mongodbService.getCardById(this.cardId);
                if (card) {
                    const bestTimeId = BestTimesModifier.updateBestTimes(card.classic1v1BestTimes, newBestTime);
                    if (bestTimeId) {
                        endMessage.newBestTimes = card.classic1v1BestTimes as BestTimes;
                        OutputFilterGateway.sendGlobalMessage.toServer(
                            winner.name + ' obtient la ' + bestTimeId + ' place dans les meilleurs temps du jeu ' + card.name + ' en Classique 1v1',
                        );
                        OutputFilterGateway.sendRecordBeaterMessage.toLobby(
                            this.playerGroup.getLobbyId,
                            'Avec cette victoire, ' +
                                winner.name +
                                ' obtient la ' +
                                bestTimeId +
                                ' place dans les meilleurs temps du jeu ' +
                                card.name +
                                ' en Classique 1v1',
                        );

                        const newTimes: Card = { classic1v1BestTimes: card.classic1v1BestTimes } as unknown as Card;
                        this.mongodbService
                            .modifyCard(this.cardId, newTimes)
                            .then(() => {
                                newTimes.id = this.cardId;
                                OutputFilterGateway.sendCardTimes.toServer(newTimes);
                            })
                            .catch((e) => Logger.error(e));
                    }
                }
            }
            console.log('Game ended ok ok ok ok');
            OutputFilterGateway.sendEndgameMessage.toLobby(this.playerGroup.getLobbyId, endMessage);
            const record = {
                players: endMessage.players,
                startDate: this.startTime,
                duration: this.stopwatch.getTime,
                gameMode: this.gameMode,
            };
            this.mongodbService
                .addPlayerRecord(record)
                .then(() => OutputFilterGateway.sendRecord.toServer(record))
                .catch((e) => Logger.error(e));
        } else {
            GameAuthorityService.removeJoinableGames(this.cardId);
            GameAuthorityService.getPendingGames.removeGame(this.id);
            const cancelMessage = {
                responseType: GameConnectionAttemptResponseType.Cancelled,
                gameId: this.id,
            } as GameConnectionRequestOutputMessageDto;
            OutputFilterGateway.sendConnectionAttemptResponseMessage.toLobby(this.playerGroup.getLobbyId, cancelMessage);
            OutputFilterGateway.sendConnectionAttemptResponseMessage.toLobby(this.waitingLobby.getLobbyId, cancelMessage);
        }
        this.playerGroup.empty();
        this.waitingLobby.empty();
    }

    kickWaitingPlayer(playerId: string) {
        const player = this.waitingLobby.leave(playerId, false);
        if (player) {
            OutputFilterGateway.sendConnectionAttemptResponseMessage.toClient(player.client, {
                responseType: GameConnectionAttemptResponseType.Rejected,
                gameId: this.id,
            } as GameConnectionRequestOutputMessageDto);
            OutputFilterGateway.sendPlayerConnectionMessage.toClient(this.playerGroup.host.client, {
                playerConnectionStatus: PlayerConnectionStatus.Left,
                user: {
                    name: player.name,
                    id: player.client.id,
                },
            });
        }
    }

    async removePlayer(playerId: string, deserter?: boolean): Promise<boolean> {
        let removedPlayer = this.playerGroup.leave(playerId, this.isOngoing);
        if (removedPlayer) {
            if (this.isOngoing && deserter !== false)
                OutputFilterGateway.sendDeserterMessage.toLobby(this.playerGroup.getLobbyId, removedPlayer.name);
            if (this.playerGroup.getPlayerNbr <= 1) this.endGame();
            else {
                OutputFilterGateway.sendPlayerConnectionMessage.toLobby(this.playerGroup.getLobbyId, {
                    playerConnectionStatus: PlayerConnectionStatus.Left,
                    user: {
                        name: removedPlayer.name,
                        id: removedPlayer.client.id,
                    },
                });
            }
            return true;
        } else if (!this.isOngoing) {
            removedPlayer = this.waitingLobby.leave(playerId, false);
            if (removedPlayer) {
                OutputFilterGateway.sendPlayerConnectionMessage.toClient(this.playerGroup.host.client, {
                    playerConnectionStatus: PlayerConnectionStatus.Left,
                    user: {
                        name: removedPlayer.name,
                        id: removedPlayer.client.id,
                    },
                });
            }
        }
        return removedPlayer !== undefined;
    }

    canEndGameEarly(): boolean {
        const totalDifferences = this.playerGroup.players[0].differenceManager.originalDifferenceAmount;
        const leadingPlayer = this.getLeadingPlayer();
        const leadingScore = leadingPlayer.differencesFound;

        // Calculate the total points already earned by all players
        let totalEarnedPoints = 0;
        for (const player of this.playerGroup.players) {
            totalEarnedPoints += player.differencesFound;
        }

        // Calculate the remaining points that can be earned
        const remainingPoints = totalDifferences - totalEarnedPoints;

        // Check if any other player can surpass the leading player's score
        for (const player of this.playerGroup.players) {
            if (player !== leadingPlayer) {
                const potentialMaxScore = player.differencesFound + remainingPoints;
                if (potentialMaxScore > leadingScore) {
                    return false; // This player can still surpass the leading player
                }
            }
        }

        return true; // No other player can surpass the leading player
    }

    getLeadingPlayer(): Player {
        return this.playerGroup.players.reduce((leading, player) => (player.differencesFound > leading.differencesFound ? player : leading));
    }

    verifyClick(playerId: string, clickCoordinates: Coordinates): boolean {
        return super.verifyClick(playerId, clickCoordinates, (foundDifferenceValues: GameDifferenceImages, player: Player) => {
            if (foundDifferenceValues) {
                const validClickResponse: GameClickOutputDto = {
                    valid: true,
                    differenceNaturalOverlay: foundDifferenceValues.differenceNaturalOverlay,
                    differenceFlashOverlay: foundDifferenceValues.differenceFlashOverlay,
                };
                this.playerGroup.forEachPlayer((p: Player): boolean => {
                    p.differenceManager.removeDifferenceByIndex(foundDifferenceValues.index, false);
                    return false;
                });
                OutputFilterGateway.sendClickResponseMessage.toClient(player.client, validClickResponse);
                OutputFilterGateway.sendOtherClick.broadcast(
                    player.client,
                    {
                        playerName: player.name,
                        valid: true,
                        differenceNaturalOverlay: foundDifferenceValues.differenceNaturalOverlay,
                        differenceFlashOverlay: foundDifferenceValues.differenceFlashOverlay,
                    },
                    this.playerGroup.getLobbyId,
                );
                player.differencesFound++;
                OutputFilterGateway.sendCheatIndex.toLobby(this.playerGroup.getLobbyId, foundDifferenceValues.index);
                if (this.canEndGameEarly()) {
                    this.endGame(this.getLeadingPlayer());
                }
                return true;
            }
            OutputFilterGateway.sendOtherClick.broadcast(
                player.client,
                {
                    playerName: player.name,
                    valid: false,
                },
                this.playerGroup.getLobbyId,
            );
            const invalidClickResponse: GameClickOutputDto = {
                valid: false,
                penaltyTime: oneSecond,
            };
            OutputFilterGateway.sendClickResponseMessage.toClient(player.client, invalidClickResponse);
            player.startPenalty(oneSecond);
            return false;
        });
    }
}
