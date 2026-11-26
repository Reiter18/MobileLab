package com.example.drivenext.presentation.auth

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.drivenext.data.auth.Supabase
import com.example.drivenext.domain.auth.AuthFormValidator
import com.example.drivenext.presentation.catalog.CatalogActivity
import java.io.File

class AuthActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SCREEN = "screen"
        const val LOGIN = 0
        const val REGISTER_ONE = 1
        const val REGISTER_TWO = 2
        const val REGISTER_THREE = 3
        const val SUCCESS = 4
        const val HOME = 5
    }

    private val account by lazy { Supabase(applicationContext) }
    private val values = mutableMapOf<String, String>()
    private var screen = LOGIN
    private var enteredFromLogin = false
    private var photoTarget = ""
    private var pendingCameraUri: Uri? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            values[photoTarget] = uri.toString()
            render()
        }
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            pendingCameraUri?.let { values[photoTarget] = it.toString() }
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen = savedInstanceState?.getInt(EXTRA_SCREEN) ?: intent.getIntExtra(EXTRA_SCREEN, LOGIN)
        enteredFromLogin = savedInstanceState?.getBoolean("entered_from_login") ?: false
        photoTarget = savedInstanceState?.getString("photo_target").orEmpty()
        pendingCameraUri = savedInstanceState?.getString("camera_uri")?.let(Uri::parse)
        savedInstanceState?.getStringArrayList("entries")?.forEach {
            val separator = it.indexOf('=')
            if (separator > 0) values[it.substring(0, separator)] = it.substring(separator + 1)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goBack() }
        })
        render()
        if (intent.data != null) {
            val callback = intent.data
            intent.data = null
            handleLink(callback)
        }
        else if (account.needsPassword()) passwordDialog()
        else if (screen == HOME && account.hasSession() && !account.registrationPending()) openCatalog()
        else if (screen == HOME) show(LOGIN)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_SCREEN, screen)
        outState.putBoolean("entered_from_login", enteredFromLogin)
        outState.putString("photo_target", photoTarget)
        outState.putString("camera_uri", pendingCameraUri?.toString())
        outState.putStringArrayList("entries", ArrayList(values.filterKeys { it !in listOf("password", "repeat", "login_password") }.map { "${it.key}=${it.value}" }))
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val ui = AuthUi(this, values, ::goBack, ::choosePhoto)
        ui.attach()
        when (screen) {
            LOGIN -> LoginScreen(ui).show(::signIn, {
                enteredFromLogin = true
                show(REGISTER_ONE)
            }, ::google, ::recover)
            REGISTER_ONE -> RegistrationScreens(ui, values).first { advance(AuthFormValidator.firstStep(values), REGISTER_TWO) }
            REGISTER_TWO -> RegistrationScreens(ui, values).second { advance(AuthFormValidator.secondStep(values), REGISTER_THREE) }
            REGISTER_THREE -> RegistrationScreens(ui, values).third(::completeRegistration, ::choosePhoto)
            SUCCESS -> CompletionScreens(ui).success(::openCatalog)
        }
    }

    private fun signIn() {
        AuthFormValidator.login(values)?.let { message(it); return }
        val email = values.getValue("login_email").trim()
        val password = values.getValue("login_password")
        network({ account.signIn(email, password) }) {
            if (account.registrationPending(email)) {
                values["email"] = email
                message("Заполни анкету, чтобы закончить регистрацию.")
                show(REGISTER_ONE)
            } else openCatalog()
        }
    }

    private fun completeRegistration() {
        if (AuthFormValidator.firstStep(values) != null) {
            message("Проверь почту и пароль на первом шаге.")
            show(REGISTER_ONE)
            return
        }
        if (AuthFormValidator.secondStep(values) != null) {
            message("Проверь личные данные на втором шаге.")
            show(REGISTER_TWO)
            return
        }
        AuthFormValidator.thirdStep(values)?.let { message(it); return }
        val form = values.toMap()
        network({ account.register(form) }) {
            for (key in listOf("password", "repeat", "login_password")) values.remove(key)
            show(SUCCESS)
        }
    }

    private fun google() {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, account.googleUrl())) }
            .onFailure { message(it.message ?: "Не удалось открыть Google.") }
    }

    private fun recover() {
        val email = values["login_email"].orEmpty().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            message("Введи почту для восстановления пароля.")
            return
        }
        network({ account.recover(email) }) { message("Если аккаунт существует, на почту придёт письмо.") }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent.data)
    }

    private fun handleLink(uri: Uri?) {
        if (uri?.toString()?.substringBefore('?') != Supabase.REDIRECT) return
        network({ account.exchange(requireNotNull(uri)) }) { recovery ->
            if (recovery) passwordDialog() else openCatalog()
        }
    }

    private fun passwordDialog() {
        val input = EditText(this).apply {
            hint = "Минимум 6 символов"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val dialog = AlertDialog.Builder(this).setTitle("Новый пароль").setView(input)
            .setCancelable(false)
            .setNegativeButton("Выйти") { _, _ -> network({ runCatching { account.signOut() } }) { show(LOGIN) } }
            .setPositiveButton("Сохранить", null).show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password = input.text.toString()
            if (password.length < 6) { input.error = "Минимум 6 символов"; return@setOnClickListener }
            network({ account.changePassword(password) }) {
                dialog.dismiss()
                openCatalog()
            }
        }
    }

    private fun <T> network(work: () -> T, done: (T) -> Unit) {
        val progress = AlertDialog.Builder(this).setMessage("Подождите…").setCancelable(false).show()
        account.run(work) { result ->
            if (!isFinishing && !isDestroyed) {
                progress.dismiss()
                result.onSuccess(done).onFailure { message(it.message ?: "Не удалось подключиться.") }
            }
        }
    }

    private fun openCatalog() {
        startActivity(Intent(this, CatalogActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun advance(error: String?, destination: Int) {
        if (error != null) message(error) else show(destination)
    }

    private fun show(destination: Int) {
        screen = destination
        render()
    }

    private fun choosePhoto(target: String) {
        photoTarget = target
        AlertDialog.Builder(this).setItems(arrayOf("Выбрать из галереи", "Сделать фото")) { _, option ->
            if (option == 0) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                val directory = File(cacheDir, "photos").apply { mkdirs() }
                val file = File.createTempFile("image_", ".jpg", directory)
                pendingCameraUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                takePhoto.launch(pendingCameraUri!!)
            }
        }.show()
    }

    private fun goBack() {
        when (screen) {
            REGISTER_THREE -> show(REGISTER_TWO)
            REGISTER_TWO -> show(REGISTER_ONE)
            REGISTER_ONE -> if (enteredFromLogin) show(LOGIN) else finish()
            else -> finish()
        }
    }

    private fun message(value: String) { Toast.makeText(this, value, Toast.LENGTH_LONG).show() }
}
