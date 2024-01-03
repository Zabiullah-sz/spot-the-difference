package com.example.jeudifferences

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.content.Intent
import android.app.Activity
import SocketClientHandler
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import  android.widget.EditText
import android.widget.Toast
import com.bumptech.glide.Glide
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Call
import java.io.File


class AccountSettingActivity: BaseActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var avatarImageView: ImageView
    val clientSocket = SocketClientHandler
    private var imageUri: Uri? = null
    private val CAMERA_REQUEST = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_setting)
        setTheme()
        avatarImageView = findViewById(R.id.avatarImageView)
        val currentUsername = findViewById<TextView>(R.id.currentUsername)
        val username = SharedPrefUtil.getUserName(this)
        val userId = SharedPrefUtil.getUserId(this)
        currentUsername.text = "Nom d'utilisateur actuel : $username"

        val pseudonymEditText = findViewById<EditText>(R.id.pseudonymEditText)
        val saveChangesButton = findViewById<Button>(R.id.saveChangesButton)
        val backToMain = findViewById<Button>(R.id.backToMain7)
        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            startActivity(mainPageActivity)
        }
            saveChangesButton.setOnClickListener {
                val newPseudonym = pseudonymEditText.text.toString()
                if (newPseudonym.isNotEmpty()) {
                    updateUsername(newPseudonym)
                } else {
                    Toast.makeText(this, "Veuillez entrer un nouveau pseudonyme", Toast.LENGTH_SHORT).show()
                }

        }

        val changeAvatarButton: Button = findViewById<Button>(R.id.changeAvatarButton)
        changeAvatarButton.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                takePicture()
            } else {
                // Request permissions
                val permission = arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestPermissions(permission, CAMERA_REQUEST)
            }
        }

    }
    private fun updateUsername(newPseudonym: String) {
        val userId= SharedPrefUtil.getUserId(this)
        val request = ChangUserNameRequest(userId.toString(), newPseudonym)
        Log.i("users request", "$request")
        val call = ApiClient.userApi.changeUsername(request)
        Log.i("user request", "$request")

        call.enqueue(object : Callback<ChangUserNameResponse> {
            override fun onResponse(
                call: Call<ChangUserNameResponse>,
                response: Response<ChangUserNameResponse>
            ) {
                handleUsernameUpdateResponse(response, newPseudonym)
            }

            override fun onFailure(call: Call<ChangUserNameResponse>, t: Throwable) {
                handleNetworkError()
            }
        })
    }

    private fun handleUsernameUpdateResponse(
        response: Response<ChangUserNameResponse>,
        newPseudonym: String
    ) {
        Log.i("UpdateUsername", "${response.body()}")
        if (response.isSuccessful) {
            val changeUserNameResponse = response.body()
            if (changeUserNameResponse?.status == "success") {
                SharedPrefUtil.setUserName(this@AccountSettingActivity, newPseudonym)
                val userId = SharedPrefUtil.getUserId(this@AccountSettingActivity)
               // currentUsername.text = "Nom d'utilisateur actuel : $newPseudonym"
                Log.d("UpdateUsername", "User ID: $userId, New Pseudonym: $newPseudonym")
                Toast.makeText(
                    this@AccountSettingActivity,
                    "Mis à jour avec succès",
                    Toast.LENGTH_SHORT
                ).show()
                sendDataToServer(userId)
            } else {
                handleUpdateFailure(changeUserNameResponse)
            }
        } else {
            Log.e("UpdateUsername", "Network request failed with code ${response.code()}")
            handleUpdateFailure(null)
        }
    }

    private fun handleUpdateFailure(changeUserNameResponse: ChangUserNameResponse?) {
        Log.e("UpdateUsername", "Update username failed. Status: ${changeUserNameResponse?.status}")
        Toast.makeText(
            this@AccountSettingActivity,
            "Échec de la mise à jour",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleNetworkError() {
        Toast.makeText(
            this@AccountSettingActivity,
            "Erreur de réseau",
            Toast.LENGTH_SHORT
        ).show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted, open the camera
                    takePicture()
                } else {
                    showPermissionDeniedDialog()
                    // Camera permission denied, show a message or handle accordingly
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission refusée")
            .setMessage("Pour prendre des photos, vous devez accorder l'autorisation d'accès à l'appareil photo. Veuillez l'activer dans les paramètres de l'application.")
            .setPositiveButton("Aller aux paramètres") { _, _ ->
                // Ouvrir les paramètres de l'application pour permettre à l'utilisateur d'activer les autorisations nécessaires
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Annuler") { _, _ ->
                Toast.makeText(this, "Autorisation d'appareil photo refusée", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    private fun takePicture() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, CAMERA_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST) {

            avatarImageView.setImageURI(imageUri)
        }
    }
    private fun sendDataToServer(userId: String?) {
        Log.i("her","here")
        if (imageUri != null) {
            Log.i("her","here2")
            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            //val userId = sharedPref.getString("userId", null)
            Log.i("her","here12")
            if (userId != null) {
                Log.i("her","here23")
                val userIdRequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
                val file = File(getRealPathFromURI(imageUri!!))
                val fileReqBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("profileImage", file.name, fileReqBody)

                val call = ApiClient.userApi.uploadProfileImage(userIdRequestBody, part)
                call.enqueue(object : Callback<UploadResponse> {
                    override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                        if (response.isSuccessful) {
                            Log.d("UploadSuccess", "Successfully uploaded image")
                        } else {
                            Log.e("UploadError", "Error uploading image: ${response.errorBody()?.string()}")
                        }
                    }
                    override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                        Log.e("UploadFailure", "Network error: ${t.localizedMessage}")
                    }
                })
            } else {
                Log.e("UserIdError", "User ID is null.")
            }
        } else {
            Log.e("UploadError", "Image Uri is null.")
        }
    }
    private fun getRealPathFromURI(contentURI: Uri): String {
        val result: String
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) {
            result = contentURI.path.toString()
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }


}





