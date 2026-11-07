package com.example.drivenext.presentation.startup

import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.drivenext.databinding.ActivityStartupBinding
import com.example.drivenext.presentation.common.DriveNextViewModelFactory
import com.example.drivenext.presentation.common.Navigation
import com.example.drivenext.presentation.auth.AuthActivity
import com.example.drivenext.data.auth.LocalAccountRepository

class StartupActivity : AppCompatActivity() {
    private val viewModel: StartupViewModel by viewModels {
        DriveNextViewModelFactory(applicationContext)
    }
    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable { chooseNextScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val binding = ActivityStartupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler.postDelayed(goNext, 2300L)
    }

    private fun chooseNextScreen() {
        if (LocalAccountRepository(applicationContext).hasSession()) {
            startActivity(Intent(this, AuthActivity::class.java).putExtra(AuthActivity.EXTRA_SCREEN, AuthActivity.HOME))
            finish()
            return
        }
        val completed = viewModel.hasCompletedOnboarding()
        val destination = if (completed) Navigation.SCREEN_GATEWAY else Navigation.SCREEN_ONBOARDING

        when {
            !viewModel.hasInternet() -> Navigation.openOffline(this, destination)
            completed -> Navigation.openGateway(this)
            else -> Navigation.openOnboarding(this)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
