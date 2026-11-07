package com.example.drivenext.domain.auth

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Locale

object AuthFormValidator {
    private val registrationEmail = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|ru)$",
        RegexOption.IGNORE_CASE
    )

    fun login(values: Map<String, String>): String? {
        val email = values["login_email"].orEmpty().trim()
        val password = values["login_password"].orEmpty()
        return if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.isBlank())
            "Введите корректную электронную почту и пароль." else null
    }

    fun firstStep(values: Map<String, String>): String? {
        if (!registrationEmail.matches(values["email"].orEmpty().trim()))
            return "Введите корректный адрес электронной почты."
        if (values["password"].isNullOrBlank()) return "Введите пароль."
        if (values["password"] != values["repeat"]) return "Пароли не совпадают."
        if (values["agreed"] != "true")
            return "Необходимо согласиться с условиями обслуживания и политикой конфиденциальности."
        return null
    }

    fun secondStep(values: Map<String, String>): String? {
        if (listOf("surname", "first_name", "birth", "gender").any { values[it].isNullOrBlank() })
            return "Пожалуйста, заполните все обязательные поля."
        if (!validPastDate(values["birth"].orEmpty())) return "Введите корректную дату рождения."
        return null
    }

    fun thirdStep(values: Map<String, String>): String? {
        if (values["licence"].orEmpty().length != 10 || values["issued"].isNullOrBlank())
            return "Пожалуйста, заполните все обязательные поля."
        if (!validPastDate(values["issued"].orEmpty())) return "Введите корректную дату выдачи."
        if (values["licence_photo"].isNullOrBlank() || values["passport_photo"].isNullOrBlank())
            return "Пожалуйста, загрузите все необходимые фото."
        return null
    }

    private fun validPastDate(value: String): Boolean = try {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(value)
        date != null && date.time <= System.currentTimeMillis()
    } catch (_: Exception) { false }
}
