/* eslint-disable max-lines */
import Classic1v1 from '@app/class/game-logic/classic1v1/classic1v1';
import Player from '@app/class/game-logic/player/player';
import UserSessionService from '@app/controller/users/user-session.service';
import { ChatMessageFilter } from '@app/model/gateway-dto/game/chat-message.dto';
import { GameClickFilter } from '@app/model/gateway-dto/game/game-click.dto';
import { GameConnectionRequestFilter } from '@app/model/gateway-dto/game/game-connection-request.dto';
import { GameValuesInputFilter } from '@app/model/gateway-dto/game/game-values.dto';
import GameAuthorityService from '@app/services/game-authority/game-authority.service';
import MongoDBService from '@app/services/mongodb/mongodb.service';
import { GameMode } from '@common/enums/game-play/game-mode';
import { ChatMessageOutputDto } from '@common/interfaces/game-play/chat-message.dto';
import { CheaterVote } from '@common/interfaces/game-play/cheater-vote';
import { PlayerValidationDto } from '@common/interfaces/game-play/game-player-validation.dto';
import { User } from '@common/interfaces/user';
import * as Events from '@common/socket-event-constants';
import { Logger } from '@nestjs/common';
import { OnGatewayConnection, OnGatewayDisconnect, SubscribeMessage, WebSocketGateway, WebSocketServer } from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { GATEWAY_PORT, GameConnectionData, UserGame } from './game.gateway.constants';
import OutputFilterGateway from './output-filters.gateway';

@WebSocketGateway(GATEWAY_PORT)
export default class GameGateway implements OnGatewayDisconnect, OnGatewayConnection {
    @WebSocketServer() server: Server;

    playersID: string[] = [];
    userDictionary: { [id: string]: string } = {};

    constructor(private mongoDBService: MongoDBService, private userSessionService: UserSessionService) {
        GameAuthorityService.mongoDBService = this.mongoDBService;
        GameAuthorityService.userSessionService = this.userSessionService;
    }

    @SubscribeMessage(Events.ToServer.IS_PLAYING)
    isPlaying(client: Socket) {
        const result = GameAuthorityService.getOngoingGames.findGameByPlayerId(client.id) !== undefined;
        OutputFilterGateway.sendPlayerStatus.toClient(client, result);
    }

    /**
     * Verifies the identity of the client , then verifies if his click hit a difference or not and
     * finally informs him and everyone in his game of the result.
     *
     * @param client - The socket of the client who sent the event
     * @param input - The coordinates of the click on the game canvas by the client
     */
    @SubscribeMessage(Events.ToServer.CLICK)
    verifyClick(client: Socket, input: GameClickFilter) {
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        const game = GameAuthorityService.getOngoingGames.findGame(input.gameId);
        if (!game) return;
        const player = game.findPlayer(client.id);
        if (!player || player.downTime === true) return;
        game.verifyClick(client.id, { x: input.x, y: input.y });
    }

    /**
     * Verifies the identity of the client, than sends the message to the other players in his game.
     *
     * @param client - The socket of the client who sent the event
     * @param input - the message sent by the client to the other players in his game
     */
    @SubscribeMessage(Events.ToServer.CHAT_MESSAGE)
    sendChatMessage(client: Socket, input: ChatMessageFilter) {
        console.log('test');
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        console.log(input);
        let game = GameAuthorityService.getOngoingGames.findGame(input.gameId);
        let player = game?.findPlayer(client.id);
        let lobbies = [];
        if (game === undefined) {
            game = GameAuthorityService.getPendingGames.findGame(input.gameId);
            player = game?.findPlayer(client.id);
            if (player === undefined) player = game?.findWaitingPlayer(client.id);
            lobbies = game.waitingLobbyIds();
            for (const lobbyId of lobbies) {
                OutputFilterGateway.sendChatMessage.broadcast(
                    client,
                    {
                        sender: player.name,
                        message: input.message,
                        time: input.time,
                    } as ChatMessageOutputDto,
                    lobbyId,
                );
            }
            if (game.getLobbyIds()[0] !== lobbies[0]) {
                for (const lobbyId of game.getLobbyIds()) {
                    OutputFilterGateway.sendChatMessage.broadcast(
                        client,
                        {
                            sender: player.name,
                            message: input.message,
                            time: input.time,
                        } as ChatMessageOutputDto,
                        lobbyId,
                    );
                }
            }
            return;
        }
        lobbies = game.getLobbyIds();
        const observer = game?.findObserver(client.id);
        if (!player && !observer) return;
        if (!player && observer) {
            OutputFilterGateway.sendChatMessage.broadcast(
                client,
                {
                    sender: observer.name,
                    message: input.message,
                    time: input.time,
                } as ChatMessageOutputDto,
                lobbies[0],
            );
            return;
        }

        for (const lobbyId of game.getLobbyIds()) {
            OutputFilterGateway.sendChatMessage.broadcast(
                client,
                {
                    sender: player.name,
                    message: input.message,
                    time: input.time,
                } as ChatMessageOutputDto,
                lobbyId,
            );
        }
    }

