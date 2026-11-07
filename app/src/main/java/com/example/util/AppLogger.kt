package com.example.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object AppLogger {
    private const val MAX_LOGS = 150
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun d(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val formatted = "$timestamp [DEBUG/$tag] $message"
        addLog(formatted)
        Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val formatted = "$timestamp [INFO/$tag] $message"
        addLog(formatted)
        Log.i(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val formatted = "$timestamp [WARN/$tag] $message ${throwable?.message.orEmpty()}"
        addLog(formatted)
        Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val formatted = "$timestamp [ERROR/$tag] $message ${throwable?.message.orEmpty()}"
        addLog(formatted)
        Log.e(tag, message, throwable)
    }

    private fun addLog(entry: String) {
        logQueue.add(entry)
        while (logQueue.size > MAX_LOGS) {
            logQueue.poll()
        }
    }

    fun getFormattedLogs(): String {
        return logQueue.joinToString("\n").ifEmpty { "No diagnostics logs recorded yet." }
    }

    fun clear() {
        logQueue.clear()
    }
}
