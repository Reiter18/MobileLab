package com.example.drivenext.presentation.gateway

import android.os.Bundle
import android.content.Intent
import com.example.drivenext.databinding.ActivityGatewayBinding
import com.example.drivenext.presentation.auth.AuthActivity
import com.example.drivenext.presentation.common.ConnectionAwareActivity
import com.example.drivenext.presentation.common.Navigation

class GatewayActivity : ConnectionAwareActivity() {
    override val returnScreen: String = Navigation.SCREEN_GATEWAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityGatewayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { openAuth(AuthActivity.LOGIN) }
        binding.registerButton.setOnClickListener { openAuth(AuthActivity.REGISTER_ONE) }
    }

    private fun openAuth(screen: Int) {
        startActivity(Intent(this, AuthActivity::class.java).putExtra(AuthActivity.EXTRA_SCREEN, screen))
    }
}
