import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
// import { MatchSettingDialogComponent } from '@app/components/config-selection/match-setting-dialog/match-setting-dialog.component';
// eslint-disable-next-line max-len
import { TimedSelectionModalComponent } from '@app/components/config-selection/timed-selection-modal/timed-selection-modal.component';
import { DIALOG_CUSTOM_CONGIF } from '@app/constants/dialog-config';
import { ChatService } from '@app/services/divers/chat.service';
import { GameListManagerService } from '@app/services/divers/game-list-manager.service';
import { GameDataService } from '@app/services/game-play/game-data.service';
import { UserService } from '@app/services/user/user.service';
// import { GameValues } from '@common/interfaces/game-play/game-values';
import { User } from '@common/interfaces/user';
import { ModalService } from '@app/services/divers/modal.service';
import { environment } from 'src/environments/environment';
import { Subscription } from 'rxjs';
@Component({
    selector: 'app-main-page',
    templateUrl: './main-page.component.html',
    styleUrls: ['./main-page.component.scss'],
})
export class MainPageComponent implements OnInit, OnDestroy {
    imageBaseUrl = environment.serverUrl + '/users/get-public-profile-image/';
    user: User;
    menuVisible = false;
    isDetached = false;
    playerName: string;
    private modalStateSubscription: Subscription;
    // eslint-disable-next-line @typescript-eslint/member-ordering
    showChatbox: boolean = true;
    constructor(
        private dialog: MatDialog,
        private gameList: GameListManagerService,
        public gameData: GameDataService,
        private userService: UserService,
        private chatService: ChatService,
        private modalService: ModalService,
    ) {
        this.modalStateSubscription = this.modalService.modalState$.subscribe((state: boolean) => {
            this.showChatbox = state;
        });
    }
    // eslint-disable-next-line max-params

    ngOnInit(): void {
        this.gameList.init();
        this.userService.currentUser$.subscribe((user) => {
            if (user) this.user = user;
        });
        this.chatService.inSeperateWindow$.subscribe((state) => {
            this.isDetached = state;
        });
    }

    ngOnDestroy() {
        this.modalStateSubscription.unsubscribe();
    }

    toggleMenu(): void {
        this.menuVisible = !this.menuVisible;
    }

    logout(): void {
        this.userService.logout().subscribe();
        this.menuVisible = false;
    }
    timedSelectionModal(): void {
        this.dialog.open(TimedSelectionModalComponent, DIALOG_CUSTOM_CONGIF);
        // usernameDialogRef.afterClosed().subscribe((matchSetting: GameValues) => {
        //     if (matchSetting.timerTime !== undefined) {
        //         const dialogConfig = Object.assign({}, DIALOG_CUSTOM_CONGIF);
        //         dialogConfig.data = matchSetting;
        //         this.dialog.open(TimedSelectionModalComponent, dialogConfig);
        //     } else {
        //         return;
        //     }
        // });
    }
}
