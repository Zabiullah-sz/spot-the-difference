import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { INTERVAL_TIME } from '@app/constants/time-constants';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { ChatService } from '@app/services/game-play/chat.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { GameValues } from '@common/interfaces/game-play/game-values';
import { FromServer } from '@common/socket-event-constants';
// import { GameValues } from '@common/interfaces/game-play/game-values';

@Component({
    selector: 'app-chatbox',
    templateUrl: './chatbox.component.html',
    styleUrls: ['./chatbox.component.scss'],
})
export class ChatboxComponent implements OnInit, OnDestroy {
    @Input() isMultiplayer: boolean;

    // eslint-disable-next-line max-params
    constructor(public chatService: ChatService, public gameData: GameDataService, public socketService: SocketClientService) {}

    ngOnInit() {
        this.gameData = {
            timeToStart: 0,
            modifiedPicture: '',
            chronometerTime: 0,
            gameID: '64f36322bfad353b9b7c9b2e',
            nbOfPlayers: 2,
            originalPicture: '',
            differenceNbr: 5,
            difficulty: 'facile',
            name: 'user1',
            gameMode: 0,
            gameName: 'Game 1',
            name2ndPlayer: 'user2',
            chronoTime: 0,
            gameValues: {} as GameValues,
        } as GameDataService;

        this.isMultiplayer = true;
        setInterval(() => {
            this.chatService.gameMode = this.gameData.gameMode;
            this.chatService.getGameMode();
            this.chatService.differenceFounded();
            this.chatService.differenceMistakeMade();
            this.chatService.differenceFound = false;
            this.chatService.differenceError = false;
        }, INTERVAL_TIME);
        this.chatService.waitForEnemyClick();

        this.chatService.reset();
    }

    ngOnDestroy(): void {
        this.socketService.removeListener(FromServer.CLICK_ENEMY);
    }
}
