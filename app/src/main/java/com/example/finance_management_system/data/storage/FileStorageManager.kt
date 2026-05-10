package com.example.finance_management_system.data.storage

import android.content.Context
import android.os.Environment
import java.io.File

class FileStorageManager(private val context: Context) {
    fun getInternalReportDirectory(): File {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInternalBackupDirectory(): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExternalExportDirectory(): File? {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
        val dir = File(base, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
