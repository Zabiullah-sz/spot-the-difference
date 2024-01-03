import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AwaitingPlayersModalComponent } from '@app/components/config-selection/awaiting-players-modal/awaiting-players-modal.component';
import { MatchSettingDialogComponent } from '@app/components/config-selection/match-setting-dialog/match-setting-dialog.component';
import { WarnPlayerModalComponent } from '@app/components/config-selection/warn-player-modal/warn-player-modal.component';
import { DIALOG_CUSTOM_CONGIF } from '@app/constants/dialog-config';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { UserService } from '@app/services/user/user.service';
import { Difficulty } from '@common/enums/game-play/difficulty';
import { GameConnectionAttemptResponseType } from '@common/enums/game-play/game-connection-attempt-response-type';
import { GameMode } from '@common/enums/game-play/game-mode';
import { PlayerConnectionStatus } from '@common/enums/game-play/player-connection-status';
import { GameValues } from '@common/interfaces/game-play/game-values';
import { SimpleUser } from '@common/interfaces/game-play/simple-user';
import * as Events from '@common/socket-event-constants';

@Component({
    selector: 'app-timed-selection-modal',
    templateUrl: './timed-selection-modal.component.html',
    styleUrls: ['./timed-selection-modal.component.scss'],
})
export class TimedSelectionModalComponent implements OnDestroy, OnInit {
    isSettingDone = true;
    waitingPlayers: SimpleUser[] = [];
    playerName: string;
    gameMode: GameMode;
    gameDifficulty: string;
    name2ndPlayer: string;
    canRequest: boolean = true;
    private dataAwaitingPlayers: { gameId: string; waitingPlayers: SimpleUser[]; displayMessage: number; playerNbr: number; isHost: boolean } = {
        gameId: '',
        waitingPlayers: this.waitingPlayers,
        displayMessage: 0,
        playerNbr: 0,
        isHost: false,
    };
    // eslint-disable-next-line max-params
    constructor(
        public dialogRef: MatDialogRef<TimedSelectionModalComponent>,
        @Inject(MAT_DIALOG_DATA)
        public data: GameValues,
        private router: Router,
        private dialog: MatDialog,
        private socketService: SocketClientService,
        public gameData: GameDataService,
        public userService: UserService,
    ) {
        dialogRef.disableClose = true;
    }

    ngOnInit(): void {
        this.requestUsername();
    }

    ngOnDestroy(): void {
        this.socketService.removeListener(Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST);
    }

    playSolo() {
        if (this.canRequest) {
            this.canRequest = false;
            // const setting = {
            //     time: this.data.timerTime,
            //     isCheatAllowed: this.data.isCheatAllowed,
            // };
            this.socketService.send(Events.ToServer.REQUEST_TO_PLAY, {
                gameMode: GameMode.LimitedTimeSolo,
                playerName: this.playerName,
                // gameSetting: setting,
            });
            this.socketService.on(Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST, this.limitedTimeSingleplayer.bind(this));
        }
    }

    playCoop() {
        if (this.canRequest) {
            this.canRequest = false;
            this.socketService.send(Events.ToServer.REQUEST_TO_PLAY, {
                gameMode: GameMode.LimitedTimeCoop,
                playerName: this.playerName,
            });
            this.socketService.on(Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST, this.limitedTimeCoopMode.bind(this));
        }
    }
    closeDialog() {
        this.dialogRef.close();
        this.socketService.send(Events.ToServer.LEAVE_GAME, this.dataAwaitingPlayers.gameId);
    }

    private requestUsername() {
        this.userService.currentUser$.subscribe((user) => {
            if (user) {
                this.playerName = user.username;
            }
        });
    }

    private matchSetting(data: typeof Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST.type): void {
        this.isSettingDone = false;
        const usernameDialogRef = this.dialog.open(MatchSettingDialogComponent, DIALOG_CUSTOM_CONGIF);
        usernameDialogRef.afterClosed().subscribe(() => {
            this.isSettingDone = true;
            if (this.dataAwaitingPlayers.playerNbr === 1) this.handlePendingCase(data);
            else
                this.dialog.open(AwaitingPlayersModalComponent, {
                    width: '500px',
                    height: 'fit-content',
                    backdropClass: 'backdropBackground',
                    data: this.dataAwaitingPlayers,
                });
        });
    }

