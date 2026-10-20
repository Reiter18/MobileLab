package com.drivenext.app.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.drivenext.app.R

data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

object OnboardingPages {
    val items: List<OnboardingPage> = listOf(
        OnboardingPage(R.drawable.onboarding_1, R.string.onboarding_title_1, R.string.onboarding_text_1),
        OnboardingPage(R.drawable.onboarding_2, R.string.onboarding_title_2, R.string.onboarding_text_2),
        OnboardingPage(R.drawable.onboarding_3, R.string.onboarding_title_3, R.string.onboarding_text_3)
    )
}
