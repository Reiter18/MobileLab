package com.example.drivenext.presentation.auth

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.drivenext.data.auth.LocalAccountRepository
import com.example.drivenext.domain.auth.AuthFormValidator
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

    private val account by lazy { LocalAccountRepository(applicationContext) }
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_SCREEN, screen)
        outState.putBoolean("entered_from_login", enteredFromLogin)
        outState.putString("photo_target", photoTarget)
        outState.putString("camera_uri", pendingCameraUri?.toString())
        outState.putStringArrayList("entries", ArrayList(values.map { "${it.key}=${it.value}" }))
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
            }, ::message)
            REGISTER_ONE -> RegistrationScreens(ui, values).first { advance(AuthFormValidator.firstStep(values), REGISTER_TWO) }
            REGISTER_TWO -> RegistrationScreens(ui, values).second { advance(AuthFormValidator.secondStep(values), REGISTER_THREE) }
            REGISTER_THREE -> RegistrationScreens(ui, values).third(::completeRegistration, ::choosePhoto)
            SUCCESS -> CompletionScreens(ui).success { show(HOME) }
            else -> CompletionScreens(ui).home {
                account.signOut()
                finish()
            }
        }
    }

    private fun signIn() {
        AuthFormValidator.login(values)?.let { message(it); return }
        if (!account.signIn(values["login_email"].orEmpty(), values["login_password"].orEmpty())) {
            message("Неверная электронная почта или пароль.")
            return
        }
        show(HOME)
    }

    private fun completeRegistration() {
        AuthFormValidator.thirdStep(values)?.let { message(it); return }
        account.register(values["email"].orEmpty(), values["password"].orEmpty())
        show(SUCCESS)
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
