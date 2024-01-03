/* eslint-disable @typescript-eslint/naming-convention */
import { Injectable, OnDestroy } from '@angular/core';
import { Event } from '@common/socket-event-constants';

import { Socket, io } from 'socket.io-client';
import { environment } from 'src/environments/environment';

@Injectable({
    providedIn: 'root',
})
export class SocketClientService implements OnDestroy {
    readonly MUTE_USER = new Event('mute');
    readonly UNMUTE_USER = new Event('unMute');
    clientSocket: Socket;
    private muted = false; // Variable pour suivre l'état de mute/unmute
    // Méthode pour activer ou désactiver la mise en sourdine
    toggleMute() {
        this.muted = !this.muted;
    }
    ngOnDestroy() {
        this.disconnect();
    }

    isSocketAlive() {
        return this.clientSocket && !this.clientSocket.disconnected;
    }

    connect() {
        if (!this.isSocketAlive()) {
            this.clientSocket = io(environment.socketUrl, { transports: ['websocket'], upgrade: false });
        }
    }

    disconnect() {
        this.clientSocket = this.clientSocket.disconnect();
    }

    on<T>(event: Event<T>, action: (data: T) => void): void {
        this.clientSocket.on(event.name, action);
    }
    off<T>(event: Event<T>): void {
        this.clientSocket.off(event.name, () => {});
    }

    send<T>(event: string, data?: T): void {
        if (!this.muted) {
            if (data) {
                this.clientSocket.emit(event, data);
            } else {
                this.clientSocket.emit(event);
            }
        }
    }

    removeListener<T>(event: Event<T>) {
        this.clientSocket.removeAllListeners(event.name);
    }
}
