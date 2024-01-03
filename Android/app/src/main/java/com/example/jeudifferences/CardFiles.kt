package com.example.jeudifferences

import android.graphics.Bitmap

class CardFiles {
    var name: String? = null
    var originalImage: Bitmap? = null
    var modifiedImage: Bitmap? = null
    var nbDifferences:  String? = null

    constructor(name: String, originalImage: Bitmap, modifiedImage: Bitmap, nbDifferences: String) {
        this.originalImage = originalImage
        this.modifiedImage = modifiedImage
        this.nbDifferences = nbDifferences
        this.name = name

    }
}
