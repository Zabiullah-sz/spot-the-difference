import { Component, Input } from '@angular/core';
import { ChatService } from '@app/services/game-play/chat.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { GameService } from '@app/services/game-play/game.service';
import { GameMode } from '@common/enums/game-play/game-mode';

@Component({
    selector: 'app-display-score',
    templateUrl: './display-score.component.html',
    styleUrls: ['./display-score.component.scss'],
})
export class DisplayScoreComponent {
    @Input() firstPlayerScore: number;
    @Input() secondPlayerScore: number;
    @Input() gamePlayerScore: Map<string, number>;
    @Input() isMultiplayer: boolean;
    currentPlayer: string = '';
    isMuted: boolean = false;
    constructor(public gameService: GameService, public gameData: GameDataService, public chatService: ChatService) {}

    get differencesLeft(): number | undefined {
        if (this.gameData.nbOfPlayers === 1) {
            return this.gameService.totalDifferences;
        } else {
            return Math.ceil(this.gameService.totalDifferences / 2);
        }
    }

    differencesFound(score: number): number {
        if (this.gameData.nbOfPlayers === 1) {
            this.gameService.differencesFoundTotal = this.gameService.differencesFound;
            return this.gameService.differencesFound;
        } else {
            this.gameService.differencesFoundTotal = score;
            return score;
        }
    }

    isLimitedTimeMode(): boolean {
        return this.gameData.gameMode === GameMode.LimitedTimeSolo || this.gameData.gameMode === GameMode.LimitedTimeCoop;
    }

    isPlayerMuted(user: string) {
        return this.chatService.mutedUsers.includes(user);
    }

    mute(user: string): void {
        this.chatService.muteUser(user);
    }
    unMute(user: string): void {
        this.chatService.unmuteUser(user);
    }
    toggleMute(playerName: string): void {
        if (this.isPlayerMuted(playerName)) {
            this.unMute(playerName);
        } else {
            this.mute(playerName);
        }
    }
}
