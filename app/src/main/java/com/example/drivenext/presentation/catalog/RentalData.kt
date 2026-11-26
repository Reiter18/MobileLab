package com.example.drivenext.presentation.catalog

import android.os.Bundle
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

internal val zone: ZoneId = ZoneId.systemDefault()
internal val dateTime = DateTimeFormatter.ofPattern("HH:mm, d MMMM yyyy", Locale.forLanguageTag("ru")).withZone(zone)
internal val date = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")).withZone(zone)

internal class RentalPeriod(state: Bundle? = null) {
    var start = state?.getString("rental_start")?.let(Instant::parse)
        ?: LocalDate.now(zone).plusDays(1).atTime(8, 0).atZone(zone).toInstant()
    var end = state?.getString("rental_end")?.let(Instant::parse)
        ?: start.atZone(zone).plusDays(3).toInstant()
    var key = state?.getString("rental_key") ?: UUID.randomUUID().toString()

    fun save(state: Bundle) {
        state.putString("rental_start", start.toString())
        state.putString("rental_end", end.toString())
        state.putString("rental_key", key)
    }
}

internal fun days(start: Instant, end: Instant): Long {
    val seconds = Duration.between(start, end).seconds
    return ((seconds + 86399) / 86400).coerceAtLeast(1)
}

internal fun durationLabel(days: Long): String {
    val word = when {
        days % 100 in 11..14 -> "дней"
        days % 10 == 1L -> "день"
        days % 10 in 2..4 -> "дня"
        else -> "дней"
    }
    return "×$days $word"
}

internal data class Booking(val data: JSONObject) {
    val id get() = data.getLong("id")
    val car get() = data.getJSONObject("car").toCar()
    val start: Instant get() = Instant.parse(data.getString("starts_at"))
    val end: Instant get() = Instant.parse(data.getString("ends_at"))
    val active get() = !data.optBoolean("cancelled") && end.isAfter(Instant.now())
    val status get() = when {
        data.optBoolean("cancelled") -> "Отменено"
        active -> "Одобрено"
        else -> "Завершено"
    }
}

