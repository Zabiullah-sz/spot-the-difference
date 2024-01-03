package com.example.jeudifferences

class CheaterInfo {
    lateinit var gameId: String
    lateinit var cheater: String
    lateinit var playerVote: String
    lateinit var voter: String


    constructor()
    constructor(gameId: String, cheater: String, playerVote: String, voter: String) {
        this.gameId = gameId
        this.cheater = cheater
        this.playerVote = playerVote
        this.voter = voter
    }
}
