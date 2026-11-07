package com.example.drivenext.presentation.connection

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.drivenext.R
import com.example.drivenext.databinding.ActivityNoConnectionBinding
import com.example.drivenext.presentation.common.DriveNextViewModelFactory
import com.example.drivenext.presentation.common.Navigation
import com.example.drivenext.presentation.common.NetworkViewModel

class NoConnectionActivity : AppCompatActivity() {
    private val viewModel: NetworkViewModel by viewModels {
        DriveNextViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNoConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.retryButton.setOnClickListener {
            if (viewModel.isInternetAvailable()) {
                val previousScreen = intent.getStringExtra(Navigation.EXTRA_RETURN_SCREEN)
                val page = intent.getIntExtra(Navigation.EXTRA_RETURN_PAGE, 0)
                if (previousScreen == Navigation.SCREEN_GATEWAY) {
                    Navigation.openGateway(this)
                } else {
                    Navigation.openOnboarding(this, page)
                }
            } else {
                Toast.makeText(this, R.string.internet_still_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
