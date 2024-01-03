import { Injectable } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class ThemeService {
    private currentTheme = localStorage.getItem('selectedTheme') || 'light-theme';

    setTheme(theme: string) {
        this.currentTheme = theme;
        document.body.className = theme;
        localStorage.setItem('selectedTheme', theme); // Sauvegarde du thème dans le stockage local
    }

    getTheme() {
        return this.currentTheme;
    }
}
