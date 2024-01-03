import { Injectable } from '@angular/core';
import { HOUR_INDEX } from '@app/constants/time-constants';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { GameMode } from '@common/enums/game-play/game-mode';
import { History } from '@common/interfaces/records/history';
import { Record } from '@common/interfaces/records/record';
import { FromServer, ToServer } from '@common/socket-event-constants';
import { format } from 'date-fns';
import { saveAs } from 'file-saver';
import * as XLSX from 'xlsx';

@Injectable({
    providedIn: 'root',
})
export class HistoryService {
    gamesHistory: History[] = [];
    initialized = false;

    constructor(private readonly socketService: SocketClientService) {}

    recordToHistory(record: Record): History {
        const gameTime = record.startDate.substring(0, record.startDate.indexOf('GMT'));

        return {
            beginning: format(new Date(record.startDate), 'dd/MM/yyyy') + ' - ' + gameTime.substring(HOUR_INDEX),

            duration: record.duration,
            gameMode: this.getGameModeString(record.gameMode),
            player1UserId: record.players[0].userId,
            player1: record.players[0].name,
            player1noDifferenceFound: record.players[0].noDifferenceFound,
            player2UserId: record.players[1]?.userId,
            player2: record.players[1]?.name,
            player2noDifferenceFound: record.players[1]?.noDifferenceFound,
            winner1: record.players[0].winner,
            deserter1: record.players[0].deserter,
            winner2: record.players[1]?.winner,
            deserter2: record.players[1]?.deserter,
        };
    }

    init() {
        if (!this.initialized) {
            this.socketService.on(FromServer.ALL_RECORDS, (records: Record[]) => {
                for (const record of records) this.gamesHistory.unshift(this.recordToHistory(record));
            });
            this.socketService.send(ToServer.GET_ALL_RECORDS);
            this.socketService.on(FromServer.SPREAD_HISTORY, (record: Record) => this.gamesHistory.unshift(this.recordToHistory(record)));
            this.initialized = true;
        }
    }
    updateHistory() {
        this.socketService.send(ToServer.GET_ALL_RECORDS);
    }
    resetHistory() {
        this.socketService.send(ToServer.DELETE_ALL_RECORDS);
        this.gamesHistory = [];
    }

    getGameModeString(gameMode: GameMode): string {
        switch (gameMode) {
            case GameMode.Classic1v1:
                return 'Classique 1v1';
            case GameMode.ClassicSolo:
                return 'Classique Solo';
            case GameMode.LimitedTimeCoop:
                return 'Tps Limité Coop';
            case GameMode.LimitedTimeSolo:
                return 'Tps Limité Solo';
            default:
                return 'NaN';
        }
    }
    exportToExcel(history: History[], fileName: string) {
        if (history.length === 0) {
            alert('Aucune donnée à exporter.');
            return;
        }

        const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(history);
        const workbook: XLSX.WorkBook = { Sheets: { data: worksheet }, SheetNames: ['data'] };
        const excelBuffer: ArrayBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer;
        const blob = new Blob([new Uint8Array(excelBuffer)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });

        saveAs(blob, fileName);
    }
}
