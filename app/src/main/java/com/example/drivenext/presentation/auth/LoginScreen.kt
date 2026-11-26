package com.example.drivenext.presentation.auth

import android.graphics.Color
import android.widget.ImageView
import com.example.drivenext.R
import android.text.InputType
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class LoginScreen(private val ui: AuthUi) {
    fun show(onSignIn: () -> Unit, onRegister: () -> Unit, onGoogle: () -> Unit, onRecover: () -> Unit) = with(ui) {
        spacer(100)
        title("Войдите в аккаунт")
        text("Пожалуйста, введите свои данные", 14f, hintColor, true)
        spacer(70)
        val email = field("Электронная почта", "Введите электронную почту", "login_email",
            keyboard = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = field("Пароль", "Введите пароль", "login_password", password = true)
        text("Забыли пароль?", 14f, purple, true).setOnClickListener {
            onRecover()
        }
        spacer(33)
        val login = TextView(activity).apply {
            text = "Войти"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.WHITE)
            background = rounded(purple)
            setOnClickListener { onSignIn() }
        }
        enableWhen(login) { email.text.isNotBlank() && password.text.isNotBlank() }
        body.addView(login, LinearLayout.LayoutParams(-1, dp(52)))
        spacer(16)
        val google = LinearLayout(activity).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            background = outline()
            addView(ImageView(activity).apply { setImageResource(R.drawable.lab_google) },
                LinearLayout.LayoutParams(dp(20), dp(20)))
            addView(TextView(activity).apply {
                text = "  Войти через Google"
                textSize = 14f
                setTextColor(Color.BLACK)
            })
            setOnClickListener { onGoogle() }
        }
        body.addView(google, LinearLayout.LayoutParams(-1, dp(52)))
        spacer(52)
        text("Зарегистрироваться", 14f, purple, true).setOnClickListener { onRegister() }
    }
}
