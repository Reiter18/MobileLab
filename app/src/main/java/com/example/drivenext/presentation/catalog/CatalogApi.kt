package com.example.drivenext.presentation.catalog

import com.example.drivenext.data.auth.Supabase
import org.json.JSONObject

internal data class Car(
    val id: String, val model: String, val brand: String,
    val price: Int, val transmission: String, val fuel: String,
    val address: String, val description: String, val insurance: Int, val deposit: Int
)

internal fun JSONObject.toCar() = Car(
    getString("id"), getString("model"), getString("brand"), getInt("price"),
    getString("transmission"), getString("fuel"), optString("address"), optString("description"),
    optInt("insurance", 300), optInt("deposit", 15000)
)

internal class CatalogApi(private val supabase: Supabase) {
    private var latest = 0

    fun load(query: String, favoriteIds: (() -> List<Long>)?, done: (Result<List<Car>>) -> Unit) {
        val request = ++latest
        supabase.run({
            val json = if (favoriteIds == null) supabase.cars(query) else supabase.favoriteCars(favoriteIds())
            List(json.length()) { json.getJSONObject(it).toCar() }
        }) { if (request == latest) done(it) }
    }

    fun cancel() { latest++ }
}
