package com.example.jeudifferences

import android.graphics.Bitmap
import android.net.Uri

class GameInfo {
    var gameName: String?= null
    var difficulty: String?=null
    var image: Bitmap? = null
    var gameId: String?= null
    var soloBestTimes: BestTimes?= null
    var multiBestTimes: BestTimes?= null

    constructor(gameName: String, difficulty: String, image: Bitmap, gameId: String, soloBestTimes: BestTimes, multiBestTimes: BestTimes) {
        this.gameName = gameName
        this.difficulty = difficulty
        this.image = image
        this.gameId = gameId
        this.soloBestTimes = soloBestTimes
        this.multiBestTimes = multiBestTimes
    }
}
