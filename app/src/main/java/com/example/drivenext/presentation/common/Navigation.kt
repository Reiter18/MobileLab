package com.example.drivenext.presentation.common

import android.app.Activity
import android.content.Intent
import com.example.drivenext.presentation.connection.NoConnectionActivity
import com.example.drivenext.presentation.gateway.GatewayActivity
import com.example.drivenext.presentation.onboarding.OnboardingActivity

object Navigation {
    const val SCREEN_ONBOARDING = "onboarding"
    const val SCREEN_GATEWAY = "gateway"
    const val EXTRA_RETURN_SCREEN = "return_screen"
    const val EXTRA_RETURN_PAGE = "return_page"
    const val EXTRA_START_PAGE = "start_page"

    fun openOffline(activity: Activity, screen: String, page: Int = 0) {
        activity.startActivity(Intent(activity, NoConnectionActivity::class.java).apply {
            putExtra(EXTRA_RETURN_SCREEN, screen)
            putExtra(EXTRA_RETURN_PAGE, page)
        })
        activity.finish()
    }

    fun openOnboarding(activity: Activity, page: Int = 0) {
        activity.startActivity(Intent(activity, OnboardingActivity::class.java).apply {
            putExtra(EXTRA_START_PAGE, page)
        })
        activity.finish()
    }

    fun openGateway(activity: Activity) {
        activity.startActivity(Intent(activity, GatewayActivity::class.java))
        activity.finish()
    }
}
