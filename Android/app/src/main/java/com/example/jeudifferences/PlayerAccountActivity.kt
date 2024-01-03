package com.example.jeudifferences

import SocketClientHandler
import ThemePreferences
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.DocumentsContract
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jeudifferences.ApiClient.userApi
import com.google.gson.Gson
import io.socket.emitter.Emitter
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class PlayerAccountActivity : BaseActivity() {
    lateinit var excelButton:Button
    lateinit var emailButton:Button
    lateinit var sendRequest:Button
    lateinit var friendBox:EditText
    // for normal requests
    lateinit var friendRecycler: RecyclerView
    lateinit var friendAdapter: friendAdapter

    //for pending requests
    lateinit var friendPendingRecycler: RecyclerView
    lateinit var friendPendingAdapter: pendingRequestAdapter

    val clientSocket = SocketClientHandler
    val createFile = 0
    var recordList = arrayListOf<historyData>()
    val allPlayers = arrayListOf<player>()
    var allActivePlayer = arrayListOf<User>()
    var friendRequests = arrayListOf<FriendRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_account)
        setTheme()
        clientSocket.connect()
        clientSocket.on("all_active_users", onActivePlayers)
        clientSocket.on("friend_request", onFriendRequest)
        clientSocket.on("notification", onNotification)
        excelButton = findViewById(R.id.excel)
        emailButton = findViewById(R.id.email)
        sendRequest = findViewById(R.id.sendRequest)
        friendBox = findViewById(R.id.friendBox)
        var backToMain = findViewById<Button>(R.id.backToMain6)

        //Log.i("friendss", "$myId")
        //normal friend requests
        friendRecycler = findViewById(R.id.possibleFriendsView)
        allActivePlayer = ArrayList()
        friendAdapter = friendAdapter(this, allActivePlayer)
        friendRecycler.layoutManager = LinearLayoutManager(this)
        friendRecycler.adapter = friendAdapter

        //pending friends requests
        friendPendingRecycler = findViewById(R.id.pendingRequests)
        friendRequests = ArrayList()
        friendPendingAdapter = pendingRequestAdapter(this, friendRequests)
        friendPendingRecycler.layoutManager = LinearLayoutManager(this)
        friendPendingRecycler.adapter = friendPendingAdapter

        onPendingRequests()
        excelButton.setOnClickListener {
            getData()
        }
        emailButton.setOnClickListener {
            shareLink()
        }
        sendRequest.setOnClickListener {
            val friendName = friendBox.text.toString()
            val gson = Gson()

            if (friendName.isNotEmpty()) {
                val payload = JSONObject().apply {
                    put("username", friendName)
                }
                val jsonString = gson.toJson(payload)
                clientSocket.send("friend_request", payload)
            } else {
               Toast.makeText(this, "Veuillez rentrer un nom", Toast.LENGTH_SHORT).show()
            }
        }

        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            clientSocket.removeListener("all_records")
            clientSocket.removeListener("get_all_records")
            clientSocket.removeListener("friend_request_response")
            clientSocket.removeListener("all_active_users")
            clientSocket.removeListener("friend_request")
            clientSocket.removeListener("notification")
            startActivity(mainPageActivity)
        }
    }

    private fun getData() {
        clientSocket.send("get_all_records")
        clientSocket.on("all_records", onAllRecords)
    }

    fun parseComplexDate(dateStr: String): Date? {
        val dateFormat = SimpleDateFormat("E MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)", Locale.CANADA)

        dateFormat.timeZone = TimeZone.getTimeZone("GMT")

        try {
            return dateFormat.parse(dateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return null
    }

    fun formatDateToString(date: Date, outputFormat: String): String {
        val outputFormatter = SimpleDateFormat(outputFormat, Locale.CANADA)
        return outputFormatter.format(date)
    }

    private val onNotification = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                val dat = args[0] as JSONObject
                val message = dat.getString("msg")
                //Log.i("kikii", "$dat")
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val customView = inflater.inflate(R.layout.custom_friend_request_notif, null)

                val errorMessageText = customView.findViewById<TextView>(R.id.friendMessage)
                errorMessageText.text = message

                val popupWindowOriginal = PopupWindow(
                    customView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                popupWindowOriginal.showAtLocation(customView, Gravity.TOP, 0, 0)

                Handler().postDelayed({
                    popupWindowOriginal.dismiss() }, 2000)
            }
        }
    }

    private val onActivePlayers = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                allActivePlayer.clear()
                val da = args[0] as JSONArray
                for (i in 0 until da.length()) {
                    val element = da.getJSONObject(i)
                    val userId = element.getString("userId")
                    val userName = element.getString("username")
                    //Log.i("kiki", "$userId, $userName")
                    if(userId != myId) {
                        allActivePlayer.add(User(userId, userName))
                        friendAdapter.notifyDataSetChanged()
                    }
                    //friendRecycler.scrollToPosition(principalMessagesArray.size -1)
                }
            }
        }
    }

    private val onFriendRequest = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0] as JSONObject
                val fromUser = d.getJSONObject("from")
                val fromId = fromUser.getString("userId")
                val fromuserName = fromUser.getString("username")
                val toUser = d.getJSONObject("to")
                val toId = toUser.getString("userId")
                val touserName = toUser.getString("username")
                var status = d.getString("status")
                //Log.i("kiki", "$d")

                if(status == "accepted") {
                    Toast.makeText(this, "Vous êtes mtn amis avec $touserName", Toast.LENGTH_SHORT).show()
                }

                if(status == "pending") {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage("Vous avez une nouvelle demande d'ami de $fromuserName")
                    builder.setCancelable(false)
                    builder.setNegativeButton("Rejeter") { dialog: DialogInterface, _: Int ->
                        val gson = Gson()
                        val from = User(fromId, fromuserName)
                        val to = User(toId, touserName)
                        val friendRequest = FriendRequest(from, to, "declined")
                        val payload = Payload(friendRequest)
                        val jsonString = gson.toJson(payload)
                        clientSocket.send("friend_request_response", jsonString)
                        dialog.dismiss()
                    }
                    builder.setPositiveButton("Accepter") {dialog: DialogInterface, _: Int ->
                        val gson = Gson()
                        val from = User(fromId,fromuserName)
                        val to = User(toId,touserName)
                        val friendRequest = FriendRequest(from, to, "accepted")
                        val payload = Payload(friendRequest)
                        val jsonString = gson.toJson(payload)
                        clientSocket.send("friend_request_response", jsonString)
                        dialog.dismiss()
                        //Log.i("kiki", "$fromId, $fromuserName, $toId, $touserName")
                    }
                    val alertfriend = builder.create()
                    alertfriend?.show()
                }
            }
        }
    }

    private val onAllRecords = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                val dataArray = args[0] as JSONArray
                var stringDate= ""

                for (i in 0 until dataArray.length()) {
                    val element = dataArray.getJSONObject(i)
                    val dateStr = element.getString("startDate")
                    val duration =element.getJSONObject("duration")
                    val gameMode = element.getString("gameMode").toString().toInt()
                    val seconds = duration.getString("seconds").toString().toInt()
                    val minutes =duration.getString("minutes").toString().toInt()

                    val parsedDate = parseComplexDate(dateStr)
                    if (parsedDate != null) {
                        val outputFormat = "yyyy-MM-dd HH:mm:ss"
                        stringDate = formatDateToString(parsedDate, outputFormat)
                        //Log.i("hello","$formattedDateStr")
                    }
                    val players = element.getJSONArray("players")
                    for (j in 0 until players.length()) {
                        val player = players.getJSONObject(j)
                        val name = player.getString("name").toString()
                        val deserter = player.getBoolean("deserter").toString()
                        val winner = player.getBoolean("winner").toString()
                        val addedPlayer = player(name, winner,deserter)
                        allPlayers.add(addedPlayer)
                    }
                    val record = historyData(stringDate, seconds, minutes,gameMode)
                    recordList.add(record)
                    //Log.i("helloo","$players")
                }
                val intentSave = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/vnd.ms-excel"
                    putExtra(Intent.EXTRA_TITLE, "fileName.xls")
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
                }
                startActivityForResult(intentSave, createFile)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == createFile && resultCode == RESULT_OK) {
            val hssfWorkbook = HSSFWorkbook()
            val hssfSheet = hssfWorkbook.createSheet("Historique de parties")
            val hssfRow1 = hssfSheet.createRow(0)
            val hssfCell1 = hssfRow1.createCell(0)
            val hssfCell2 = hssfRow1.createCell(1)
            val hssfCell3 = hssfRow1.createCell(2)
            val hssfCell4 = hssfRow1.createCell(3)

            val hssfSheet2 = hssfWorkbook.createSheet("Tous les joueurs")
            val hssfRowAllPlayers = hssfSheet2.createRow(0)
            val hssfCell5 = hssfRowAllPlayers.createCell(0)
            val hssfCell6 = hssfRowAllPlayers.createCell(1)
            val hssfCell7 = hssfRowAllPlayers.createCell(2)

            hssfCell1.setCellValue("Date")
            hssfCell2.setCellValue("Seconds")
            hssfCell3.setCellValue("Minutes")
            hssfCell4.setCellValue("GameMode")
            hssfCell5.setCellValue("Player name")
            hssfCell6.setCellValue("Winner")
            hssfCell7.setCellValue("Deserter")

            for (i in 0 until recordList.size) {
                val hssfRow = hssfSheet.createRow(i + 1)

                // Populate the date in column 0
                val hssfCellTime = hssfRow.createCell(0)
                hssfCellTime.setCellValue(recordList[i].startDate)

                // Populate the seconds in column 1
                val hssfCellSensor1 = hssfRow.createCell(1)
                hssfCellSensor1.setCellValue(recordList[i].seconds.toDouble())

                // Populate the minutes in column 2
                val hssfCellSensor2 = hssfRow.createCell(2)
                hssfCellSensor2.setCellValue(recordList[i].minutes.toDouble())

                // Populate the gameMode in column 3
                val hssfCellSensor3 = hssfRow.createCell(3)
                hssfCellSensor3.setCellValue(recordList[i].gameMode.toDouble())
            }

            for (j in 0 until allPlayers.size) {
                val hssfRowplayer = hssfSheet2.createRow(j+1)
                val hssfCellSensor4 = hssfRowplayer.createCell(0)
                hssfCellSensor4.setCellValue(allPlayers[j].name)
                val hssfCellSensor5 = hssfRowplayer.createCell(1)
                hssfCellSensor5.setCellValue(allPlayers[j].winner)
                val hssfCellSensor6 = hssfRowplayer.createCell(2)
                hssfCellSensor6.setCellValue(allPlayers[j].deserter)
            }

            val uri = data!!.data
            try {
                val outputStream = this.contentResolver.openOutputStream(uri!!)
                if (outputStream != null) {
                    hssfWorkbook.write(outputStream)
                    hssfWorkbook.close()
                }
            }
            catch (e:Exception) {
                print(e.localizedMessage)
            }
        }
    }


    //inspiré d'une réponse sur stackOverflow
    fun shareLink() {
        val inviteMessage = StringBuilder()
        inviteMessage.append("Rejoins-moi en cliquant sur le lien et en téléchargant le jeu!")
        inviteMessage.append("\n")
        inviteMessage.append("\n")
        inviteMessage.append("https://drive.google.com/file/d/1wBGbPIuPy-COkDHiXBvO0n4qkFNZfR6l/view?usp=drive_link")

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "*/*"
            shareIntent.putExtra(Intent.EXTRA_TEXT, inviteMessage.toString())
            try {
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(applicationContext, "No App Available", Toast.LENGTH_SHORT).show()
            }

    }

    fun onPendingRequests() {
        val userId = myId
        val call = userApi.getFriendRequests(userId)

        call.enqueue(object : Callback<ArrayList<FriendRequest>> {
            override fun onResponse(call: Call<ArrayList<FriendRequest>>, response: Response<ArrayList<FriendRequest>>) {
                if (response.isSuccessful) {
                    val data  = response.body()!!
                    var friendPending:FriendRequest
                    for(i in 0 until data.size) {
                        friendPending = FriendRequest(data[i].from, data[i].to, data[i].status)
                        friendRequests.add(friendPending)
                    }
                    friendPendingAdapter.notifyDataSetChanged()
                    //Log.i("friendss", "$friendRequests")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("friendss", "Error: ${response.code()}, $errorBody")
                }
            }

            override fun onFailure(call: Call<ArrayList<FriendRequest>>, t: Throwable) {
                // Handle failure
            }
        })
    }

    companion object {
        lateinit var myId:String
    }

}
