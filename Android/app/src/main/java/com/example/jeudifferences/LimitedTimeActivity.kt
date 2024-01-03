package com.example.jeudifferences

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import io.socket.emitter.Emitter
import org.json.JSONArray
import SocketClientHandler
import android.graphics.Bitmap
import android.widget.FrameLayout

class LimitedTimeActivity : BaseActivity() {
    var clientSocket = SocketClientHandler
    var original: ImageView? = null
    var modified: ImageView? = null
    lateinit var originalFrame: FrameLayout
    lateinit var modifiedFrame: FrameLayout

    private var gameDataList = mutableListOf<CardFiles>()
    val nextCardFiles = mutableListOf<CardFiles>()

    private var pendingCardUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setContentView(R.layout.activity_classic_coop)
        var intentLimitedTime= intent
        val modified = LimitedTimeActivity.data.modifiedImage
        val original = LimitedTimeActivity.data.originalImage


        // Get reference to the ImageView from the layout
        val originalImageView: ImageView = findViewById(R.id.originalImage)
        val modifiedImageView: ImageView = findViewById(R.id.modifiedImage)
        originalFrame = findViewById(R.id.originalFrame)
        modifiedFrame = findViewById(R.id.modifiedFrame)
        val originalBitmap: Bitmap = BitmapFactory.decodeByteArray(original, 0, original.size)
        val modifiedBitmap: Bitmap = BitmapFactory.decodeByteArray(modified, 0, modified.size)

        // Set the image to the ImageView
        originalImageView.setImageBitmap(originalBitmap)
        modifiedImageView.setImageBitmap(modifiedBitmap)
        clientSocket.connect()

       // original = findViewById(R.id.originalImage)
       // modified = findViewById(R.id.modifiedImage)

        clientSocket.on("next_card", onNextCard)
    }

    private val onNextCard = Emitter.Listener() { args ->
        if (args[0] != null) {
            val data = args[0] as JSONArray

            // Vérifiez que les variables data et element ne sont pas null.
            if (data != null ) {
                runOnUiThread {
                    val element = data.getJSONObject(0)

                    // Décodez les images.
                    val originalImage = element.get("originalImage").toString()
                    val decodedString: ByteArray = Base64.decode(originalImage, Base64.DEFAULT)
                    val bitmapImageOriginal = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    val modifiedImage = element.get("modifiedImage").toString()
                    val decodedString1: ByteArray = Base64.decode(modifiedImage, Base64.DEFAULT)
                    val bitmapImageModified = BitmapFactory.decodeByteArray(decodedString1, 0, decodedString1.size)

                    // Créez un nouvel objet CardFiles.
                    val newCard = CardFiles(
                        name = element.get("name").toString(),
                        originalImage = bitmapImageOriginal,
                        modifiedImage = bitmapImageModified,
                        nbDifferences = element.get("differenceNbr").toString()
                    )

                    // Ajoutez la nouvelle carte à la liste des cartes à traiter.
                    nextCardFiles.add(newCard)

                    // Si une mise à jour de carte est en attente, mettez à jour les cartes.
                    if (pendingCardUpdate) {
                        updateCards()
                    }
                }
            }
        }
    }

    private fun updateCards() {
        // Vérifiez si la liste des cartes à traiter est vide.
        if (nextCardFiles.isNotEmpty()) {
            // Récupérez la première carte de la liste.
            val nextCard = nextCardFiles.removeFirst()

            // Mettez à jour les images des cartes.
            original?.setImageBitmap(nextCard.originalImage)
            modified?.setImageBitmap(nextCard.modifiedImage)
        } else {
            // Si la liste des cartes à traiter est vide, désactivez la mise à jour de carte.
            pendingCardUpdate = false
        }
    }
    companion object {
        lateinit var data: gameData4
       // var soundSucess: Int = 1
        //var soundError: Int = 3
    }
}
