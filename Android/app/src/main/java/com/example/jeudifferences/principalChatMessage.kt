package com.example.jeudifferences

class principalChatMessage {
    var chatId: String? = null
    var message: String? = null
    var sender: String? = null
    var type:String?= null
    var userId:String?= null

    constructor()
    constructor(chatId:String, message: String, sender: String, type:String, userId: String) {
        this.chatId = chatId
        this.message = message
        this.sender = sender
        this.type= type
        this.userId = userId
    }
    constructor(chatId:String, message: String, sender: String, type:String) {
        this.chatId = chatId
        this.message = message
        this.sender = sender
        this.type= type
        this.userId = userId
    }
}
