package com.example.jeudifferences

class ActiveGames {
    var gameMode: String?= null
    var id: String?=null
    var playersNbr: Int? = null

    constructor(gameMode: String, id: String, playersNbr: Int) {
        this.gameMode = gameMode
        this.id = id
        this.playersNbr = playersNbr
    }
}
