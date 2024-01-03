/* eslint-disable no-underscore-dangle */
/* eslint-disable no-console */
import { Injectable } from '@angular/core';
import { TWO_DIGIT_TIME } from '@app/constants/time-constants';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { UserNameCheckerService } from '@app/services/game-selection/user-name-checker.service';
import { GameMode } from '@common/enums/game-play/game-mode';
import { Chat } from '@common/interfaces/chat';
import { ChatMessageOutputDto } from '@common/interfaces/game-play/chat-message.dto';
import { GameClickOutputDto } from '@common/interfaces/game-play/game-click.dto';
import { Message } from '@common/interfaces/game-play/message';
import { FromServer, ToServer } from '@common/socket-event-constants';
import { BehaviorSubject } from 'rxjs';
import { GameDataService } from './game-data.service';
import { GameService } from './game.service';
import { ReplayService } from './replay.service';
@Injectable({
    providedIn: 'root',
})
export class ChatService {
    activeChat$: BehaviorSubject<Chat | null> = new BehaviorSubject<Chat | null>(null); // Ajout de activeChat$ comme BehaviorSubject
    differenceFound: boolean;
    differenceError: boolean;
    playerGaveUp: boolean;
    valueChanged: boolean;
    mutedPlayers: string[] = [];
    isCoop: boolean = true;
    messages: Message[] = [];
    inputValue: string = '';
    recordBeaterMessage: string;
    gameMode = this.gameData.gameMode;
    mutedUsers: string[] = [];
    muted: string;
    private isMessage: boolean;
    // private currentUser: User | null = null;
    // eslint-disable-next-line max-params
    constructor(
        public socketService: SocketClientService,
        public gameData: GameDataService,
        private userNameChecker: UserNameCheckerService,
        private classicGameService: GameService,
        private replayService: ReplayService,
    ) {
        // this.mutedUsers = [];
        this.socketService.on(FromServer.HINT, () => {
            const message: Message = {
                text: 'Indice utilisé',
                time: this.getCurrentTime(),
                isMessageText: false,
                messageReceived: false,
            };
            this.pushMessage(message);
        });
        this.socketService.on(FromServer.CHAT_MESSAGE, (data: ChatMessageOutputDto) => {
            const message: Message = {
                text: data.sender + ': ' + data.message,
                time: data.time,
                isMessageText: true,
                messageReceived: true,
            };

            const isMuted = this.isUserMuted(data.sender);
            if (!isMuted) {
                this.pushMessage(message);
                this.sortMessages();
            }
            console.log('list of messages', this.messages);
        });

        this.socketService.on(FromServer.CHEAT_ALERT, (data: string) => {
            const currentTime = this.getCurrentTime();
            const message: Message = {
                text: data + ' a triché!',
                time: currentTime,
                isMessageText: false,
                messageReceived: false,
            };
            this.pushMessage(message);
        });

        this.socketService.on(FromServer.GLOBAL_MESSAGE, (data: string) => {
            const message: Message = {
                text: data,

                time: this.getCurrentTime(),
                isMessageText: false,
                messageReceived: false,
            };
            this.pushMessage(message);
        });

        this.socketService.on(FromServer.RECORD_BEATER, (data: string) => {
            this.recordBeaterMessage = data;
        });

        this.socketService.on(FromServer.DESERTER, (data: string) => {
            const message: Message = {
                text: data + ' a abandonné la partie',
                time: this.getCurrentTime(),
                isMessageText: false,
                messageReceived: false,
            };
            this.pushMessage(message);
        });

        this.replayService.replayEvent.subscribe(this.reset.bind(this));
    }
    isUserMuted(userId: string): boolean {
        return this.mutedUsers.includes(userId);
    }

    waitForEnemyClick() {
        this.socketService.on(FromServer.CLICK_ENEMY, (data: GameClickOutputDto) => {
            const message: Message = {
                text: (data.valid ? 'Différence trouvée par' : 'Erreur par') + ' ' + data.playerName,
                time: this.getCurrentTime(),
                isMessageText: false,
                messageReceived: false,
            };
            this.pushMessage(message);
        });
    }