    @SubscribeMessage(Events.ToServer.PROTOTYPE_CHAT_MESSAGE)
    sendPrototypeChatMessage(client: Socket, input: ChatMessageFilter) {
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        OutputFilterGateway.sendChatMessage.broadcast(
            client,
            {
                sender: this.userDictionary[client.id],
                message: input.message,
                time: input.time,
            } as ChatMessageOutputDto,
            this.playersID[0] + 'L',
        );
        // }
    }

    /**
     * Parses the data sent by the client to a minimalist format and attempts to connect him to a
     * game.
     *
     * @param client - The socket of the client who sent the event
     * @param input - The necessary information for the client to connect to a game
     */
    @SubscribeMessage(Events.ToServer.REQUEST_TO_PLAY)
    joinGame(client: Socket, input: GameConnectionRequestFilter) {
        Logger.log('Attempting to connect client: ' + client.id);
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        const userId = this.userSessionService.getUser(client.id).userId;

        const data: GameConnectionData = {
            gameMode: input.gameMode,
            cardId: input.cardId,
            user: {
                name: input.playerName,
                client,
                userId,
            } as UserGame,
            startEarly: input.startEarly,
        };
        GameAuthorityService.connect(data);
    }

    @SubscribeMessage(Events.ToServer.USERNAME_VALIDATION)
    usernameAvailability(client: Socket, input: User) {
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        const isUsernameAvailable = this.userSessionService.isUsernameAvailable(client.id, input);
        OutputFilterGateway.sendValidationResponse.toClient(client, isUsernameAvailable);
        if (isUsernameAvailable) OutputFilterGateway.sendAllActiveUsers.toServer(this.userSessionService.getAllActiveUsers());
    }
    @SubscribeMessage(Events.ToServer.UPDATE_USERNAME)
    updateUsername(client: Socket, input: User) {
        const isUsernameAvailable = this.userSessionService.updateUsername(client.id, input);
        if (isUsernameAvailable) OutputFilterGateway.sendAllActiveUsers.toServer(this.userSessionService.getAllActiveUsers());
    }

    @SubscribeMessage(Events.ToServer.REQUEST_TO_CHAT)
    joinChat(client: Socket, input: GameConnectionRequestFilter) {
        Logger.log('Attempting to connect client: ' + client.id);
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        const userId = this.userSessionService.getUser(client.id).userId;
        const data: GameConnectionData = {
            gameMode: input.gameMode,
            cardId: input.cardId,
            user: {
                name: input.playerName.trim(),
                client,
                userId,
            },
        } as GameConnectionData;
        let lobbyID = '';
        lobbyID = this.playersID[0] + 'L';
        if (this.playersID.length === 0) {
            lobbyID = client.id + 'L';
        }
        this.userDictionary[client.id] = data.user.name;
        const newPlayer = new Player(data.user);
        newPlayer.client.join(lobbyID);
        this.playersID.push(client.id);
    }

