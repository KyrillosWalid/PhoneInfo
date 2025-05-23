package com.phoneinfo.kyrilloswalid

import android.annotation.SuppressLint
import android.content.*
import android.net.wifi.WifiManager
import android.os.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var batteryTextView: TextView
    private lateinit var ipaddresst: TextView
    private lateinit var server: SimpleWebServer
    private lateinit var htmlFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        batteryTextView = findViewById(R.id.batteryTextView)
        ipaddresst = findViewById(R.id.ipaddress)

        // حفظ ملف HTML مؤقت في مجلد الكاش (يمكن تعديله لاحقاً للـ Download)
        htmlFile = File(cacheDir, "battery_status.html")

        // بدء خادم الويب المحلي
        server = SimpleWebServer(this, 8080, htmlFile)
        server.start()

        // استقبال تحديثات حالة البطارية
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
                if (level != -1 && temperature != -1) {
                    val tempCelsius = temperature / 10.0
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                    batteryTextView.text = "نسبة البطارية: $level%\nدرجة الحرارة: $tempCelsius°C"
                    ipaddresst.text = "${getLocalIpAddress()}:8080"

                    updateHtmlFile(time, level, tempCelsius)
                }
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun updateHtmlFile(time: String, level: Int, temperature: Double) {
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="ar">
            <head>
                <meta charset="UTF-8" />
                <title>معلومات البطارية</title>
                <style>
                    body { font-family: sans-serif; background-color: #f0f0f0; padding: 20px; text-align: center; }
                    table { margin: auto; border-collapse: collapse; width: 80%; }
                    th, td { border: 1px solid #444; padding: 8px; }
                    th { background-color: #ddd; }
                </style>
            </head>
            <body>
                <h1>سجل معلومات البطارية</h1>
                <table id="batteryTable">
                    <thead>
                        <tr><th>الوقت</th><th>نسبة البطارية</th><th>درجة الحرارة</th></tr>
                    </thead>
                    <tbody id="batteryBody"></tbody>
                </table>

                <script>
                    // استرجاع البيانات من localStorage أو بدء مصفوفة جديدة
                    const data = JSON.parse(localStorage.getItem("batteryData") || "[]");

                    // إضافة السطر الجديد
                    data.push({ time: "$time", level: "$level%", temp: "$temperature°C" });

                    // حفظ البيانات المحدثة
                    localStorage.setItem("batteryData", JSON.stringify(data));

                    // عرض جميع الصفوف في الجدول
                    const body = document.getElementById("batteryBody");
                    body.innerHTML = ""; // تفريغ الجدول قبل الإضافة لمنع التكرار
                    data.forEach(row => {
                        const tr = document.createElement("tr");
                        tr.innerHTML = "<td>" + row.time + "</td><td>" + row.level + "</td><td>" + row.temp + "</td>";
                        body.appendChild(tr);
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        try {
            FileWriter(htmlFile).use { it.write(htmlContent) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getLocalIpAddress(): String? {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return if (ip != 0) {
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip)
            try {
                InetAddress.getByAddress(buffer.array()).hostAddress
            } catch (e: UnknownHostException) {
                null
            }
        } else null
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }
}

class SimpleWebServer(
    private val context: Context,
    port: Int,
    private val htmlFile: File
) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession?): Response {
        return if (htmlFile.exists()) {
            newFixedLengthResponse(Response.Status.OK, "text/html", htmlFile.readText())
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - File Not Found")
        }
    }
}