    // Function to convert HH:MM:SS time string to total seconds
    timeStringToSeconds(timeString: string) {
        const parts = timeString.split(':');
        const hours = parseInt(parts[0], 10);
        const minutes = parseInt(parts[1], 10);
        const seconds = parseInt(parts[2], 10);
        return hours * 3600 + minutes * 60 + seconds;
    }

    // Method to sort messages by time
    sortMessages() {
        this.messages.sort((a, b) => this.timeStringToSeconds(b.time) - this.timeStringToSeconds(a.time));
    }

    getGameMode() {
        if (this.gameData.gameMode === GameMode.ClassicSolo || this.gameMode === GameMode.LimitedTimeSolo) {
            this.isCoop = false;
        } else if (this.gameMode === GameMode.Classic1v1 || this.gameMode === GameMode.LimitedTimeCoop) {
            this.isCoop = true;
        }
    }

    reset() {
        this.messages = [];
    }

    onKeyDown(event: Event): void {
        if (this.gameMode === GameMode.LimitedTimeCoop || this.gameMode === GameMode.Classic1v1) {
            if (event instanceof KeyboardEvent && event.key === 'Enter') {
                this.isMessage = true;
                this.addMessage(true);
            }
        }
    }

    differenceFounded() {
        if (this.differenceFound) {
            if (this.gameMode === GameMode.ClassicSolo || this.gameMode === GameMode.LimitedTimeSolo) {
                this.inputValue = 'Différence trouvée';
            } else {
                this.inputValue = `Différence trouvée par ${this.gameData.name}`;
            }
            this.addMessage(false);
        }
    }

    differenceMistakeMade() {
        if (this.differenceError) {
            if (this.gameMode === GameMode.ClassicSolo || this.gameMode === GameMode.LimitedTimeSolo) {
                this.inputValue = 'Erreur';
            } else {
                this.inputValue = `Erreur par ${this.gameData.name}`;
            }
            this.addMessage(false);
        }
    }

    onFocus() {
        this.classicGameService.canCheat = false;
    }

    onFocusOut() {
        this.classicGameService.canCheat = true;
    }

    pushMessage(message: Message) {
        this.replayService.store(() => this.pushMessage(message));
        this.messages.unshift(message);
    }
    /// //////////MUTE AND UNMUTE///////////////////////////////
    muteUser(userIdToMute: string): void {
        this.muted = userIdToMute;
        this.mutedUsers.push(userIdToMute);
    }
    unmuteUser(userIdToUnmute: string): void {
        const index = this.mutedUsers.indexOf(userIdToUnmute);
        if (index !== -1) {
            this.mutedUsers.splice(index, 1);
        }
    }

    private addMessage(isPersonalMessage: boolean) {
        this.inputValue = this.inputValue.trim();
        if (this.userNameChecker.isValidInput(this.inputValue)) {
            const prefix = isPersonalMessage ? 'VOUS: ' : '';
            const newMessage: Message = {
                text: prefix + this.inputValue,
                time: this.getCurrentTime(),
                isMessageText: this.isMessage,
                messageReceived: false,
            };
            const messageData = {
                gameId: this.gameData.gameID,
                message: this.inputValue,
                time: newMessage.time,
            };
            if (this.isMessage) {
                this.socketService.send(ToServer.CHAT_MESSAGE, messageData);
            }
            this.pushMessage(newMessage);
            this.sortMessages();
            this.inputValue = '';
            this.isMessage = false;
        }
    }

    private getCurrentTime(): string {
        const now = new Date();
        const hours = this.formatTime(now.getHours());
        const minutes = this.formatTime(now.getMinutes());
        const seconds = this.formatTime(now.getSeconds());
        return `${hours}:${minutes}:${seconds}`;
    }

    private formatTime(time: number): string {
        return time < TWO_DIGIT_TIME ? `0${time}` : `${time}`;
    }
}
