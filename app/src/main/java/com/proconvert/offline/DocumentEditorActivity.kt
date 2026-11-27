package com.proconvert.offline

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DocumentEditorActivity : AppCompatActivity() {

    private var currentFileUri: Uri? = null
    private var isModified: Boolean = false
    private lateinit var etNotes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        currentFileUri = intent.data
        val tvFileUri: TextView = findViewById(R.id.tvFileUri)
        etNotes = findViewById(R.id.etNotes)

        tvFileUri.text = "Opened File: ${currentFileUri?.lastPathSegment ?: "Unknown"}"

        etNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isModified = true
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onPause() {
        super.onPause()
        if (isModified && currentFileUri != null) {
            val noteText = etNotes.text.toString()
            val fileName = currentFileUri?.lastPathSegment ?: "edited_doc"
            isModified = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val backupFile = File(getExternalFilesDir(null), "${fileName}_autosave.txt")
                    backupFile.writeText(noteText)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Toast.makeText(this, "Changes auto-saved on exit", Toast.LENGTH_SHORT).show()
        }
    }
}
