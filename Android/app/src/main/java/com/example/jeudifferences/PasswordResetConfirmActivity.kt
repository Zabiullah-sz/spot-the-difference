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

class PasswordResetConfirmActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset_confirm)
        setTheme()
        val backToMain = findViewById<Button>(R.id.backToMain12)
        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, PasswordResetRequestActivity::class.java)
            startActivity(mainPageActivity)
        }

        val submitPasswordButton = findViewById<Button>(R.id.submitPasswordButton)
        submitPasswordButton.setOnClickListener {
            val tokenEditText = findViewById<EditText>(R.id.token)
            val token = tokenEditText.text.toString()
           val newPasswordEditText = findViewById<EditText>(R.id.confirm_password)
            val newPassword = newPasswordEditText.text.toString()

            val resetPasswordRequest = ResetPasswordRequest( token, newPassword)

            val call = ApiClient.userApi.resetPassword(resetPasswordRequest)
            call.enqueue(object : Callback<ResetPasswordResponse> {
                override fun onResponse(
                    call: Call<ResetPasswordResponse>,
                    response: Response<ResetPasswordResponse?>
                ) {
                    //Log.i("la reponse est", "$call")

                    if (response.isSuccessful) {
                        //Log.i(
                            //"Réponse réussie",
                          //  "Code: ${response.code()}, Message: ${response.body()?.msg}"
                      //  )

                        Toast.makeText(
                            this@PasswordResetConfirmActivity,
                            "Password reset  successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(
                            this@PasswordResetConfirmActivity,
                            MainActivity::class.java
                        )
                        startActivity(intent)
                    } else {
                        Log.e("Réponse échouée", "Code: ${response.code()}, Message: ${response.message()}")

                        // Password reset failed, display error message
                        Toast.makeText(
                            this@PasswordResetConfirmActivity,
                            "Password reset failed. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    finish()
                }

                override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                    Log.e("Échec de la requête", "Message: ${t.message}")

                    // Password reset request failed, display error message
                    Toast.makeText(
                        this@PasswordResetConfirmActivity,
                        "Password reset request failed. Please check your internet connection and try again.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            })
        }
    }
}
