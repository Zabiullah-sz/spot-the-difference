import DifferenceManager from '@app/class/game-logic/difference-manager/difference-manager';
import Timer from '@app/class/watch/timer/timer';
import { UserGame } from '@app/gateways/game.gateway.constants';
import { User } from '@common/interfaces/user';
import { Socket } from 'socket.io';

export default class Player {
    name: string;
    client: Socket;
    userId: string;

    differencesFound = 0;
    differenceManager: DifferenceManager | undefined;
    protected penaltyTimer: Timer;

    constructor(user: UserGame, public downTime = false) {
        this.name = user.name;
        this.client = user.client;
        this.userId = user.userId;
        this.penaltyTimer = new Timer();
    }
    get getUser(): User {
        return {
            username: this.name,
            userId: this.userId,
        };
    }

    startPenalty(seconds: number, onEnd?: () => void) {
        if (this.downTime && this.penaltyTimer.hasMoreTimeThan(seconds)) return;
        this.downTime = true;
        this.penaltyTimer.onEnd = () => {
            this.downTime = false;
            if (onEnd) onEnd();
        };
        this.penaltyTimer.set(seconds).start();
    }
}
