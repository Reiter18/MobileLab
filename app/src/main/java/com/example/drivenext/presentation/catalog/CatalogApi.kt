package com.example.drivenext.presentation.catalog

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

internal data class Car(
    val id: String, val model: String, val brand: String,
    val price: Int, val transmission: String, val fuel: String
)

internal class CatalogApi {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var latest = 0

    fun load(query: String, done: (Result<List<Car>>) -> Unit) {
        val request = ++latest
        worker.submit {
            val result = runCatching {
                val term = URLEncoder.encode(query, "UTF-8")
                try {
                    read("10.0.2.2", term)
                } catch (_: IOException) {
                    read("127.0.0.1", term) // adb reverse на телефоне
                }
            }
            main.post { if (request == latest) done(result) }
        }
    }

    private fun read(host: String, term: String): List<Car> {
        val connection = URL("http://$host:8765/cars?q=$term").openConnection() as HttpURLConnection
        connection.connectTimeout = 1500
        connection.readTimeout = 3000
        try {
            val json = JSONArray(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            return List(json.length()) { index ->
                val car = json.getJSONObject(index)
                Car(car.getString("id"), car.getString("model"), car.getString("brand"),
                    car.getInt("price"), car.getString("transmission"), car.getString("fuel"))
            }
        } finally {
            connection.disconnect()
        }
    }

    fun cancel() { latest++ }

    fun close() {
        cancel()
        worker.shutdownNow()
    }
}