    private startGame(arg: typeof Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST.type) {
        if (arg.difficulty === Difficulty.Easy) this.gameDifficulty = 'facile';
        else if (arg.difficulty === Difficulty.Hard) this.gameDifficulty = 'difficile';
        this.gameData.timeToStart = arg.startingIn;
        this.gameData.modifiedPicture = arg.modifiedImage;
        this.gameData.chronoTime = arg.time;
        this.gameData.gameID = arg.gameId;
        this.gameData.nbOfPlayers = arg.playerNbr;
        this.gameData.originalPicture = arg.originalImage;
        this.gameData.differenceNbr = arg.differenceNbr;
        this.gameData.difficulty = this.gameDifficulty;
        this.gameData.name = this.playerName;
        this.gameData.gameName = arg.gameName;
        this.gameData.gameMode = this.gameMode;
        this.gameData.name2ndPlayer = this.name2ndPlayer;
        this.gameData.gameValues = arg.gameValues;
        this.gameData.allPlayers = arg.players;
        this.router.navigateByUrl('/game');
    }

    private limitedTimeSingleplayer(data: typeof Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST.type) {
        this.canRequest = true;
        this.gameMode = GameMode.LimitedTimeSolo;
        switch (data.responseType) {
            case GameConnectionAttemptResponseType.Starting:
                this.startGame(data);
                this.dialog.closeAll();
                break;
            case GameConnectionAttemptResponseType.Pending:
                // should never happen
                this.dialog.closeAll();
                break;
            case GameConnectionAttemptResponseType.Cancelled:
                if (!WarnPlayerModalComponent.opened) {
                    WarnPlayerModalComponent.opened = true;
                    this.dialog.open(WarnPlayerModalComponent, {
                        width: '450px',
                        height: '400x',
                        data: { warning: 3 },
                    });
                }
                break;
            case GameConnectionAttemptResponseType.Rejected:
                // should never happen
                this.dialog.closeAll();
                break;
        }
    }

    private handlePendingCase(data: typeof Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST.type) {
        console.log(data);
        this.dataAwaitingPlayers.gameId = data.gameId;
        this.dataAwaitingPlayers.playerNbr = data.playerNbr;
        console.log('playerNBR', this.dataAwaitingPlayers);
        this.socketService.on(Events.FromServer.PLAYER_STATUS, (d: typeof Events.FromServer.PLAYER_STATUS.type) => {
            if (d.playerConnectionStatus === PlayerConnectionStatus.Joined) {
                this.name2ndPlayer = d.user !== undefined ? d.user?.name : 'Anonymous';
            } else if (d.playerConnectionStatus === PlayerConnectionStatus.Left) {
                if (d.playerNbr) this.dataAwaitingPlayers.playerNbr = d.playerNbr;
                this.dataAwaitingPlayers.isHost = d.newHost === this.playerName;
            }
        });

        this.dataAwaitingPlayers.isHost = data.hostName === this.playerName;
        if (this.isSettingDone) {
            if (!AwaitingPlayersModalComponent.opened) {
                AwaitingPlayersModalComponent.opened = true;
                this.dialog.open(AwaitingPlayersModalComponent, {
                    width: '500px',
                    height: 'fit-content',
                    backdropClass: 'backdropBackground',
                    data: this.dataAwaitingPlayers,
                });
            }
        }
    }

    private limitedTimeCoopMode(data: typeof Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST.type) {
        this.canRequest = true;
        this.gameMode = GameMode.LimitedTimeCoop;
        this.gameData.gameID = data.gameId;
        switch (data.responseType) {
            case GameConnectionAttemptResponseType.Starting:
                this.dialog.closeAll();
                if (this.name2ndPlayer === undefined) {
                    if (data.hostName) this.name2ndPlayer = data.hostName;
                }
                this.socketService.removeListener(Events.FromServer.PLAYER_STATUS);
                this.startGame(data);
                break;
            case GameConnectionAttemptResponseType.Pending:
                if (data.isSettingSpecified === false) this.matchSetting(data);
                else this.handlePendingCase(data);
                break;
            case GameConnectionAttemptResponseType.Cancelled:
                if (!WarnPlayerModalComponent.opened) {
                    WarnPlayerModalComponent.opened = true;
                    this.dialog.open(WarnPlayerModalComponent, {
                        width: '450px',
                        height: '400x',
                        data: { warning: 3 },
                    });
                }
                break;
        }
    }
}
