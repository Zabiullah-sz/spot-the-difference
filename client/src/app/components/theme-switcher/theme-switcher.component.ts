/* eslint-disable no-console */
import { Component } from '@angular/core';
import { ThemeService } from '@app/services/theme.service';

@Component({
    selector: 'app-theme-switcher',
    templateUrl: './theme-switcher.component.html',
    styleUrls: ['./theme-switcher.component.scss'],
})
export class ThemeSwitcherComponent {
    constructor(private themeService: ThemeService) {}

    get themeServiceInstance() {
        return this.themeService;
    }

    toggleTheme() {
        console.log('toggleTheme called');
        const currentTheme = this.themeService.getTheme();
        console.log('Current theme:', currentTheme);
        if (currentTheme === 'light-theme') {
            this.themeService.setTheme('dark-theme');
        } else {
            this.themeService.setTheme('light-theme');
        }
    }
}
