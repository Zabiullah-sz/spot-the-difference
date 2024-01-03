package com.example.jeudifferences

class message {
    var message: String? = null
    var time: String? = null
    var senderName: String? = null
    var isMessageText: Boolean? = true
    var isCheating:Boolean? = false
    var isDeserter:Boolean? = false
    var isClickEnemy:Boolean? = false
    var isValid:Boolean? = false
    var isGlobal:Boolean? = false
    var isKicked:Boolean?= false

    constructor()

    constructor(message: String, senderName: String, isGlobal: Boolean, isDeserter: Boolean, isCheating: Boolean) {
        this.message = message
        this.senderName = senderName
        this.isGlobal = isGlobal
        this.isDeserter = isDeserter
        this.isCheating = isCheating
    }

    constructor(message: String, senderName: String, isGlobal: Boolean, isDeserter: Boolean, isCheating: Boolean, isKicked: Boolean) {
        this.message = message
        this.senderName = senderName
        this.isGlobal = isGlobal
        this.isDeserter = isDeserter
        this.isCheating = isCheating
        this.isKicked = isKicked
    }

    constructor(message: String, senderName: String, time: String? = null, isGlobal: Boolean, isDeserter: Boolean, isCheating: Boolean, isKicked: Boolean) {
        this.message = message
        this.senderName = senderName
        this.isGlobal = isGlobal
        this.isDeserter = isDeserter
        this.isCheating = isCheating
        this.isKicked = isKicked
        this.time = time
    }




    constructor(message: String, senderName: String, isMessageText:Boolean, isCheating:Boolean, isDeserter:Boolean,
                isClickEnemy:Boolean, isValid:Boolean, isGlobal:Boolean) {
        this.message = message
        this.senderName = senderName
        this.isMessageText = isMessageText
        this.isCheating = isCheating
        this.isDeserter = isDeserter
        this.isClickEnemy = isClickEnemy
        this.isValid = isValid
        this.isGlobal = isGlobal
    }


    constructor(message: String, senderName: String) {
        this.message = message
        this.senderName = senderName
    }
}
