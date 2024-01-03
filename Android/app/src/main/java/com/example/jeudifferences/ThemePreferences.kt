import android.content.Context
import android.content.SharedPreferences
import com.example.jeudifferences.R

class ThemePreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "ThemePrefs"
        const val KEY_THEME = "selectedTheme"
    }
    val DEFAULT_THEME = R.style.Theme_JeuDifferences
    fun getSelectedTheme(): Int {
        return sharedPreferences.getInt(KEY_THEME, DEFAULT_THEME)
    }

    fun setSelectedTheme(theme: Int) {
        sharedPreferences.edit().putInt(KEY_THEME, theme).apply()
    }

}
