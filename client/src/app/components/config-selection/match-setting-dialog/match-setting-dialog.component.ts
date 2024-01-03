import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { MAX_TIMER, MIN_TIMER } from '@app/constants/time-constants';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { UserNameCheckerService } from '@app/services/game-selection/user-name-checker.service';
import { GameValues } from '@common/interfaces/game-play/game-values';
import * as Events from '@common/socket-event-constants';

@Component({
    selector: 'app-match-setting-dialog',
    templateUrl: './match-setting-dialog.component.html',
    styleUrls: ['./match-setting-dialog.component.scss'],
})
export class MatchSettingDialogComponent {
    isInRange: boolean = true;
    isCheatingAllowed: boolean = true;

    constructor(
        public dialogRef: MatDialogRef<MatchSettingDialogComponent>,
        private userNameChecker: UserNameCheckerService,
        public socketService: SocketClientService,
        public gameData: GameDataService,
    ) {}

    closeDialog(timeLimit: string | undefined, isCheatingAllowed: boolean) {
        const matchSetting = {
            timerTime: timeLimit,
            isCheatAllowed: isCheatingAllowed,
        } as unknown as GameValues;
        this.socketService.send(Events.ToServer.SEND_GAME_SETTINGS, { gameId: this.gameData.gameID, matchSetting });
        this.dialogRef.close(matchSetting);
    }

    enforceLimits(inputElement: HTMLInputElement) {
        let value = parseInt(inputElement.value, 10);
        if (isNaN(value)) {
            // Handle the case where the input is not a number, if needed
            return;
        }
        if (value < 20) {
            value = 20;
        } else if (value > 300) {
            value = 300;
        }
        inputElement.value = value.toString();
    }

    warnBadValue(timerTime: string) {
        if (parseInt(timerTime, 10) < MIN_TIMER || parseInt(timerTime, 10) > MAX_TIMER) {
            this.isInRange = false;
        } else {
            this.isInRange = true;
        }
    }
    onChange(inputValue: string) {
        const inputElement = document.querySelector('#userInput');
        const confirmButton = document.querySelector('#confirm button');
        if (inputElement && confirmButton) {
            if (this.userNameChecker.isValidInput(inputValue)) {
                inputElement.classList.remove('error');
                confirmButton.removeAttribute('disabled');
                confirmButton.classList.remove('disabled');
            } else {
                inputElement.classList.add('error');
                confirmButton.classList.add('disabled');
                confirmButton.setAttribute('disabled', '');
            }
        }
    }
}
