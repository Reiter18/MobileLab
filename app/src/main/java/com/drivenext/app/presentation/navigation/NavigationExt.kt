package com.drivenext.app.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.drivenext.app.R
import com.drivenext.app.domain.model.StartDestination

fun NavController.openStartDestination(destination: StartDestination) {
    val currentId = currentDestination?.id ?: return
    val targetId = when (destination) {
        StartDestination.NO_CONNECTION -> R.id.noConnectionFragment
        StartDestination.ONBOARDING -> R.id.onboardingFragment
        StartDestination.LOGIN -> R.id.loginFragment
        StartDestination.MAIN -> R.id.mainFragment
    }
    if (currentId == targetId) return
    navigate(
        targetId,
        null,
        navOptions { popUpTo(currentId) { inclusive = true } }
    )
}
