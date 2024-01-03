/* eslint-disable @typescript-eslint/naming-convention */
import { Component } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { WarningDialogComponent } from '@app/components/config-selection/warning-dialog/warning-dialog.component';
import { DIALOG_CUSTOM_CONGIF } from '@app/constants/dialog-config';
import { HistoryService } from '@app/services/game-config/history.service';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx'; // Importez la bibliothèque XLSX

@Component({
    selector: 'app-history',
    templateUrl: './history.component.html',
    styleUrls: ['./history.component.scss'],
})
export class HistoryComponent {
    constructor(public historyService: HistoryService, public dialogRef: MatDialogRef<HistoryComponent>, public dialog: MatDialog) {}

    resetHistory() {
        const warningDialogRef = this.warnUser();
        warningDialogRef.afterClosed().subscribe((confirmed: boolean) => {
            if (confirmed) this.historyService.resetHistory();
        });
    }

    closeDialog() {
        this.dialogRef.close();
    }

    // Fonction pour exporter les données au format Excel
    exportToExcel() {
        // Créez un tableau de données à exporter en excluant les colonnes booléennes
        const dataToExport = this.historyService.gamesHistory.map((game) => ({
            Début: game.beginning,
            Durée: game.duration,
            Mode: game.gameMode,
            Joueur1: game.player1,
            Joueur2: game.player2,
        }));

        const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(dataToExport);
        const workbook: XLSX.WorkBook = { Sheets: { data: worksheet }, SheetNames: ['data'] };
        const excelBuffer: ArrayBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer;
        const blob = new Blob([new Uint8Array(excelBuffer)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
        saveAs(blob, 'historique_des_parties.xlsx');
    }

    private warnUser() {
        const dialogConfig = Object.assign({}, DIALOG_CUSTOM_CONGIF);
        dialogConfig.data = 'supprimer l`historique des parties jouées';
        return this.dialog.open(WarningDialogComponent, dialogConfig);
    }
}
