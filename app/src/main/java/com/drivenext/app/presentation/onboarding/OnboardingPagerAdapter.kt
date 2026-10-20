package com.drivenext.app.presentation.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val pages: List<OnboardingPage>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment =
        OnboardingPageFragment.newInstance(pages[position])
}
