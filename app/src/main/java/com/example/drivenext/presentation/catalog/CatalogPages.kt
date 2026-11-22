package com.example.drivenext.presentation.catalog

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.drivenext.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun Context.catalogNavigation(page: String, navigate: (String) -> Unit) = LinearLayout(this).apply {
    setPadding(dp(36), 0, dp(36), 0)
    elevation = dp(3).toFloat()
    setBackgroundColor(Color.WHITE)
    val settingsSelected = page in listOf("settings", "profile", "add")
    val homeSelected = !settingsSelected && page != "bookings"
    val icons = listOf(
        Triple(if (homeSelected) R.drawable.home_on else R.drawable.home_off, "Главная", "home"),
        Triple(R.drawable.saved, "Мои бронирования", "bookings"),
        Triple(if (settingsSelected) R.drawable.gear_on else R.drawable.gear_off, "Настройки", "settings")
    )
    for ((icon, name, destination) in icons) {
        val item = FrameLayout(this@catalogNavigation).apply {
            contentDescription = name
            isSelected = when (destination) {
                "home" -> homeSelected
                "settings" -> settingsSelected
                else -> page == "bookings"
            }
            setOnClickListener {
                navigate(destination)
            }
            addView(picture(icon).apply {
                if (destination == "bookings" && page == "bookings") setColorFilter(purple)
            }, FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER))
        }
        addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }
}


internal fun Context.catalogHeading(parent: LinearLayout, title: String, back: (() -> Unit)? = null) {
    val row = FrameLayout(this)
    row.addView(label(title, 19f, ink, true).apply { gravity = Gravity.CENTER },
        FrameLayout.LayoutParams(-1, -1))
    if (back != null) row.addView(label("‹", 32f).apply {
        gravity = Gravity.CENTER
        contentDescription = "Назад"
        setOnClickListener { back() }
    }, FrameLayout.LayoutParams(dp(52), -1, Gravity.START))
    parent.addView(row, LinearLayout.LayoutParams(-1, dp(62)))
}