    @SubscribeMessage(Events.ToServer.REMOVE_PLAYER)
    leaveChat(client: Socket) {
        this.userSessionService.removePlayer(this.userDictionary[client.id]);
    }

    /**
     * Verifies the identity of the client, than kicks a player from a waiting lobby or makes him
     * join a game depending on the host's decision
     *
     * @param client - The socket of the client who sent the event
     * @param input - The necessary information to accept or reject a player from a game
     */
    @SubscribeMessage(Events.ToServer.PLAYER_VALIDATION)
    async validatePlayer(client: Socket, input: PlayerValidationDto) {
        if (typeof input === 'string') {
            input = JSON.parse(input);
        }
        const game = GameAuthorityService.getPendingGames.findGame(input.gameId);
        if (!game || game.getGameMode !== GameMode.Classic1v1 || game.host?.client.id !== client.id) return;
        if (!input.canJoin) (game as Classic1v1).kickWaitingPlayer(input.playersId[0]);
        else game.startGame(input.playersId);
    }

    /**
     * Starts by verifying the identity of the player before sending him the data, then sends him an array
     * with the length of the available differences and the yet to find differences with their appropriate
     * indexes.
     *
     * @param client - The socket of the client who sent the event
     * @param gameId - The id of the game in which the client plays
     */
    @SubscribeMessage(Events.ToServer.CHEAT)
    async sendCheatFlashes(client: Socket, gameId: string) {
        const game = GameAuthorityService.getOngoingGames.findGame(gameId);
        const player = game?.findPlayer(client.id);
        const flashes = player?.differenceManager?.cheatFlashImages;
        if (flashes) {
            OutputFilterGateway.sendAllCheatFlashImages.toClient(client, flashes);
            OutputFilterGateway.sendCheatAlert.broadcast(client, player.name, game.getLobbyIds()[0]);
        }
    }

    /**
     * Starts by verifying the identity of the player before sending him the data, then sends him an array
     * with the length of the available differences and the yet to find differences with their appropriate
     * indexes.
     *
     * @param client - The socket of the client who sent the event
     * @param gameId - The id of the game in which the client plays
     * @param vote - the vote of the player
     */
    @SubscribeMessage(Events.ToServer.KICK_OUT_PLAYER)
    async kickOutPlayer(client: Socket, vote: CheaterVote) {
        if (typeof vote === 'string') {
            vote = JSON.parse(vote);
        }
        const game = GameAuthorityService.getOngoingGames.findGame(vote.gameId);
        if (game) {
            const cheaterRemoved = await game.processVote(vote.cheater, vote.voter, vote.playerVote);
            if (cheaterRemoved) {
                OutputFilterGateway.sendGlobalMessage.toLobby(
                    game.getLobbyIds()[0],
                    vote.cheater + ' a été exclu de la partie pour comportement frauduleux.',
                );
            }
        }
    }

    @SubscribeMessage(Events.ToServer.SEND_GAME_SETTINGS)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    async addGameSettings(client: Socket, gameValues: any) {
        let isCheatingAllowed: boolean;
        let timerTime: number;

        if (typeof gameValues === 'string') {
            gameValues = JSON.parse(gameValues);
            isCheatingAllowed = gameValues.isCheatAllowed;
            timerTime = gameValues.timerTime;
        } else {
            isCheatingAllowed = gameValues.matchSetting.isCheatAllowed;
            timerTime = gameValues.matchSetting.timerTime;
        }

        const game = GameAuthorityService.getPendingGames.findGame(gameValues.gameId);
        game.gameValues.isCheatAllowed = isCheatingAllowed;
        game.gameValues.timerTime = timerTime;
        game.isSettingDone = true;
    }
    @SubscribeMessage(Events.ToServer.LEAVE_OBSERVER)
    async leaveObserver(client: Socket, input: { gameId: string }) {
        const game = GameAuthorityService.getOngoingGames.findGame(input.gameId);
        if (!game) return;
        const observer = game.findObserver(client.id);
        if (!observer) return;
        game.removeObserver(client.id);
        console.log(game.getLobbyIds()[0]);
        OutputFilterGateway.sendObserverLeft.toLobby(game.getLobbyIds()[0], observer.name);
    }

