package com.example.jeudifferences

class MessageData {
    var message: String? = null
    var gameId: String? = null
    var time: String? = null

    constructor()
    constructor(message: String, gameId: String, time: String) {
        this.message = message
        this.gameId = gameId
        this.time = time
    }
}
