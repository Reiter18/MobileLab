package com.example.drivenext.presentation.auth

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup

class RegistrationScreens(private val ui: AuthUi, private val values: MutableMap<String, String>) {
    fun first(onNext: () -> Unit) = with(ui) {
        title("Создать аккаунт", true)
        spacer(90)
        field("Электронная почта", "Введите электронную почту", "email",
            keyboard = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        field("Придумайте пароль", "Введите пароль", "password", password = true)
        field("Повторите пароль", "Введите пароль", "repeat", password = true)
        val check = CheckBox(activity).apply {
            text = "Я согласен с условиями обслуживания и политикой конфиденциальности"
            textSize = 12f
            setTextColor(Color.BLACK)
            buttonTintList = ColorStateList.valueOf(purple)
            isChecked = values["agreed"] == "true"
            setOnCheckedChangeListener { _, checked -> values["agreed"] = checked.toString() }
        }
        body.addView(check)
        bottomButton("Далее", onNext)
    }

    fun second(onNext: () -> Unit) = with(ui) {
        title("Создать аккаунт", true)
        spacer(28)
        field("Фамилия", "Введите фамилию", "surname")
        field("Имя", "Введите имя", "first_name")
        field("Отчество", "Введите отчество", "patronymic")
        dateField("Дата рождения", "birth")
        text("Пол")
        val group = RadioGroup(activity).apply { orientation = RadioGroup.HORIZONTAL }
        listOf("Мужской", "Женский").forEach { gender ->
            group.addView(RadioButton(activity).apply {
                text = gender
                textSize = 14f
                setTextColor(hintColor)
                buttonTintList = ColorStateList.valueOf(purple)
                isChecked = values["gender"] == gender
                setOnClickListener { values["gender"] = gender }
            })
        }
        body.addView(group)
        bottomButton("Далее", onNext)
    }

    fun third(onNext: () -> Unit, onPhoto: (String) -> Unit) = with(ui) {
        title("Создать аккаунт", true)
        val avatar = ImageView(activity).apply {
            if (values.containsKey("avatar")) setImageURI(Uri.parse(values["avatar"]))
            else {
                setImageResource(android.R.drawable.ic_menu_myplaces)
                imageTintList = avatarTint()
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(32), dp(15), dp(32), dp(15))
            setOnClickListener { onPhoto("avatar") }
            contentDescription = "Добавить фото профиля"
        }
        body.addView(avatar, LinearLayout.LayoutParams(-1, dp(150)))
        text("Добавление фотографии поможет владельцам и арендаторам узнать друг друга, когда они будут забирать машину")
        spacer(18)
        field("Номер водительского удостоверения", "0000000000", "licence", keyboard = InputType.TYPE_CLASS_NUMBER)
        dateField("Дата выдачи", "issued")
        upload("Загрузите фото водительского удостоверения", "licence_photo")
        upload("Загрузите фото паспорта", "passport_photo")
        bottomButton("Далее", onNext)
    }
}
