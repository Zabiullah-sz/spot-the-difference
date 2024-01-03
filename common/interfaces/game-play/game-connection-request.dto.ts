import { GameMode } from '@common/enums/game-play/game-mode';
import { Difficulty } from '../../enums/game-play/difficulty';
import { GameConnectionAttemptResponseType } from '../../enums/game-play/game-connection-attempt-response-type';
import { GameValues } from './game-values';

export interface GameConnectionRequestOutputMessageDto {
    responseType: GameConnectionAttemptResponseType;
    gameName: string;
    playerNbr: number;
    startingIn: number;
    originalImage: string;
    modifiedImage: string;
    time: number;
    gameId: string;
    difficulty: Difficulty;
    differenceNbr: number;
    hostName: string;
    gameValues: GameValues;
    players: string[];
    isSettingSpecified: boolean;
}
export interface GameConnectionObserverResponseTypeDto {
    gameMode: GameMode;
    responseType: string;
    gameName: string;
    playerNbr: number;
    startingIn: number;
    originalImage: string;
    modifiedImage: string;
    time: number;
    gameId: string;
    difficulty: Difficulty;
    differenceNbr: number;
    hostName: string;
    gameValues: GameValues;
    players: string[];
}
