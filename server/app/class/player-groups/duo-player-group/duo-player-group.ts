import Player from '@app/class/game-logic/player/player';
import { UserGame } from '@app/gateways/game.gateway.constants';
import PlayerGroup from '@app/class/player-groups/default-player-group/player-group';

export default class DuoPlayerGroup extends PlayerGroup {
    constructor(user: UserGame, onJoin?: (player: Player) => void) {
        super(2, 2);
        this.joinUser(user, onJoin);
    }
}
