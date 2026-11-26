package com.example.drivenext.presentation.auth

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ImageView
import com.example.drivenext.R

class CompletionScreens(private val ui: AuthUi) {
    fun success(onNext: () -> Unit) = with(ui) {
        title("Пользуйтесь с удовольствием")
        spacer(150)
        val tick = ImageView(activity).apply { setImageResource(R.drawable.lab_success) }
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
}
