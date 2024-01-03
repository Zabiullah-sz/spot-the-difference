import { GameMode }  from '../enums/game-play/game-mode';

import { User } from './user';
export interface ActiveGame {
    gameMode: GameMode;
    id: string;
    players: User[];
}