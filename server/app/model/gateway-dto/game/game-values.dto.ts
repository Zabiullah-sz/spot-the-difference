import { GameValues } from '@common/interfaces/game-play/game-values';
import { Optional } from '@nestjs/common';
import { IsDefined, IsNumber, IsPositive, Max } from 'class-validator';
import { LIMITED_TIME_MAX_TIME } from './game.constants';

export class GameValuesInputFilter implements GameValues {
    @IsDefined()
    @IsNumber()
    @Max(LIMITED_TIME_MAX_TIME)
    @IsPositive()
    timerTime: number;

    @Optional()
    @IsDefined()
    @IsNumber()
    @IsPositive()
    penaltyTime: number;

    @Optional()
    @IsDefined()
    @IsNumber()
    @IsPositive()
    gainedTime: number;

    @IsDefined()
    @IsNumber()
    @IsPositive()
    isCheatAllowed: boolean;
}
