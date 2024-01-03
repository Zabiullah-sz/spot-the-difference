package com.example.jeudifferences

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.IOException
import java.io.File
import android.widget.EditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import android.content.SharedPreferences
import android.provider.Settings
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import kotlinx.serialization.StringFormat
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody


//cette classe est inspiré d'un article de code en ligne
class CreateAccountActivity : BaseActivity()  {
    lateinit var cameraButton: Button
    lateinit var avatar:ImageView
    lateinit var createAccountButton:Button
    lateinit var usernameEditText: EditText
    lateinit var passwordEditText: EditText
    lateinit var emailEditText: EditText
    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         setTheme()
        setContentView(R.layout.activity_create_account)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        emailEditText = findViewById(R.id.userEmailEditText)

        passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        //check if allowed to use camera (permission granted)
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_DENIED
        ) {
            val permission = arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requestPermissions(permission, 110)
        }

        avatar = findViewById(R.id.avatarCreation)
        cameraButton = findViewById(R.id.cameraButton)
        createAccountButton = findViewById(R.id.createAccountButton)

        cameraButton.setOnClickListener {
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

        createAccountButton.setOnClickListener {
            signUpAndSendData()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

            avatar.setImageURI(imageUri)
        }
    }

    private fun signUpAndSendData() {
        val email = emailEditText.text.toString()
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            Log.e("SignUpError", "Username or password cannot be blank.")
            return
        }
        val signupRequest = SignupRequest(password, username, email)
        val call = ApiClient.userApi.signUp(signupRequest)
        call.enqueue(object : Callback<SignupResponse> {
            override fun onResponse(
                call: Call<SignupResponse>,
                response: Response<SignupResponse>
            ) {
                if (response.isSuccessful) {
                    Log.d("SignUpSuccess", "Successfully signed up: ${response.body()?.msg}")
                    Toast.makeText(
                        this@CreateAccountActivity,
                        "Compte créer avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                    loginToServer(username, password)
                    val intent =
                        Intent(this@CreateAccountActivity, MainActivity::class.java)
                    startActivity(intent)

                } else {
                    Toast.makeText(
                        this@CreateAccountActivity,
                        "Nom d'utilisateur déjà pris",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("SignUpError", "Error signing up: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Log.e("SignUpFailure", "Network error: ${t.localizedMessage}")
            }
        })
    }

    private fun saveUserDataToPrefs(user: User) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("userName", user?.username)
            putString("userId", user?.userId)
            apply()
        }
    }

    private fun loginToServer(loginUsername: String, loginPassword: String) {

        // Login user using centralized ApiClient
        val call = ApiClient.userApi.login(LoginRequest(loginUsername, loginPassword))
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                if (response.isSuccessful) {
                    // Successfully logged in
                    val user = response.body()?.user
                    user?.let { saveUserDataToPrefs(it) }

                    Log.d("LoginSuccess", "Logged in user: ${user?.username}")
                    sendDataToServer(user?.userId)
                } else {
                    // Handle login error
                    Log.e("LoginError", "Error logging in: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Handle network errors or other issues
                Log.e("LoginFailure", "Network error: ${t.localizedMessage}")
            }
        })
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
