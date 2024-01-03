package com.example.jeudifferences

import android.graphics.Bitmap
import org.json.JSONObject


data class gameData(val gameMode:Int, val playerName: String )
data class gameData1 (val gameMode: Int, val startEarly: Boolean)
data class gameData2 (val gameMode: Int, val cardId:String, val playerName: String)
data class gameData3(val playersId:ArrayList<String>, val gameId:String, val canJoin:Boolean)
data class gameData4(val playerName: String, val playerNbr:String, val gameId: String, val gameName:String,
                     val difficulty:String, val differenceNbr:String, val originalImage:ByteArray,
                     val modifiedImage:ByteArray, val timetillStart:String, val playerNames:ArrayList<String>, val gameMode: Int, val gameValues: JSONObject)
data class FriendRequest(val from:User, val to:User, val status:String)
data class User(val userId: String, val username: String)
data class Payload(val friendRequest: FriendRequest)
data class PlayerClickFalse(val x: Float, val y: Float, val isModified:Boolean, val time:Long)
data class PlayerClickValid(val x:Float, val y:Float, val time: Long, val flashOriginal:String, val differenceFlashOverlays: List<String>, val name:String)
data class inGameMessage(val message: message,val time: Long, val stringTime:String)


data class historyData(val startDate:String, val seconds:Int, val minutes:Int, val gameMode:Int)
data class player(val name:String, val winner: String, val deserter:String)

data class playerData(val name:String, val winner: String, val deserter:String, val nbrDifference:String )
data class VoiceMessage(val chatId:String, val message: ByteArray, val sender:String )

