import * as Events from '@common/socket-event-constants';
import { OnGatewayInit, WebSocketGateway, WebSocketServer } from '@nestjs/websockets';
import { Server } from 'socket.io';
import EmissionFilter from './emission-filter';
import { GATEWAY_PORT } from './game.gateway.constants';

@WebSocketGateway(GATEWAY_PORT)
export default class OutputFilterGateway implements OnGatewayInit {
    static sendAllRecords = new EmissionFilter(Events.FromServer.ALL_RECORDS);
    static sendCardValidationResponse = new EmissionFilter(Events.FromServer.CARD_VALIDATION);
    static sendValidationResponse = new EmissionFilter(Events.FromServer.VALIDATION_RESPONSE);
    static sendCardCreationResponse = new EmissionFilter(Events.FromServer.CARD_CREATION);
    static sendOtherClick = new EmissionFilter(Events.FromServer.CLICK_ENEMY);
    static sendChatMessage = new EmissionFilter(Events.FromServer.CHAT_MESSAGE);
    static sendDeleteAllCardsOutcome = new EmissionFilter(Events.FromServer.CARD_DELETE_RESPONSE);
    static sendCardPreviews = new EmissionFilter(Events.FromServer.ALL_GAME_CARDS);
    static sendAllCardPreviewTimes = new EmissionFilter(Events.FromServer.ALL_FRONTEND_CARD_TIMES);
    static sendCardTimes = new EmissionFilter(Events.FromServer.FRONTEND_CARD_TIMES);
    static sendCardPreview = new EmissionFilter(Events.FromServer.GAME_CARD);
    static sendJoinableGames = new EmissionFilter(Events.FromServer.JOINABLE_GAME_CARDS);
    static sendGlobalMessage = new EmissionFilter(Events.FromServer.GLOBAL_MESSAGE);
    static sendRecordBeaterMessage = new EmissionFilter(Events.FromServer.RECORD_BEATER);
    static sendEndgameMessage = new EmissionFilter(Events.FromServer.ENDGAME);
    static sendPlayerConnectionMessage = new EmissionFilter(Events.FromServer.PLAYER_STATUS);
    static sendConnectionAttemptResponseMessage = new EmissionFilter(Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST);
    static sendConnectionObserverResponseMessage = new EmissionFilter(Events.FromServer.RESPONSE_TO_JOIN_GAME_OBSERVER_REQUEST);
    static sendDeserterMessage = new EmissionFilter(Events.FromServer.DESERTER);
    static sendClickResponseMessage = new EmissionFilter(Events.FromServer.CLICK_PERSONAL);
    static sendAllCheatFlashImages = new EmissionFilter(Events.FromServer.CHEAT);
    static sendPlayerStatus = new EmissionFilter(Events.FromServer.IS_PLAYING);
    static sendHint = new EmissionFilter(Events.FromServer.HINT);
    static sendCheatIndex = new EmissionFilter(Events.FromServer.CHEAT_INDEX);
    static sendGameValues = new EmissionFilter(Events.FromServer.GAME_VALUES);
    static sendNextCard = new EmissionFilter(Events.FromServer.NEXT_CARD);
    static sendRecord = new EmissionFilter(Events.FromServer.SPREAD_HISTORY);
    static sendTime = new EmissionFilter(Events.FromServer.TIME);
    static sendLoadChat = new EmissionFilter(Events.FromServer.LOAD_CHAT);
    static sendNewMessage = new EmissionFilter(Events.FromServer.NEW_MESSAGE);
    static sendAllChats = new EmissionFilter(Events.FromServer.ALL_CHATS);
    static sendAllActiveUsers = new EmissionFilter(Events.FromServer.ALL_ACTIVE_USERS);
    static sendNotification = new EmissionFilter(Events.FromServer.NOTIFICATION);
    static sendFriendRequest = new EmissionFilter(Events.FromServer.FRIEND_REQUEST);
    static sendActiveGames = new EmissionFilter(Events.FromServer.ACTIVE_GAMES);
    static sendCheatAlert = new EmissionFilter(Events.FromServer.CHEAT_ALERT);
    static sendCheaterBackToLobby = new EmissionFilter(Events.FromServer.BACK_TO_LOBBY);
    static sendObserverJoined = new EmissionFilter(Events.FromServer.NEW_OBSERVER);
    static sendObserverLeft = new EmissionFilter(Events.FromServer.OBSERVER_LEFT);

    @WebSocketServer() static server: Server;

    afterInit(server: Server) {
        OutputFilterGateway.server = server;
        EmissionFilter.server = server;
    }
}
