package com.example.jeudifferences

import SocketClientHandler
import ThemePreferences
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import io.socket.emitter.Emitter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.text.SpannedString
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ToggleButton
import org.json.JSONObject


open class MainActivity : BaseActivity() {
    lateinit var edit: EditText
    //lateinit var selectedColor: ColorObject
    private var isUsernameValid = false
    var game = ""
    val clientSocket = SocketClientHandler
    lateinit var loginButton: Button
    lateinit var loginUsernameEditText: EditText
    lateinit var loginPasswordEditText: EditText
    private lateinit var themeManager: ThemeManager
    private lateinit var progressBar: ProgressBar



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      //  setContentView(R.layout.activity_main)


        loginButton = findViewById(R.id.loginButton)
        loginUsernameEditText = findViewById(R.id.loginUsernameEditText)
        loginPasswordEditText = findViewById(R.id.loginPasswordEditText)
        var showPasswordToggle: ImageButton = findViewById(R.id.showPasswordToggle)
        clientSocket.connect()
        clientSocket.on("validation-response", onValidation)


        loginPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        showPasswordToggle.setOnClickListener {
            val isPasswordVisible = loginPasswordEditText.transformationMethod == null

            if (isPasswordVisible) {
                loginPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                loginPasswordEditText.transformationMethod = null
            }
            loginPasswordEditText.setSelection(loginPasswordEditText.text.length)
        }
        loginButton.setOnClickListener {
            loginToServer()
        }

        // Connect to the server
        //clientSocket.connect()

        edit = findViewById(R.id.loginUsernameEditText)
        val createView = findViewById<Button>(R.id.navigateToCreateButton).setOnClickListener {
            val goToCreatePageintent = Intent(this, CreateAccountActivity::class.java)
            startActivity(goToCreatePageintent)
        }
        val username = SharedPrefUtil.getUserName(this)
        val userId = SharedPrefUtil.getUserId(this)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordLink)
        forgotPasswordLink.setOnClickListener {
            val intentPasswordRequest = Intent(this, PasswordResetRequestActivity::class.java)
            startActivity(intentPasswordRequest)
        }
    }
//////// hehe/////////////////

    private val onValidation = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0]
                if(data == true) {
                    val intent = Intent(this@MainActivity, MainPageActivity::class.java)
                    startActivity(intent)
                }
                if(data == false) {
                    val errorMessage = "Compte déjà ouvert sur un autre appareil"
                    runOnUiThread {
                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveUserDataToPrefs(user: User) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("userName", user?.username)
            putString("userId", user?.userId)
            apply()
        }
    }

    private fun getUserFromPrefs(): String? {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPref.getString("userName", null)
    }

    private fun clearUserFromPrefs() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("username")
            apply()
        }
    }


    /*private val onValidation = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val userNameVerification = args[0] as Boolean
                if(userNameVerification) {
                    connectToChat()
                }
                else {
                    Toast.makeText(this, "This username is not available or doesn't meet the requirements", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }*/
    private fun loginToServer() {
        val loginUsername = loginUsernameEditText.text.toString()
        val loginPassword = loginPasswordEditText.text.toString()

        // Login user using centralized ApiClient
        val call = ApiClient.userApi.login(LoginRequest(loginUsername, loginPassword))
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    // Successfully logged in
                    val gson = Gson()
                    val user = response.body()?.user
                    val jsonString = gson.toJson(user)
                    clientSocket.send("validation", jsonString)

                    user?.let { saveUserDataToPrefs(it) }
                    ChatActivity.name = user?.username.toString()
                    if (user != null) {
                        ChatActivity.currentId=user.userId
                        PlayerAccountActivity.myId = user.userId
                        Log.i("logii","${user.userId}")
                    }
                    Log.d("LoginSuccess", "Logged in user: ${response.body()}")
                    //Log.d("LoginSuccess", "Logged in user: ${user?.userName}")

                    val sharedPref = getSharedPreferences("MyGamePrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("playerName", loginUsername)
                        apply()
                    }

                } else {
                    // Display error message
                    val errorMessage = "nom d'utilisateur ou mot de passe invalide"
                    runOnUiThread {
                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    Log.e("LoginError", "Error logging in: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Handle network errors or other issues
                Log.e("LoginFailure", "Network error: ${t.localizedMessage}")
            }
        })
    }


}