    /**
     * Attempts to remove the client from a game, and thus looks for a corresponding game in the ongoing and
     * pending games list and try's to remove him
     *
     * @param client - The socket of the client who sent the event
     * @param gameId - The id of the game in which the client plays
     */
    @SubscribeMessage(Events.ToServer.LEAVE_GAME)
    async leaveGame(client: Socket, gameId: string) {
        const removedPlayer = await GameAuthorityService.getPendingGames.findGame(gameId)?.removePlayer(client.id);
        if (!removedPlayer) await GameAuthorityService.getOngoingGames.findGame(gameId)?.removePlayer(client.id);
    }

    @SubscribeMessage(Events.ToServer.SET_GAME_VALUES)
    async setGameValues(client: Socket, gameValues: GameValuesInputFilter) {
        if (gameValues.gainedTime !== undefined) GameAuthorityService.gameValues.gainedTime = gameValues.gainedTime;
        if (gameValues.penaltyTime !== undefined) GameAuthorityService.gameValues.penaltyTime = gameValues.penaltyTime;
        if (gameValues.timerTime !== undefined) GameAuthorityService.gameValues.timerTime = gameValues.timerTime;
    }

    @SubscribeMessage(Events.ToServer.GET_GAME_VALUES)
    async sendGameValues(client: Socket) {
        OutputFilterGateway.sendGameValues.toClient(client, GameAuthorityService.gameValues);
    }

    /**
     * Attempts to find an ongoing game with the corresponding Id and sends the client a hint if he is
     * in the game
     *
     * @param client - The socket of the client who sent the event
     * @param gameId - The id of the game in which the client plays
     */
    @SubscribeMessage(Events.ToServer.HINT)
    getHint(client: Socket, gameId: string) {
        const hint = GameAuthorityService.getOngoingGames.findGame(gameId)?.getHint(client.id);
        if (hint) OutputFilterGateway.sendHint.toClient(client, hint);
    }

    /**
     * Sends a list of joinable games to the client
     *
     * @param client - The socket of the client who sent the event
     */
    @SubscribeMessage(Events.ToServer.JOINABLE_GAME_CARDS)
    getJoinableGames(client: Socket) {
        OutputFilterGateway.sendJoinableGames.toClient(client, GameAuthorityService.joinableGames);
    }

    /**
     * Sends a list of all the active games to the client
     *
     * @param client - The socket of the client who sent the event
     */
    @SubscribeMessage(Events.ToServer.ACTIVE_GAMES)
    getActiveGames(client: Socket) {
        OutputFilterGateway.sendActiveGames.toClient(client, GameAuthorityService.getOngoingGamesArray);
    }

    /**
     * Request to join a game as an observer
     *
     * @param client - The socket of the client who sent the event
     */
    @SubscribeMessage(Events.ToServer.JOIN_AS_OBSERVER)
    joinAsObserver(client: Socket, input: { gameId: string }) {
        const game = GameAuthorityService.getOngoingGames.findGame(input.gameId);
        if (!game) return;
        const user = this.userSessionService.getUser(client.id);
        const userGame = {
            name: user.username,
            client,
            userId: user.userId,
        } as UserGame;
        if (!user) return;
        game.addObserver(userGame);
        const data = game.getDataForObserver();
        OutputFilterGateway.sendConnectionObserverResponseMessage.toClient(client, data);
        if (game.getGameMode === GameMode.LimitedTimeCoop || game.getGameMode === GameMode.LimitedTimeSolo) {
            game.newObserver();
        }
        OutputFilterGateway.sendObserverJoined.toLobby(game.getLobbyIds()[0], user.username);
    }

    handleConnection(client: Socket) {
        Logger.log('A client has connected: ' + client.id);
    }

    handleDisconnect(client: Socket) {
        GameAuthorityService.removePlayer(client.id);
        this.userSessionService.removePlayer(client.id);
        Logger.log('A client has disconnected: ' + client.id);
    }
}
