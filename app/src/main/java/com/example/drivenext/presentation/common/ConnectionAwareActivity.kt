package com.example.drivenext.presentation.common

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

abstract class ConnectionAwareActivity : AppCompatActivity() {
    protected abstract val returnScreen: String
    protected open val returnPage: Int get() = 0

    private val networkViewModel: NetworkViewModel by viewModels {
        DriveNextViewModelFactory(applicationContext)
    }
    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }
    private var isMonitoring = false
    private var isGoingOffline = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) = recheckConnection()
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = recheckConnection()
    }

    override fun onStart() {
        super.onStart()
        if (!networkViewModel.isInternetAvailable()) {
            showOfflineScreen()
            return
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            isMonitoring = true
        } catch (_: RuntimeException) {
        }
    }

    override fun onStop() {
        if (isMonitoring) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (_: RuntimeException) {
            }
            isMonitoring = false
        }
        super.onStop()
    }

    private fun recheckConnection() {
        runOnUiThread {
            if (!isFinishing && !isDestroyed && !networkViewModel.isInternetAvailable()) {
                showOfflineScreen()
            }
        }
    }

    private fun showOfflineScreen() {
        if (isGoingOffline) return
        isGoingOffline = true
        Navigation.openOffline(this, returnScreen, returnPage)
    }
}
