/* eslint-disable no-underscore-dangle */
import { Component, OnInit } from '@angular/core';
import { HistoryService } from '@app/services/game-config/history.service';
import { UserService } from '@app/services/user/user.service';
import { History } from '@common/interfaces/records/history';
import { User, UserRanking } from '@common/interfaces/user';
import { ChartData } from 'chart.js';
import { environment } from 'src/environments/environment';
@Component({
    selector: 'app-stat',
    templateUrl: './stat.component.html',
    styleUrls: ['./stat.component.scss'],
})
export class StatComponent implements OnInit {
    imageBaseUrl = environment.serverUrl + '/users/get-public-profile-image/';
    gameTypeLabels: string[] = ['Classique 1v1', 'Classique Solo', 'Tps Limité Coop', 'Tps Limité Solo'];
    gameTypeData: ChartData<'pie', number[]> = { labels: this.gameTypeLabels, datasets: [{ data: [] }] };

    gameOutcomeData: ChartData<'pie', number[]> = { labels: ['Won', 'Lost', 'Abandoned'], datasets: [{ data: [] }] };
    // TODO: move this to class/interface
    stats = {
        gamesPlayed: 0,
        gamesWon: 0,
        gamesLost: 0,
        gamesAbandoned: 0,
        gamesPlayedClassic1v1: 0,
        gamesPlayedClassicSolo: 0,
        gamesPlayedLimitedTimeCoop: 0,
        gamesPlayedLimitedTimeSolo: 0,
        averageGameDuration: 0,
        averageNumberOfDifferencesFound: 0,
    };
    userHistory: History[] = [];
    ranking: UserRanking[] = [];
    // Updated mock data for activeUsers to include profile pictures
    activeUsers: User[] = [];
    constructor(private userService: UserService, private historyService: HistoryService) {}


    ngOnInit() {
        this.historyService.updateHistory();
        this.userHistory = this.filterHistory(this.historyService.gamesHistory);
        this.userService.getRanking().subscribe((ranking) => {
            this.ranking = ranking;
        });
    }

    // Mocked function to update username
    filterHistory(history: History[]): History[] {
        this.userService.currentUser$.subscribe((user) => {
            if (!user) return;
            const filteredHistory = history.filter(
                (game) => game.player1UserId === user.userId.toString() || game.player2UserId === user.userId.toString(),
            );
            this.extractStats(filteredHistory);
            return filteredHistory;
        });
        return [];
    }
    extractStats(history: History[]): void {
        const classic1v1 = history.filter((game) => game.gameMode === 'Classique 1v1');
        const classicSolo = history.filter((game) => game.gameMode === 'Classique Solo');
        const limitedTimeCoop = history.filter((game) => game.gameMode === 'Tps Limité Coop');
        const limitedTimeSolo = history.filter((game) => game.gameMode === 'Tps Limité Solo');
        this.stats.gamesPlayed = history.length;
        this.stats.gamesPlayedClassic1v1 = classic1v1.length;
        this.stats.gamesPlayedClassicSolo = classicSolo.length;
        this.stats.gamesPlayedLimitedTimeCoop = limitedTimeCoop.length;
        this.stats.gamesPlayedLimitedTimeSolo = limitedTimeSolo.length;
        this.stats.gamesWon = history.filter((game) => game.winner1).length;
        this.stats.gamesLost = history.filter((game) => !game.winner1 && !game.deserter1).length;
        this.stats.gamesAbandoned = history.filter((game) => game.deserter1).length;
        this.stats.averageNumberOfDifferencesFound = history.reduce((acc, curr) => acc + curr.player1noDifferenceFound, 0) / history.length;
        this.stats.averageGameDuration = history.reduce((acc, curr) => acc + curr.duration.seconds + curr.duration.minutes * 60, 0) / history.length;
        this.gameTypeData.datasets[0].data = [
            this.stats.gamesPlayedClassic1v1,
            this.stats.gamesPlayedClassicSolo,
            this.stats.gamesPlayedLimitedTimeCoop,
            this.stats.gamesPlayedLimitedTimeSolo,
        ];

        this.gameOutcomeData.datasets[0].data = [this.stats.gamesWon, this.stats.gamesLost, this.stats.gamesAbandoned];
    }
}
