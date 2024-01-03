/* eslint-disable @typescript-eslint/no-magic-numbers */
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { GameMode } from '@common/enums/game-play/game-mode';
import { SimpleUser } from '@common/interfaces/game-play/simple-user';
import { FromServer, ToServer } from '@common/socket-event-constants';
import { ModalService } from '@app/services/divers/modal.service';

@Component({
    selector: 'app-awaiting-players-modal',
    templateUrl: './awaiting-players-modal.component.html',
    styleUrls: ['./awaiting-players-modal.component.scss'],
})
export class AwaitingPlayersModalComponent implements OnDestroy, OnInit {
    static opened = false;
    acceptedPlayersId: string[] = [];
    // eslint-disable-next-line @typescript-eslint/member-ordering
    showChatbox: boolean = true; // Or whatever your default state is
    constructor(
        public dialogRef: MatDialogRef<AwaitingPlayersModalComponent>,
        @Inject(MAT_DIALOG_DATA)
        public data: { gameId: string; waitingPlayers: SimpleUser[]; displayMessage: number; playerNbr: number; isHost: boolean },
        private socketService: SocketClientService,
        private modalService: ModalService,
    ) {
        dialogRef.disableClose = true;
    }
    ngOnDestroy(): void {
        this.socketService.removeListener(FromServer.RESPONSE_TO_JOIN_GAME_REQUEST);
        this.socketService.removeListener(FromServer.PLAYER_STATUS);
        AwaitingPlayersModalComponent.opened = false;
        this.modalService.setModalDestroyState();
    }

    ngOnInit() {
        AwaitingPlayersModalComponent.opened = true;
        if (this.data.playerNbr === undefined || this.data.playerNbr === 0) this.data.playerNbr = 1;
        this.modalService.setModalInitState();
        console.log(this.data);
    }

    closeDialog() {
        this.dialogRef.close();
        this.socketService.send(ToServer.LEAVE_GAME, this.data.gameId);
    }

    startEarly() {
        if (this.data.displayMessage === 0) {
            this.socketService.send(ToServer.REQUEST_TO_PLAY, {
                gameMode: GameMode.LimitedTimeCoop,
                startEarly: true,
            });
        } else if (this.data.displayMessage === 1) {
            console.log(this.acceptedPlayersId);
            this.socketService.send(ToServer.PLAYER_VALIDATION, {
                playersId: this.acceptedPlayersId,
                gameId: this.data.gameId,
                canJoin: true,
            });
        }
    }

    removePlayer(canJoin: boolean, playerId: string, playerName: string) {
        const playerToRemove = this.data.waitingPlayers.find((player) => player.name === playerName);

        if (playerToRemove && playerToRemove.isAccepted) {
            this.data.playerNbr--;
        }
        this.socketService.send(ToServer.PLAYER_VALIDATION, {
            playersId: [playerId],
            gameId: this.data.gameId,
            canJoin,
        });
        this.acceptedPlayersId = this.acceptedPlayersId.filter((item) => item !== playerId);
    }

    validatePlayer(canJoin: boolean, playerId: string, playerName: string) {
        for (const player of this.data.waitingPlayers) {
            if (player.id === playerId) {
                player.isAccepted = true;
                this.data.playerNbr++;
                if (player.isAccepted) this.acceptedPlayersId.push(player.id);
            }
        }
        // match commence automatiquement lorsque nombre de joueur est complet
        if (this.data.playerNbr >= 4) {
            this.socketService.send(ToServer.PLAYER_VALIDATION, {
                playersId: this.acceptedPlayersId,
                gameId: this.data.gameId,
                canJoin,
            });
        }
    }
}
