package com.example.jeudifferences
import SocketClientHandler
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerLogOutActivity:BaseActivity() {
    var socketClient = SocketClientHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_logout)
        val call = ApiClient.userApi.logout()
        logout()
    }

    private fun logout() {
        val call: Call<LogoutResponse> = ApiClient.userApi.logout()

        call.enqueue(object : Callback<LogoutResponse> {
            override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                if (response.isSuccessful) {
                    val msg = response.body()?.msg
                  //  Toast.makeText(this@PlayerLogOutActivity,"Déconnexion avec succés",Toast.LENGTH_SHORT).show()
                    Log.i("deconnexion", "deconnexion avec succes")
                    socketClient.disconnect()
                    val intentMain = Intent(this@PlayerLogOutActivity,MainActivity::class.java)
                    startActivity(intentMain)
                } else {
                   // Toast.makeText(this@PlayerLogOutActivity,"Déconnexion a echouée",Toast.LENGTH_SHORT).show()
                    Log.i("deconnexion", "deconnexion échec")
                }
            }

            override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                // Gérer les erreurs de connexion
            }
        })
    }
}
