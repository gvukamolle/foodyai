// Создать файл: app/src/main/java/com/example/calorietracker/utils/CacheManager.kt

package com.example.calorietracker.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object CacheManager {

    suspend fun getCacheSize(context: Context): Long = withContext(Dispatchers.IO) {
        var size = 0L
        size += getDirSize(context.cacheDir)
        context.externalCacheDir?.let { size += getDirSize(it) }
        return@withContext size
    }

    suspend fun clearCache(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            deleteDir(context.cacheDir)
            context.cacheDir.mkdirs()

            context.externalCacheDir?.let {
                deleteDir(it)
                it.mkdirs()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(
            size / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    suspend fun getCacheDetails(context: Context): CacheInfo = withContext(Dispatchers.IO) {
        val totalSize = getCacheSize(context)
        CacheInfo(
            totalSize = totalSize,
            internalSize = getDirSize(context.cacheDir),
            externalSize = context.externalCacheDir?.let { getDirSize(it) } ?: 0L,
            imagesCacheSize = 0L,
            exportFilesSize = 0L,
            breakdown = emptyMap()
        )
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        getDirSize(file)
                    } else {
                        file.length()
                    }
                }
            }
        }
        return size
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDir(File(dir, child))
                    if (!success) return false
                }
            }
        }
        return dir.delete()
    }

    data class CacheInfo(
        val totalSize: Long,
        val internalSize: Long,
        val externalSize: Long,
        val imagesCacheSize: Long,
        val exportFilesSize: Long,
        val breakdown: Map<String, String>
    )
}