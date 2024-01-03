package com.example.jeudifferences
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class PasswordResetRequestActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.activity_password_reset)

        val backToMain = findViewById<Button>(R.id.backToMain11)
        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainActivity::class.java)
            startActivity(mainPageActivity)
        }
        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)
        resetPasswordButton.setOnClickListener {
            // Récupérer les données nécessaires (email, username, etc.)
            val emailEditText = findViewById<EditText>(R.id.emailEditText)
            val email = emailEditText.text.toString()
            val usernameEditText = findViewById<EditText>(R.id.username)
           // var username = SharedPrefUtil.getUserName(this)
           var username = usernameEditText.text.toString()
            //Log.i("user name is","$username")
            if (username == "") {
                Toast.makeText(
                    this@PasswordResetRequestActivity,
                    "le mot de passe est invalide",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val call = ApiClient.userApi.forgotPassword(email)
                call.enqueue(object : Callback<EmailResponse> {
                    override fun onResponse(
                        call: Call<EmailResponse>,
                        response: Response<EmailResponse?>
                    ) {
                        //Log.i("la reponse est", "$call")
                        if (response.isSuccessful) {
                            //Log.i(
                              //  "Réponse réussie",
                              //  "Code: ${response.code()}, Message: ${response.body()?.msg}"
                          //  )
                            val emailResponse = response.body()
                            if (emailResponse?.status == "success") {
                              //  Log.i("Password reset token:", "$token")
                                val passwordResetIntent = Intent(
                                    this@PasswordResetRequestActivity,
                                    PasswordResetConfirmActivity::class.java
                                )
                              //  passwordResetIntent.putExtra("token", token)
                                startActivity(passwordResetIntent)

                                // Password reset email sent successfully
                                Toast.makeText(
                                    this@PasswordResetRequestActivity,
                                    "Password reset email sent successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(
                                    this@PasswordResetRequestActivity,
                                    PasswordResetConfirmActivity::class.java
                                )
                                startActivity(intent)
                            } else {
                                //Log.i("Réponse échouée", "Code: ${response.code()}")
                                // Failed to send password reset email
                                Toast.makeText(
                                    this@PasswordResetRequestActivity,
                                    "Failed to send password reset email",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<EmailResponse>, t: Throwable?) {
                        val errorMessage = t?.message ?: "Unknown error"
                        Log.e("Error sending request",errorMessage)
                        Toast.makeText(
                            this@PasswordResetRequestActivity,
                            "Failed to send password reset email to me ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

        }
    }
}

