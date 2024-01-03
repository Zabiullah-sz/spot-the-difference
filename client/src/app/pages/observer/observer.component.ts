import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { GameMode } from '@common/enums/game-play/game-mode';
import { ActiveGame } from '@common/interfaces/active-game';
import * as Events from '@common/socket-event-constants';

@Component({
    selector: 'app-observer',
    templateUrl: './observer.component.html',
    styleUrls: ['./observer.component.scss'],
})
export class ObserverComponent implements OnInit {
    activeGames: ActiveGame[] = [];
    constructor(private socketService: SocketClientService, private gameData: GameDataService, private router: Router) {}

    ngOnInit(): void {
        this.socketService.on(Events.FromServer.ACTIVE_GAMES, (data) => {
            this.activeGames = data;
        });
        this.socketService.send(Events.ToServer.ACTIVE_GAMES);

        this.socketService.on(Events.FromServer.RESPONSE_TO_JOIN_GAME_OBSERVER_REQUEST, (data) => {
            if (data.responseType === 'success') {
                this.gameData.timeToStart = data.startingIn;
                this.gameData.modifiedPicture = data.modifiedImage;
                this.gameData.chronoTime = data.time;
                this.gameData.gameID = data.gameId;
                this.gameData.nbOfPlayers = data.playerNbr;
                this.gameData.originalPicture = data.originalImage;
                this.gameData.differenceNbr = data.differenceNbr;
                this.gameData.difficulty = 'hard';
                this.gameData.gameMode = data.gameMode;

                this.gameData.gameName = data.gameName;
                // this.gameData.gameMode = data.gameMode;
                // this.gameData.name2ndPlayer = this.name2ndPlayer;
                this.gameData.gameValues = data.gameValues;
                this.gameData.allPlayers = data.players;

                this.router.navigateByUrl('/observer-game');
            }
        });
    }

    joinAsObserver(gameId: string): void {
        this.socketService.send(Events.ToServer.JOIN_AS_OBSERVER, { gameId });
    }
    gameModeToString(mode: GameMode): string {
        switch (mode) {
            case GameMode.Classic1v1:
                return 'Clasique 1v1';
            case GameMode.ClassicSolo:
                return 'Classique Solo';
            case GameMode.LimitedTimeCoop:
                return 'Temps limité Coop';
            case GameMode.LimitedTimeSolo:
                return 'Temps limité Solo';
            default:
                return 'Mode de jeu inconnu';
        }
    }
    refresh(): void {
        this.socketService.send(Events.ToServer.ACTIVE_GAMES);
    }
}
