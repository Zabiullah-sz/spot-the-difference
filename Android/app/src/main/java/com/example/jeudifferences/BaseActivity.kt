package com.example.jeudifferences

import ThemePreferences
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import org.junit.runners.model.InitializationError
import kotlin.properties.Delegates

open class BaseActivity : AppCompatActivity() {
    lateinit var selectedColor: ColorObject

    private val defaultTheme = R.drawable.background1
    private lateinit var themeManager: ThemeManager
    private var originalTheme by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
      //  resetBackgroundToOriginalTheme()
        loadColorSpinner()

    }
     fun resetBackgroundToOriginalTheme() {
        val themePreferences = ThemePreferences(this)
        themePreferences.setSelectedTheme(defaultTheme)
        window.decorView.setBackgroundResource(defaultTheme)
    }
 fun setTheme(){
//    val currentTheme = ThemePreferences(this).getSelectedTheme()
    window.decorView.setBackgroundResource(defaultTheme)
    //loadColorSpinner()

}
     fun loadColorSpinner() {
        val themePreferences = ThemePreferences(this)
        // val currentTheme = themePreferences.getSelectedTheme()
        val colorSpinnerView = findViewById<Spinner>(R.id.colorSpinner)
        selectedColor = ColorList().defaultColor
        colorSpinnerView.adapter =
            ColorSpinnerAdapter(applicationContext, ColorList().basicColors())
        colorSpinnerView.setSelection(ColorList().colorPosition(selectedColor), false)

        colorSpinnerView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                var currentTheme = ColorList().basicColors()[position].resourceId

                themePreferences.setSelectedTheme(currentTheme)
                Log.i("currentTeme ", "$currentTheme")
                window.decorView.setBackgroundResource(currentTheme)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                ColorList().basicColors()[0].resourceId

            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        resetBackgroundToOriginalTheme()
    }
}

