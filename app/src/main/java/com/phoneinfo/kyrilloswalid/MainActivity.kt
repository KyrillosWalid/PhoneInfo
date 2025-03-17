package com.phoneinfo.kyrilloswalid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import fi.iki.elonen.NanoHTTPD

import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.TextView
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var batteryTextView: TextView
    private lateinit var server: SimpleWebServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        batteryTextView = findViewById(R.id.batteryTextView)

        // إنشاء مستقبل لمراقبة نسبة البطارية
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

                if (level != -1 && temperature != -1) {
                    val tempCelsius = temperature / 10.0  // تحويل إلى درجة مئوية
                    updateBatteryInfo(level, tempCelsius)
                }
            }
        }

        // تسجيل المستقبل لمراقبة تغييرات مستوى البطارية
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        server = SimpleWebServer(this, 8080)
        server.start()
    }

    private fun updateBatteryInfo(level: Int, temperature: Double) {
        // تحديث واجهة المستخدم
        batteryTextView.text = "نسبة البطارية: $level%\nدرجة حرارة البطارية: $temperature°C"

        // كتابة البيانات إلى ملف HTML
        writeBatteryToHtml(level, temperature)
    }
    private fun writeBatteryToHtml(level: Int, temperature: Double) {
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="ar">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>معلومات البطارية</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 20px; background-color: #f4f4f4; }
                    h1 { color: #333; }
                </style>
            </head>
            <body>
                <h1>نسبة البطارية: $level%</h1>
                <h2>درجة حرارة البطارية: $temperature°C</h2>
            </body>
            </html>
        """.trimIndent()

        try {
            val filePath = File(getExternalFilesDir(null), "battery_status.html")
            val writer = FileWriter(filePath)
            writer.write(htmlContent)
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        }
    }
class SimpleWebServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/"

        return when (uri) {
            "/" -> serveText(context.filesDir)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - File Not Found")
        }
    }

    private fun serveText(text: String): Response {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", text)
    }

    private fun serveFile(fileName: String): Response {
        val file = File(context.filesDir, fileName)

        return if (file.exists()) {
            newFixedLengthResponse(Response.Status.OK, "text/html", file.readText())
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }
    }
}