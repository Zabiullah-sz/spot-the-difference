import { HistoryService } from '@app/services/game-config/history.service';
/* eslint-disable max-params */
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { GameConstantsComponent } from '@app/components/config-selection/game-constants/game-constants.component';
import { HistoryComponent } from '@app/components/config-selection/history/history.component';
import { WarningDialogComponent } from '@app/components/config-selection/warning-dialog/warning-dialog.component';
import { DIALOG_CUSTOM_CONGIF } from '@app/constants/dialog-config';
import { GameSelection } from '@app/interfaces/game-card/game-selection';
import { GameListManagerService } from '@app/services/divers/game-list-manager.service';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { SoundService } from '@app/services/game-play/sound.service';
import { GameSelectorService } from '@app/services/game-selection/game-selector.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
    selector: 'app-config-page',
    templateUrl: './config-page.component.html',
    styleUrls: ['./config-page.component.scss'],
})
export class ConfigPageComponent implements OnInit, OnDestroy {
    buttonNames: [string, string, string] = ['Supprimer', 'Réinitialiser top 3', 'Réinitialiser top 3'];
    selectedSuccessSoundIndex: number = 1;
    selectedErrorSoundIndex: number = 1;
    private componentDestroyed$: Subject<void> = new Subject<void>();

    constructor(
        public dialog: MatDialog,
        public selectorService: GameSelectorService,
        public gameListManager: GameListManagerService,
        public socketService: SocketClientService,
        public soundService: SoundService,
        public historyService: HistoryService,
    ) {}

    ngOnInit(): void {
        this.selectorService.selectionValue.pipe(takeUntil(this.componentDestroyed$)).subscribe(async (values) => this.clickHandler(values));
    }

    ngOnDestroy(): void {
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
    }
    exportHistoryToExcel() {
        this.historyService.exportToExcel(this.historyService.gamesHistory, 'historique_des_parties.xlsx');
    }
    playSuccessSound(): void {
        this.soundService.setSelectedSuccessSound(this.selectedSuccessSoundIndex);
        this.soundService.playSuccess();
    }

    playErrorSound(): void {
        this.soundService.setSelectedErrorSound(this.selectedErrorSoundIndex);
        this.soundService.playError();
    }

    requestGameConstants(): void {
        this.dialog.open(GameConstantsComponent, DIALOG_CUSTOM_CONGIF);
    }

    requestHistory(): void {
        this.dialog.open(HistoryComponent, DIALOG_CUSTOM_CONGIF);
    }

    warnPlayer(action: string) {
        const dialogConfig = Object.assign({}, DIALOG_CUSTOM_CONGIF);
        dialogConfig.data = action;
        return this.dialog.open(WarningDialogComponent, dialogConfig);
    }

    deleteAllGames() {
        const dialogRef = this.warnPlayer('supprimer tous les jeux');
        dialogRef.afterClosed().subscribe((confirmed: boolean) => {
            if (confirmed) this.gameListManager.deleteAllGames();
        });
    }

    resetAllBestTimes() {
        const dialogRef = this.warnPlayer('réinitialiser les meilleurs temps de tous les jeux');
        dialogRef.afterClosed().subscribe((confirmed: boolean) => {
            if (confirmed) this.gameListManager.resetAllBestTimes();
        });
    }
    selectSuccessSound(index: number) {
        this.selectedSuccessSoundIndex = index;
        this.soundService.setSelectedSuccessSound(index);
    }

    // Méthode pour sélectionner le son d'erreur
    selectErrorSound(index: number) {
        this.selectedErrorSoundIndex = index;
        this.soundService.setSelectedErrorSound(index);
    }
    confirmSuccessSound(): void {
        // Enregistrez l'indice sélectionné pour le son de succès
        this.selectSuccessSound(this.selectedSuccessSoundIndex);
        // Vous pouvez également ajouter du code supplémentaire ici, par exemple, pour afficher un message de confirmation.
    }

    confirmErrorSound(): void {
        // Enregistrez l'indice sélectionné pour le son d'erreur
        this.selectErrorSound(this.selectedErrorSoundIndex);
        // Vous pouvez également ajouter du code supplémentaire ici, par exemple, pour afficher un message de confirmation.
    }

    private deleteGame(id: string) {
        const dialogRef = this.warnPlayer('supprimer ce jeu');
        dialogRef.afterClosed().subscribe((confirmed: boolean) => {
            if (confirmed) this.gameListManager.deleteGame(id);
        });
    }

    private clickHandler(values: GameSelection) {
        if (values.buttonName === this.buttonNames[0]) {
            this.deleteGame(values.id);
        } else {
            this.resetBestTimes(values.id);
        }
    }

    private resetBestTimes(id: string) {
        const dialogRef = this.warnPlayer('réinitialiser les meilleurs temps de ce jeu');
        dialogRef.afterClosed().subscribe((confirmed: boolean) => {
            if (confirmed) this.gameListManager.resetBestTimes(id);
        });
    }

    // Méthode pour sélectionner le son de succès
}
