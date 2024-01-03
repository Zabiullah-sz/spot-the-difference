import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AwaitingPlayersModalComponent } from '@app/components/config-selection/awaiting-players-modal/awaiting-players-modal.component';
import { MatchSettingDialogComponent } from '@app/components/config-selection/match-setting-dialog/match-setting-dialog.component';
import { WarnPlayerModalComponent } from '@app/components/config-selection/warn-player-modal/warn-player-modal.component';
import { DIALOG_CUSTOM_CONGIF } from '@app/constants/dialog-config';
import { GameSelection } from '@app/interfaces/game-card/game-selection';
import { ModalService } from '@app/services/divers/modal.service';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { GameSelectorService } from '@app/services/game-selection/game-selector.service';
import { UserService } from '@app/services/user/user.service';
import { Difficulty } from '@common/enums/game-play/difficulty';
import { GameConnectionAttemptResponseType } from '@common/enums/game-play/game-connection-attempt-response-type';
import { GameMode } from '@common/enums/game-play/game-mode';
import { PlayerConnectionStatus } from '@common/enums/game-play/player-connection-status';
import { GameConnectionRequestOutputMessageDto } from '@common/interfaces/game-play/game-connection-request.dto';
import { SimpleUser } from '@common/interfaces/game-play/simple-user';
import * as Events from '@common/socket-event-constants';
import { Subject, Subscription, takeUntil } from 'rxjs';

@Component({
    selector: 'app-classic-selection-page',
    templateUrl: './classic-selection-page.component.html',
    styleUrls: ['./classic-selection-page.component.scss'],
})
export class ClassicSelectionPageComponent implements OnInit, OnDestroy {
    buttonNames: [string, string, string] = ['Jouer en solo', 'Jouer multijoueur', 'Joindre multijoueur'];
    private gameMode: GameMode = GameMode.ClassicSolo;
    private componentDestroyed$: Subject<void> = new Subject<void>();
    private gameDifficulty: string;
    private namePlayer: string;
    private name2ndPlayer: string;
    private waitingPlayers: SimpleUser[] = [];
    private modalStateSubscription: Subscription;
    // eslint-disable-next-line @typescript-eslint/member-ordering
    showChatbox: boolean = true;
    private data: { gameId: string; waitingPlayers: SimpleUser[]; displayMessage: number; isHost: boolean } = {
        gameId: '',
        waitingPlayers: this.waitingPlayers,
        displayMessage: 0,
        isHost: false,
    };

    // eslint-disable-next-line max-params
    constructor(
        public gameData: GameDataService,
        public socketService: SocketClientService,
        private dialog: MatDialog,
        public router: Router,
        private selectorService: GameSelectorService,
        public userService: UserService,
        private modalService: ModalService,
    ) {
        this.modalStateSubscription = this.modalService.modalState$.subscribe((state: boolean) => {
            this.showChatbox = state;
        });
    }

    ngOnInit() {
        this.selectorService.selectionValue.pipe(takeUntil(this.componentDestroyed$)).subscribe((values) => {
            this.requestUsername(values);
        });
    }

    ngOnDestroy() {
        this.socketService.removeListener(Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST);
        this.dialog.closeAll();
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
        this.modalStateSubscription.unsubscribe();
    }

    private getGameMode(side: string) {
        let gameMode: GameMode;
        if (side === this.buttonNames[0]) {
            gameMode = GameMode.ClassicSolo;
        } else {
            gameMode = GameMode.Classic1v1;
        }
        return gameMode;
    }

    private requestUsername(selection: GameSelection) {
        this.userService.currentUser$.subscribe((user) => {
            if (user) {
                this.namePlayer = user.username;
                if (this.userService.canJoinClassic) {
                    this.choseGame(selection);
                }
            }
        });
    }

    private startGame(arg: GameConnectionRequestOutputMessageDto) {
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
        this.gameData.name = this.namePlayer;
        this.gameData.gameName = arg.gameName;
        this.gameData.gameMode = this.gameMode;
        this.gameData.name2ndPlayer = this.name2ndPlayer;
        this.gameData.gameValues = arg.gameValues;
        this.router.navigateByUrl('/game');
    }

    private removeWaitingPlayer(playerId: string) {
        for (let i = 0; i < this.waitingPlayers.length; i++) {
            const waitingPlayer = this.waitingPlayers[i];
            if (waitingPlayer.id === playerId) {
                this.waitingPlayers.splice(i, 1);
                return;
            }
        }
    }

    private classic1v1(data: GameConnectionRequestOutputMessageDto) {
        this.data.gameId = data.gameId;
        this.gameData.gameID = data.gameId;
        this.gameMode = GameMode.Classic1v1;
        if (data.isSettingSpecified === false) {
            this.data.isHost = true;
            this.data.displayMessage = 1;
            this.matchSetting();
        } else if (data.isSettingSpecified === true) {
            this.data.displayMessage = 2;
            this.dialog.open(AwaitingPlayersModalComponent, {
                width: 'fit-content',
                height: 'fit-content',
                backdropClass: 'backdropBackground',
                data: this.data,
            });
        }
        switch (data.responseType) {
            case GameConnectionAttemptResponseType.Starting:
                this.dialog.closeAll();
                if (this.waitingPlayers.length !== 0) this.name2ndPlayer = this.waitingPlayers[0].name;
                else if (data.hostName) this.name2ndPlayer = data.hostName;
                this.gameData.allPlayers = data.players;
                this.startGame(data);
                break;
            case GameConnectionAttemptResponseType.Pending: {
                this.socketService.on(Events.FromServer.PLAYER_STATUS, (d: typeof Events.FromServer.PLAYER_STATUS.type) => {
                    if (d.user) {
                        switch (d.playerConnectionStatus) {
                            case PlayerConnectionStatus.AttemptingToJoin:
                                this.waitingPlayers.push(d.user);
                                break;
                            case PlayerConnectionStatus.Left:
                                this.removeWaitingPlayer(d.user.id);
                                break;
                        }
                    }
                });
                break;
            }
            case GameConnectionAttemptResponseType.Cancelled:
                {
                    const dialogConfig = Object.assign({}, DIALOG_CUSTOM_CONGIF);
                    dialogConfig.data = { warning: 0 };
                    this.dialog.closeAll();
                    this.dialog.open(WarnPlayerModalComponent, dialogConfig);
                }
                break;
            case GameConnectionAttemptResponseType.Rejected:
                {
                    const dialogConfig = Object.assign({}, DIALOG_CUSTOM_CONGIF);
                    dialogConfig.data = { warning: 1 };
                    this.dialog.closeAll();
                    this.dialog.open(WarnPlayerModalComponent, dialogConfig);
                }
                break;
        }
    }

    private matchSetting(): void {
        const usernameDialogRef = this.dialog.open(MatchSettingDialogComponent, DIALOG_CUSTOM_CONGIF);
        usernameDialogRef.afterClosed().subscribe(() => {
            this.dialog.open(AwaitingPlayersModalComponent, {
                width: 'fit-content',
                height: 'fit-content',
                backdropClass: 'backdropBackground',
                data: this.data,
            });
        });
    }

    private choseGame(selection: GameSelection) {
        this.waitingPlayers = [];
        this.data.waitingPlayers = this.waitingPlayers;
        const gameMode = this.getGameMode(selection.buttonName);
        this.socketService.send(Events.ToServer.REQUEST_TO_PLAY, { gameMode, cardId: selection.id, playerName: this.namePlayer });
        this.socketService.on(Events.FromServer.RESPONSE_TO_JOIN_GAME_REQUEST, this.classic1v1.bind(this));
    }
}
