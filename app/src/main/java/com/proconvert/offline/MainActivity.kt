package com.proconvert.offline

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvDocCount: TextView
    private lateinit var listViewDocs: ListView
    private var docList: List<DocumentItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDocCount = findViewById(R.id.tvDocCount)
        listViewDocs = findViewById(R.id.listViewDocs)
        val btnCreateSamplePdf: Button = findViewById(R.id.btnCreateSamplePdf)

        btnCreateSamplePdf.setOnClickListener {
            createAndAutoOpenPdf()
        }

        listViewDocs.setOnItemClickListener { _, _, position, _ ->
            openInViewerEditor(docList[position].uri)
        }

        checkStoragePermissionAndLoad()
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT) {
            intent.data?.let { fileUri ->
                openInViewerEditor(fileUri)
            }
        }
    }

    private fun checkStoragePermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadAllDocuments()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } else {
            loadAllDocuments()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadAllDocuments()
        }
    }

    private fun loadAllDocuments() {
        lifecycleScope.launch(Dispatchers.IO) {
            val docs = DocumentScannerHelper.fetchAllDocuments(this@MainActivity)
            withContext(Dispatchers.Main) {
                docList = docs
                tvDocCount.text = "Found ${docs.size} documents on device"
                val names = docs.map { "${it.name} (${it.size / 1024} KB)" }
                listViewDocs.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, names)
            }
        }
    }

    private fun createAndAutoOpenPdf() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val paint = Paint().apply { textSize = 18f }
            page.canvas.drawText("Created by ProConvert Offline", 50f, 80f, paint)
            pdfDocument.finishPage(page)

            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "ProConvert_${System.currentTimeMillis()}.pdf")

            FileOutputStream(outFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                MediaScannerConnection.scanFile(this@MainActivity, arrayOf(outFile.absolutePath), null, null)
                val fileUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${applicationContext.packageName}.provider",
                    outFile
                )
                openInViewerEditor(fileUri)
            }
        }
    }

    private fun openInViewerEditor(uri: Uri) {
        val viewerIntent = Intent(this, DocumentEditorActivity::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivity(viewerIntent)
    }
}
