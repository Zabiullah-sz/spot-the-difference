package com.example.jeudifferences

import ThemePreferences


class ColorList
{
    val defaultColor: ColorObject = ColorObject("Théme 1", R.drawable.background1)

    fun colorPosition(imageObject: ColorObject): Int {
        for (i in basicColors().indices) {
            if (imageObject == basicColors()[i])
                return i
        }
        return 0
    }
    fun basicColors(): List<ColorObject> {
        return listOf(
            ColorObject("Théme 1", R.drawable.background1),
            ColorObject("Théme 2",R.drawable.purpule_bckground),
            ColorObject("Théme 3", R.drawable.pink_background)
        )
    }
}
