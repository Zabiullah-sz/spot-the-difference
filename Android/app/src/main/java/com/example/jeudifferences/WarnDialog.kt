package com.example.jeudifferences

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.widget.AppCompatButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog


class WarnDialog {
    private var cancelButton: AppCompatButton
    private var confirmButton: AppCompatButton
    private var text: TextView
    private var dialog: AlertDialog

    constructor(context: Context) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.choice_warning_dialog, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)
        dialog =  builder.create()
        cancelButton = view.findViewById(R.id.buttonWarnBack)
        confirmButton = view.findViewById(R.id.buttonWarnConfirm)
        text = view.findViewById(R.id.textViewToChangeWarn)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun show(newText: String, callback: (() -> Unit)? = null) {
        text.text = newText

        confirmButton.setOnClickListener {
            callback?.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }
}
