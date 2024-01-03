package com.example.jeudifferences

class GameSettings {
    var gameId: String? = null
    var timerTime: Int? = null
    var isCheatAllowed: Boolean? = null

    constructor()
    constructor(gameId: String, timerTime: Int, isCheatAllowed: Boolean) {
        this.gameId = gameId
        this.timerTime = timerTime
        this.isCheatAllowed = isCheatAllowed
    }

}
