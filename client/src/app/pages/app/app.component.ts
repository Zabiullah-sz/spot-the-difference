/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable no-console */
import { Component, OnInit } from '@angular/core';
import { ChatService } from '@app/services/divers/chat.service';
import { FriendService } from '@app/services/divers/friend.service';
import { GameListManagerService } from '@app/services/divers/game-list-manager.service';
import { HistoryService } from '@app/services/game-config/history.service';
import { ThemeService } from '@app/services/theme.service';
import { UserService } from '@app/services/user/user.service';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
    // eslint-disable-next-line max-params
    constructor(
        private gameList: GameListManagerService,
        private readonly historyService: HistoryService,
        private themeService: ThemeService,
        private userService: UserService,
        private chatService: ChatService,
        private friendService: FriendService,
    ) {
        this.themeService.setTheme('light-theme');

        // Vérifiez si le thème actuel est "light-theme" et ajoutez l'attribut "theme" en conséquence
        if (this.themeService.getTheme() === 'light-theme') {
            this.addThemeAttribute('light');
        }
    }

    ngOnInit(): void {
        this.userService.init();
        this.chatService.init();
        this.friendService.init();
        this.gameList.init();
        this.historyService.init();
    }

    onChangeTheme(event: any) {
        const selectedTheme = event?.target?.value;
        if (selectedTheme) {
            console.log(`Changing theme to: ${selectedTheme}`);
            this.themeService.setTheme(selectedTheme);

            // Vérifiez si le thème sélectionné est "light-theme" et ajoutez ou supprimez l'attribut "theme" en conséquence
            if (selectedTheme === 'light-theme') {
                this.addThemeAttribute('light');
            } else {
                this.removeThemeAttribute();
            }
        }
    }

    private addThemeAttribute(theme: string) {
        const bodyElement = document.querySelector('body');
        if (bodyElement) {
            bodyElement.setAttribute('theme', theme);
        }
    }

    private removeThemeAttribute() {
        const bodyElement = document.querySelector('body');
        if (bodyElement) {
            bodyElement.removeAttribute('theme');
        }
    }
}
