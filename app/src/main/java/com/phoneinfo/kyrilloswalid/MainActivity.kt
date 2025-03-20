package com.phoneinfo.kyrilloswalid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.TextView
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var batteryTextView: TextView
    private lateinit var ipaddresst: TextView
    private lateinit var server: SimpleWebServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        batteryTextView = findViewById(R.id.batteryTextView)
        ipaddresst = findViewById(R.id.ipaddress)

        val port = 8080
        // إنشاء مستقبل لمراقبة نسبة البطارية
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

                if (level != -1 && temperature != -1) {
                    val tempCelsius = temperature / 10.0  // تحويل إلى درجة مئوية
                    updateBatteryInfo(level, tempCelsius, port)
                }
            }
        }

        // تسجيل المستقبل لمراقبة تغييرات مستوى البطارية
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        server = SimpleWebServer(this, port)
        server.start()
    }

    private fun updateBatteryInfo(level: Int, temperature: Double, port: Int) {
        // تحديث واجهة المستخدم
        batteryTextView.text = "نسبة البطارية: $level%\nدرجة حرارة البطارية: $temperature°C"

        // كتابة البيانات إلى ملف HTML
        writeBatteryToHtml(level, temperature)
        var ip = getLocalIpAddress(this) + ":" + port
        ipaddresst.setText(ip)

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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PhoneInfoKWR", "التطبيق يتم إغلاقه!")
        server.stop()
    }
    fun getLocalIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress

        if (ipAddress == 0) return null  // لم يتم العثور على عنوان IP

        return try {
            val byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipAddress)
            InetAddress.getByAddress(byteBuffer.array()).hostAddress
        } catch (e: UnknownHostException) {
            null
        }
    }

    }
class SimpleWebServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/"

        return when (uri) {
            "/" -> serveFile(File(context.getExternalFilesDir(null), "battery_status.html"))
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - File Not Found")
        }
    }

    private fun serveText(text: String): Response {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", text)
    }

    private fun serveFile(fileName: File): Response {
        val file = fileName

        return if (file.exists()) {
            newFixedLengthResponse(Response.Status.OK, "text/html", file.readText())
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }
    }
}