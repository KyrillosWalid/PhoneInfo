package com.phoneinfo.kyrilloswalid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.widget.TextView
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var batteryTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        batteryTextView = findViewById(R.id.batteryTextView)

        // إنشاء مستقبل لمراقبة نسبة البطارية
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra("level", -1) ?: -1
                if (level != -1) {
                    updateBatteryLevel(level)
                }
            }
        }

        // تسجيل المستقبل لمراقبة تغييرات مستوى البطارية
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    }
    private fun updateBatteryLevel(level: Int) {
        // تحديث واجهة المستخدم
        batteryTextView.text = "نسبة البطارية: $level%"

        // كتابة البيانات إلى ملف HTML
        writeBatteryToHtml(level)
    }

    private fun writeBatteryToHtml(level: Int) {
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="ar">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>نسبة البطارية</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 20px; background-color: #f4f4f4; }
                    h1 { color: #333; }
                </style>
            </head>
            <body>
                <h1>نسبة البطارية الحالية: $level%</h1>
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