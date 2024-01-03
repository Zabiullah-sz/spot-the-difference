package com.example.jeudifferences

import SocketClientHandler
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.db.williamchart.view.DonutChartView
import io.socket.emitter.Emitter
import org.json.JSONArray
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone



class AccountStatActivity: AppCompatActivity() {
    private val clientSocket = SocketClientHandler // Assuming this is a singleton object
    private var recordList = arrayListOf<historyData>()
    private val gameModeNames = listOf("Classic1v1", "ClassicSolo", "LimitedTimeCoop", "LimitedTimeSolo")
    lateinit var backToMain: Button
    data class player2(val name:String, val winner: String, val deserter:String,val nbDiff:Int)

    private val allPlayers: MutableList<player2> = mutableListOf()
    private lateinit var donutChartView: DonutChartView
    private lateinit var donutChartTypeView: DonutChartView
    private lateinit var textTotalView: TextView
    private lateinit var textTemp: TextView
    private lateinit var textUser: TextView
    var GameTime:List<Float> =listOf()
    private var gameStat: List<Float> = listOf()
    private var gameTypesStat: List<Float> = listOf() // List for game types
    private var averageTime = 0
    private var averageDiff = 0
    private lateinit var textTypeGame: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_stats)
        backToMain = findViewById(R.id.backToMain)
        textTotalView = findViewById(R.id.totalGame)
        textTemp = findViewById(R.id.textTemp)
        textUser = findViewById(R.id.textUserName)
        textTypeGame = findViewById(R.id.typeGame)
        donutChartView = findViewById(R.id.donutChart)
        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            startActivity(mainPageActivity)
        }
        donutChartView.apply {


            val grayColor = Color.parseColor("#808080")
            val orangeColor = Color.parseColor("#FFA500")  // Orange
            val greenColor = Color.parseColor("#008000")   // Vert
            val redColor = Color.parseColor("#FF0000")     // Rouge

            donutColors = intArrayOf(
                greenColor,   // Couleur pour les parties gagnées
                redColor      // Couleur pour les parties perdues
            )
            animation.duration= 1000
            gameStat = listOf(1f, 1f)
            animate(gameStat)
        }

        donutChartTypeView = findViewById(R.id.donutChartType) // Make sure this ID exists in your layout
        donutChartTypeView.apply {
            // Define colors for different game types. Adjust as needed.
            val grayColor = Color.parseColor("#808080")
            val orangeColor = Color.parseColor("#FFA500")  // Orange
            val greenColor = Color.parseColor("#008000")   // Vert
            val redColor = Color.parseColor("#FF0000")     // Rouge

            donutColors = intArrayOf(
                grayColor,
                orangeColor,  // Couleur pour le total des parties
                greenColor,   // Couleur pour les parties gagnées
                redColor
            )
            gameTypesStat = listOf(0f, 0f, 0f, 0f)
            animation.duration = 1000
            animate(gameTypesStat)
        }

        clientSocket.connect()
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
    private fun updateGameTypeText() {
        val gameTypeCounts = countGamesByMode()
        val totalGameTypeCount = gameTypeCounts.sum()
        val stringBuilder = StringBuilder()

        gameModeNames.forEachIndexed { index, gameModeName ->
            if (index < gameTypeCounts.size) {
                val percentage = (gameTypeCounts[index].toFloat() / totalGameTypeCount) * 100
                stringBuilder.append("$gameModeName: ${String.format("%.2f", percentage)}%\n")
            }
        }

        textTypeGame.text = stringBuilder.toString()
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
                    }
                    val players = element.getJSONArray("players")
                    for (j in 0 until players.length()) {
                        val player = players.getJSONObject(j)
                        val name = player.getString("name").toString()
                        val deserter = player.getBoolean("deserter").toString()
                        val winner = player.getBoolean("winner").toString()
                        val nbDiff = player.getString("noDifferenceFound").toString().toInt()
                        val addedPlayer = player2(name, winner,deserter, nbDiff)

                        Log.i("helloWinner","$winner")
                         allPlayers.add(addedPlayer)
                        Log.i("liste palyer","$allPlayers")
                    }
                    val record = historyData(stringDate, seconds, minutes,gameMode)
                    recordList.add(record)
                    //Log.i("helloo","$players")
                }
                displayGameHistory()
            }

        }

    }

    private fun countGamesByMode(): List<Int> {
        val counts = IntArray(gameModeNames.size)
        for (record in recordList) {
            if (record.gameMode in 0 until gameModeNames.size) {
                counts[record.gameMode]++
            }
        }
        return counts.toList()
    }


    private fun displayGameHistory() {
        val username = SharedPrefUtil.getUserName(this)
        textUser.text = "Historique de : $username"
        // Logic to calculate total games won and lost
        val filteredPlayers = allPlayers.filter { it.name == username }
        val totalGamesWon = filteredPlayers.count { it.winner.toString().toBoolean() }
        val totalGamesLost = filteredPlayers.size - totalGamesWon
        val totalGamesPlayed = totalGamesWon + totalGamesLost
        val winPercentage = (totalGamesWon.toFloat() / totalGamesPlayed) * 100
        val lossPercentage = (totalGamesLost.toFloat() / totalGamesPlayed) * 100

        gameStat = listOf(winPercentage, lossPercentage)

        // Populate gameTypesStat and normalize
        val gameTypeCounts = countGamesByMode()
        val totalGameTypeCount = gameTypeCounts.sum()
        gameTypesStat = gameTypeCounts.map { (it.toFloat() / totalGameTypeCount) * 100 }

        // Animate the charts with the normalized data
        donutChartView.animate(gameStat)
        donutChartTypeView.animate(gameTypesStat)
        var totalTimeInSeconds = 0
        var totalDifferencesFound = 0
        val averageTimeInSeconds = if (recordList.isNotEmpty()) totalTimeInSeconds / recordList.size else 0
        averageTime = averageTimeInSeconds // Assuming this is in seconds
        averageDiff = if (recordList.isNotEmpty()) totalDifferencesFound / recordList.size else 0
        updateGameTypeText()
        showStat()
    }

    private fun showStat() {
         val totalGamesWon = gameStat[0].toInt()
        val totalGamesLoose = gameStat[1].toInt()

        val TotalGame = totalGamesWon+totalGamesLoose
        val displayText = "\nTotal des jeux gagnés : $totalGamesWon%\nTotal des jeux perdus : $totalGamesLoose%"
        textTotalView.text = displayText
        val displayTestTime = "Temps moyen pour chaque partie est: $averageTime s\n"
        textTemp.text=displayTestTime
    }
    // Include other necessary functions and companion object if needed
}