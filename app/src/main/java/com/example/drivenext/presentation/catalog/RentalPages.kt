package com.example.drivenext.presentation.catalog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.drivenext.R
import com.example.drivenext.data.auth.Supabase
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class RentalPages(
    private val activity: AppCompatActivity,
    private val content: LinearLayout,
    private val account: Supabase
) {
    private val favorites get() = FavoritesDb.get(activity).favorites()
    private val accent = Color.rgb(247, 243, 253)

    fun details(id: Long, back: () -> Unit, reserve: () -> Unit) = with(activity) {
        catalogHeading(content, "Детали", back)
        val header = content.getChildAt(0) as FrameLayout
        val heart = picture(R.drawable.lab4_heart).apply {
            visibility = View.INVISIBLE
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        header.addView(heart, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END or Gravity.CENTER_VERTICAL))
        val body = scroll()
        load(body, { account.car(id).toCar() to favorites.ids(account.ownerKey()).contains(id) }) { (car, saved) ->
            val image = picture(carPicture(car, full = true))
            val photoHeight = if (car.brand == "Tesla") resources.displayMetrics.widthPixels * 264 / 388 else dp(215)
            body.addView(image, LinearLayout.LayoutParams(-1, photoHeight))
            val info = inset(body)
            info.addView(label("${car.brand} ${car.model}", 19f, ink, true).apply {
                setPadding(0, dp(12), 0, dp(12))
            })
            line(info)
            info.addView(label("Адрес нахождения", 13f, muted).apply { setPadding(0, dp(12), 0, 0) })
            location(info, car.address)
            line(info)
            info.addView(label("Описание", 16f, ink, true).apply { setPadding(0, dp(12), 0, dp(8)) })
            info.addView(label(car.description.ifBlank { "${car.brand} ${car.model} · ${car.transmission} · ${car.fuel}" }, 14f).apply {
                setLineSpacing(dp(5).toFloat(), 1f)
                setPadding(0, 0, 0, dp(14))
            })
            val foot = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(8), dp(18), dp(10))
                addView(label("${car.price}₽/день", 14f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
                addView(action("Забронировать", true, reserve), LinearLayout.LayoutParams(dp(155), dp(42)))
            }
            content.addView(foot)
            var selected = saved
            fun paintHeart() {
                heart.setImageResource(if (selected) R.drawable.lab4_heart_on else R.drawable.lab4_heart)
                heart.contentDescription = if (selected) "Убрать из избранного" else "Добавить в избранное"
            }
            paintHeart()
            heart.visibility = View.VISIBLE
            heart.setOnClickListener {
                heart.isEnabled = false
                val next = !selected
                account.run({
                    val owner = account.ownerKey()
                    if (next) favorites.add(Favorite(owner, id)) else favorites.remove(owner, id)
                }) { result ->
                    if (heart.isAttachedToWindow) {
                        heart.isEnabled = true
                        result.onSuccess { selected = next; paintHeart() }.onFailure {
                            alert("Не удалось изменить избранное. Попробуйте снова.")
                        }
                    }
                }
            }
        }
    }

    fun checkout(id: Long, period: RentalPeriod, back: () -> Unit, success: () -> Unit) = with(activity) {
        catalogHeading(content, "Оформление аренды", back)
        val body = inset(scroll())
        load(body, { account.car(id).toCar() }) { car ->
            summary(body, car)
            val start = pair(body, "Начало аренды:", "")
            val end = pair(body, "Конец аренды:", "")
            line(body)
            location(body, car.address, "Адрес нахождения")
            line(body)
            val rent = pair(body, "", "")
            val insurance = pair(body, "", "")
            val totalBox = column().apply {
                background = shape(accent, 8)
                setPadding(dp(14), dp(4), dp(14), dp(6))
            }
            body.addView(totalBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val total = pair(totalBox, "Итого", "", true)
            pair(totalBox, "Возвращаемый депозит", "${car.deposit}₽")
            val submit = action("Продолжить", true) {
                if (period.start.isBefore(Instant.now())) alert("Дата начала аренды уже прошла.")
                else if (!period.end.isAfter(period.start)) alert("Конец аренды должен быть позже начала.")
                else write({
                    account.book(id, period.start.toString(), period.end.toString(), period.key)
                }, success)
            }
            fun update() {
                start.second.text = dateTime.format(period.start)
                end.second.text = dateTime.format(period.end)
                val count = days(period.start, period.end)
                rent.first.text = "Аренда автомобиля ${durationLabel(count)}"
                rent.second.text = "${car.price * count}₽"
                insurance.first.text = "Страховка ${durationLabel(count)}"
                insurance.second.text = "${car.insurance * count}₽"
                total.second.text = "${(car.price.toLong() + car.insurance) * count}₽"
            }
            start.second.setOnClickListener { chooseTime(period.start) {
                period.start = it
                if (!period.end.isAfter(it)) period.end = it.plus(Duration.ofDays(3))
                period.key = UUID.randomUUID().toString()
                update()
            } }
            end.second.setOnClickListener { chooseTime(period.end) {
                if (!it.isAfter(period.start)) alert("Конец аренды должен быть позже начала.")
                else {
                    period.end = it
                    period.key = UUID.randomUUID().toString()
                    update()
                }
            } }
            update()
            addBottom(submit)
        }
    }

    fun success(bookings: () -> Unit, home: () -> Unit) = with(activity) {
        catalogHeading(content, "Удачной поездки")
        content.addView(View(this), LinearLayout.LayoutParams(1, 0, 2f))
        content.addView(picture(R.drawable.lab4_check), LinearLayout.LayoutParams(dp(88), dp(88)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        content.addView(label("Бронирование успешно создано", 18f, ink, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(25), dp(8), 0)
        })
        content.addView(View(this), LinearLayout.LayoutParams(1, 0, 3f))
        content.addView(label("Перейти к своим бронированиям", 14f).apply {
            gravity = Gravity.CENTER
            setOnClickListener { bookings() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        addBottom(action("Домой", true, home))
    }

    fun bookings(back: () -> Unit, open: (Long) -> Unit) = with(activity) {
        catalogHeading(content, "Мои бронирования", back)
        val body = inset(scroll())
        load(body, {
            val json = account.bookings()
            List(json.length()) { Booking(json.getJSONObject(it)) }.sortedByDescending { it.active }
        }) { items ->
            if (items.isEmpty()) body.addView(label("Пока нет бронирований", 15f))
            items.forEach { item ->
                val car = item.car
                val row = column().apply {
                    setPadding(0, dp(13), 0, dp(13))
                    addView(label("${car.brand} ${car.model}", 17f, ink, true))
                    val status = when {
                        item.active -> "Начало аренды: ${dateTime.format(item.start)}"
                        item.status == "Отменено" -> "Бронирование отменено"
                        else -> "Аренда завершена, ${date.format(item.end)}"
                    }
                    addView(label(status, 14f, muted))
                    setOnClickListener { open(item.id) }
                }
                body.addView(row)
                line(body)
            }
        }
    }

    fun booking(id: Long, fallbackName: String, back: () -> Unit) = with(activity) {
        catalogHeading(content, "Бронирование # $id", back)
        val body = inset(scroll())
        load(body, { Booking(account.booking(id)) to account.driver() }) { (item, driver) ->
            val info = item.data
            val count = days(item.start, item.end)
            val price = info.getInt("daily_price")
            val insurance = info.getInt("daily_insurance")
            val name = listOf("surname", "first_name", "patronymic")
                .mapNotNull { driver?.optString(it)?.takeIf { value -> value.isNotBlank() && value != "null" } }
                .joinToString(" ").ifBlank { fallbackName.ifBlank { "Не указано" } }
            summary(body, item.car.copy(price = price))
            pair(body, "Адрес нахождения", info.optString("address"))
            pair(body, "Начало аренды", dateTime.format(item.start))
            pair(body, "Конец аренды", dateTime.format(item.end))
            pair(body, "ФИО водителя", name)
            pair(body, "Номер ВУ", driver?.optString("licence") ?: "Не указан")
            pair(body, "Статус аренды", item.status)
            line(body)
            pair(body, "Аренда ${durationLabel(count)}", "${price * count}₽")
            pair(body, "Страховка ${durationLabel(count)}", "${insurance * count}₽")
            pair(body, "Общая сумма", "${(price.toLong() + insurance) * count}₽", true)
            if (item.active) addBottom(action("Отменить бронирование", true) {
                AlertDialog.Builder(this).setMessage("Отменить бронирование?")
                    .setNegativeButton("Назад", null)
                    .setPositiveButton("Отменить") { _, _ -> write({ account.cancelBooking(id) }, back) }
                    .show()
            })
        }
    }

    private fun carPicture(car: Car, full: Boolean = false) = when {
        car.brand == "Tesla" -> R.drawable.lab4_tesla
        full -> R.drawable.car
        else -> R.drawable.lab4_rental_car
    }

    private fun summary(parent: LinearLayout, car: Car) = with(activity) {
        val box = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = shape(Color.WHITE, 7, Color.BLACK)
            setPadding(dp(20), 0, dp(12), 0)
            val text = column()
            text.addView(label(car.model, 16f, ink, true))
            text.addView(label(car.brand, 13f, muted))
            text.addView(label("${car.price}₽ в день", 14f, purple, true))
            addView(text, LinearLayout.LayoutParams(0, -2, 1f))
            addView(picture(carPicture(car)), LinearLayout.LayoutParams(0, dp(100), 1.4f))
        }
        parent.addView(box, LinearLayout.LayoutParams(-1, dp(177)).apply { bottomMargin = dp(12) })
    }

    private fun scroll() = with(activity) {
        val body = column()
        content.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        body
    }

    private fun inset(parent: LinearLayout) = activity.column().apply {
        setPadding(activity.dp(18), activity.dp(4), activity.dp(18), activity.dp(12))
        parent.addView(this)
    }

    private fun pair(parent: LinearLayout, left: String, right: String, bold: Boolean = false) = with(activity) {
        val title = label(left, if (bold) 16f else 14f, ink, bold)
        val value = label(right, if (bold) 16f else 14f, ink, bold).apply { gravity = Gravity.END }
        parent.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
            addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            addView(value, LinearLayout.LayoutParams(0, -2, 1f))
        })
        title to value
    }

    private fun location(parent: LinearLayout, address: String, subtitle: String = "") = with(activity) {
        parent.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
            addView(picture(R.drawable.lab4_location), LinearLayout.LayoutParams(dp(23), dp(23)))
            addView(column().apply {
                setPadding(dp(12), 0, 0, 0)
                addView(label(address, 14f))
                if (subtitle.isNotEmpty()) addView(label(subtitle, 13f, muted))
            })
        })
    }

    private fun line(parent: LinearLayout) = with(activity) {
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(226, 229, 235)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(5); bottomMargin = dp(5) })
    }

    private fun addBottom(button: View) = with(activity) {
        content.addView(button, LinearLayout.LayoutParams(-1, dp(48)).apply {
            setMargins(dp(18), dp(10), dp(18), dp(24))
        })
    }

    private fun chooseTime(value: Instant, selected: (Instant) -> Unit) {
        val current = value.atZone(zone)
        DatePickerDialog(activity, { _, year, month, day ->
            TimePickerDialog(activity, { _, hour, minute ->
                selected(LocalDate.of(year, month + 1, day).atTime(hour, minute).atZone(zone).toInstant())
            }, current.hour, current.minute, true).show()
        }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
            datePicker.minDate = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        }.show()
    }

    private fun <T> load(body: LinearLayout, work: () -> T, done: (T) -> Unit): Unit = with(activity) {
        val progress = ProgressBar(this)
        body.addView(progress, LinearLayout.LayoutParams(dp(44), dp(44)).apply { gravity = Gravity.CENTER })
        account.run(work) { result ->
            if (content.isAttachedToWindow) {
                body.removeView(progress)
                result.onSuccess(done).onFailure {
                    body.addView(label("Не удалось загрузить данные. Попробуйте снова.", 15f))
                    body.addView(action("Повторить", true) {
                        body.removeAllViews()
                        load(body, work, done)
                    }, LinearLayout.LayoutParams(-1, dp(44)))
                }
            }
        }
    }

    private fun write(work: () -> Unit, done: () -> Unit) {
        val dialog = AlertDialog.Builder(activity).setMessage("Подождите…").setCancelable(false).show()
        account.run(work) { result ->
            if (!activity.isDestroyed && !activity.isFinishing) {
                dialog.dismiss()
                if (content.isAttachedToWindow) result.onSuccess { done() }.onFailure {
                    alert(it.message ?: "Не удалось сохранить бронирование.")
                }
            }
        }
    }

    private fun alert(message: String) {
        AlertDialog.Builder(activity).setMessage(message).setPositiveButton("ОК", null).show()
    }
}