internal class CatalogPages(
    private val activity: AppCompatActivity,
    private val content: LinearLayout,
    private val prefs: SharedPreferences,
    private val show: (String) -> Unit,
    private val pickAvatar: () -> Unit,
    private val signOut: () -> Unit
) {
    private fun scrollBody(): LinearLayout {
        val body = activity.column()
        content.addView(ScrollView(activity).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return body
    }

    private fun avatar(fallback: Int) = activity.picture(fallback).apply {
        val saved = prefs.getString("avatar", null)
        if (saved != null) {
            runCatching { setImageURI(Uri.parse(saved)) }
            if (drawable == null) setImageResource(fallback)
            clipToOutline = true
            background = activity.shape(Color.rgb(235, 235, 235), 100)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun name() = prefs.getString("name", "").orEmpty().ifBlank { "Пользователь" }
    private fun email() = prefs.getString("email", "").orEmpty()

    fun settings() {
        activity.catalogHeading(content, "Настройки")
        val body = scrollBody()
        row(body, name(), email(), R.drawable.person, true, 65) { show("profile") }
        row(body, "Мои бронирования", icon = R.drawable.booking, arrow = true) { show("bookings") }
        row(body, "Тема", icon = R.drawable.theme, arrow = true) { info("Тема") }
        row(body, "Уведомления", icon = R.drawable.bell, arrow = true) { info("Уведомления") }
        row(body, "Подключить свой автомобиль", icon = R.drawable.connect, arrow = true) { show("add") }
        line(body)
        row(body, "Помощь", icon = R.drawable.help, arrow = true) { info("Помощь") }
        row(body, "Пригласи друга", icon = R.drawable.invite, arrow = true) {
            activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Попробуй DriveNext — приложение для аренды автомобилей.")
            }, "Пригласи друга"))
        }
    }

    fun profile() {
        activity.catalogHeading(content, "Профиль")
        val body = scrollBody()
        val photo = FrameLayout(activity).apply {
            contentDescription = "Изменить аватар"
            addView(avatar(R.drawable.avatar), FrameLayout.LayoutParams(-1, -1))
            if (prefs.contains("avatar")) addView(activity.label("+", 16f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = activity.shape(purple, 20)
            }, FrameLayout.LayoutParams(activity.dp(20), activity.dp(20), Gravity.BOTTOM or Gravity.END))
            setOnClickListener { pickAvatar() }
        }
        body.addView(photo, LinearLayout.LayoutParams(activity.dp(92), activity.dp(92)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = activity.dp(12)
        })
        body.addView(activity.label(name(), 21f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, activity.dp(14), 0, 0)
        })
        val joined = prefs.getLong("joined", 0L)
        if (joined != 0L) {
            val month = SimpleDateFormat("MMMM yyyy", Locale("ru")).format(Date(joined))
            body.addView(activity.label("Дата регистрации · $month", 14f).apply { gravity = Gravity.CENTER })
        }
        body.addView(View(activity), LinearLayout.LayoutParams(1, activity.dp(18)))
        row(body, "Электронная почта", email())
        line(body)
        row(body, "Пароль", "Поменять пароль") { info("Пароль") }
        line(body)
        row(body, "Пол", prefs.getString("gender", "").orEmpty().ifBlank { "Не указан" })
        line(body)
        row(body, "Google", "Не подключён")
        line(body)
        row(body, "Выйти из профиля", click = signOut)
    }

    private fun row(
        parent: LinearLayout, title: String, subtitle: String = "", icon: Int = 0,
        arrow: Boolean = false, iconSize: Int = 22, click: (() -> Unit)? = null
    ) {
        val row = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(18), activity.dp(10), activity.dp(16), activity.dp(10))
            minimumHeight = activity.dp(if (subtitle.isBlank()) 47 else 64)
        }
        if (icon != 0) {
            val image = if (icon == R.drawable.person) avatar(icon) else activity.picture(icon)
            row.addView(image, LinearLayout.LayoutParams(activity.dp(iconSize), activity.dp(iconSize)).apply { rightMargin = activity.dp(8) })
        }
        val labels = activity.column()
        labels.addView(activity.label(title, 15f, purple))
        if (subtitle.isNotBlank()) labels.addView(activity.label(subtitle, 13f, muted))
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        if (arrow) row.addView(activity.label("›", 25f, purple))
        if (click != null) row.setOnClickListener { click() }
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun line(body: LinearLayout) {
        body.addView(View(activity).apply { setBackgroundColor(Color.rgb(224, 227, 233)) },
            LinearLayout.LayoutParams(-1, activity.dp(1)))
    }

    fun carPage(car: Car, reserve: Boolean, back: String) {
        activity.catalogHeading(content, if (reserve) "Оформление аренды" else "Детали") { show(back) }
        val body = scrollBody()
        body.addView(activity.picture(R.drawable.car), LinearLayout.LayoutParams(-1, activity.dp(160)))
        row(body, car.model, car.brand)
        row(body, "${car.price}₽ в день", "${car.transmission} · ${car.fuel}")
        if (reserve) row(body, "Подтвердить бронирование") {
            val entries = prefs.getStringSet("bookings", emptySet()).orEmpty().toMutableSet()
            entries.add("${car.id}| ${car.brand} ${car.model} · ${car.price}₽ в день")
            prefs.edit().putStringSet("bookings", entries).apply()
            show("bookings")
        }
    }

    fun addCar() {
        activity.catalogHeading(content, "Подключить свой автомобиль") { show("settings") }
        val body = scrollBody().apply { setPadding(activity.dp(18), activity.dp(12), activity.dp(18), 0) }
        val car = EditText(activity).apply {
            hint = "Марка и модель"
            setSingleLine()
            setText(prefs.getString("my_car", ""))
        }
        body.addView(car)
        body.addView(activity.action("Сохранить черновик", true) {
            val text = car.text.toString().trim()
            if (text.isBlank()) car.error = "Введите марку и модель"
            else {
                prefs.edit().putString("my_car", text).apply()
                show("settings")
            }
        }, LinearLayout.LayoutParams(-1, activity.dp(44)))
    }

    fun bookings() {
        activity.catalogHeading(content, "Мои бронирования")
        val body = scrollBody()
        val entries = prefs.getStringSet("bookings", emptySet()).orEmpty()
        if (entries.isEmpty()) row(body, "Пока нет бронирований")
        else entries.sorted().forEach { row(body, it.substringAfter("| ")) }
    }

    private fun info(title: String) {
        AlertDialog.Builder(activity).setTitle(title).setMessage("Раздел появится позже.")
            .setPositiveButton("Закрыть", null).show()
    }
}
