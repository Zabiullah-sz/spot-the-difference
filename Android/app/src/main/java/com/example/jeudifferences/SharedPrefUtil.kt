package com.example.jeudifferences
import android.content.Context
class SharedPrefUtil {

    companion object {

        fun getUserName(context: Context): String? {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            return sharedPref.getString("userName", null)
        }
        fun getUserId(context: Context): String? {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            return sharedPref.getString("userId", null)
        }

        fun setUserName(context: Context, username: String?) {
            val sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPref.edit()
                .putString("userName", username)
                .apply()
        }

    }

}

