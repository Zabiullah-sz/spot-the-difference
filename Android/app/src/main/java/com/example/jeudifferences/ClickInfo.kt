package com.example.jeudifferences

class ClickInfo {
    var gameId: String? = null
    var x: Int? = null
    var y: Int? = null

    constructor()
    constructor(gameId: String, x: Int, y: Int) {
        this.gameId = gameId
        this.x = x
        this.y = y
    }
}
