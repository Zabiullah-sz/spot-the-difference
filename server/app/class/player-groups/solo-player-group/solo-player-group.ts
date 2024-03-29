import Player from '@app/class/game-logic/player/player';
import PlayerGroup from '@app/class/player-groups/default-player-group/player-group';
import { UserGame } from '@app/gateways/game.gateway.constants';

export default class SinglePlayerGroup extends PlayerGroup {
    constructor(user: UserGame, onJoin?: (player: Player) => void) {
        super(1, 1);
        this.joinUser(user, onJoin);
    }
}
