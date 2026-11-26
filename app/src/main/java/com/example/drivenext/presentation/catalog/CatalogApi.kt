package com.example.drivenext.presentation.catalog

import com.example.drivenext.data.auth.Supabase

internal data class Car(
    val id: String, val model: String, val brand: String,
    val price: Int, val transmission: String, val fuel: String
)

internal class CatalogApi(private val supabase: Supabase) {
    private var latest = 0

    fun load(query: String, done: (Result<List<Car>>) -> Unit) {
        val request = ++latest
        supabase.run({
            val json = supabase.cars(query)
            List(json.length()) { index ->
                val car = json.getJSONObject(index)
                Car(car.getString("id"), car.getString("model"), car.getString("brand"),
                    car.getInt("price"), car.getString("transmission"), car.getString("fuel"))
            }
        }) { if (request == latest) done(it) }
    }

    fun cancel() { latest++ }
}
