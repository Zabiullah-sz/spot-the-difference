import { TimeConcept } from '../general/time-concept';

export interface History {
    beginning: string;
    duration: TimeConcept;
    gameMode: string;
    player1UserId: string;
    player1: string;
    winner1: boolean;
    deserter1: boolean;
    player2UserId?: string;
    player2?: string;
    winner2?: boolean;
    deserter2?: boolean;
    player1noDifferenceFound: number;
    player2noDifferenceFound?: number;
}
