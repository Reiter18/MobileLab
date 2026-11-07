package com.example.drivenext.presentation.auth

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class CompletionScreens(private val ui: AuthUi) {
    fun success(onNext: () -> Unit) = with(ui) {
        title("Пользуйтесь с удовольствием")
        spacer(150)
        val tick = TextView(activity).apply {
            text = "✓"
            textSize = 45f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(purple, 100)
        }
        body.addView(tick, LinearLayout.LayoutParams(dp(82), dp(82)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        spacer(36)
        text("Поздравляем", 25f, Color.BLACK, true).typeface = Typeface.DEFAULT_BOLD
        spacer(24)
        text("Вы успешно зарегистрировались. Обработка ваших персональных данных займет около двух часов. Спасибо за ожидание.",
            14f, Color.DKGRAY, true)
        bottomButton("Далее", onNext)
    }

    fun home(onSignOut: () -> Unit) = with(ui) {
        title("DriveNext")
        spacer(90)
        text("Главный экран", 25f, purple, true)
        bottomButton("Выйти", onSignOut)
    }
}
