package com.example.drivenext.presentation.onboarding

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.drivenext.R
import com.example.drivenext.databinding.ActivityOnboardingBinding
import com.example.drivenext.presentation.common.ConnectionAwareActivity
import com.example.drivenext.presentation.common.DriveNextViewModelFactory
import com.example.drivenext.presentation.common.Navigation

class OnboardingActivity : ConnectionAwareActivity() {
    override val returnScreen: String = Navigation.SCREEN_ONBOARDING
    override val returnPage: Int get() = binding.viewPager.currentItem

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels {
        DriveNextViewModelFactory(applicationContext)
    }

    private val pageListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateControls(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingPagerAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(pageListener)
        if (savedInstanceState == null) {
            val firstPage = intent.getIntExtra(Navigation.EXTRA_START_PAGE, 0).coerceIn(0, 2)
            binding.viewPager.setCurrentItem(firstPage, false)
            updateControls(firstPage)
        }

        binding.skipButton.setOnClickListener { finishOnboarding() }
        binding.nextButton.setOnClickListener {
            val currentPage = binding.viewPager.currentItem
            if (currentPage < 2) {
                binding.viewPager.setCurrentItem(currentPage + 1, true)
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateControls(page: Int) {
        binding.nextButton.setText(if (page == 2) R.string.lets_go else R.string.next)
        val dots: List<View> = listOf(binding.dotOne, binding.dotTwo, binding.dotThree)
        dots.forEachIndexed { index, dot ->
            val selected = index == page
            dot.setBackgroundResource(if (selected) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = ((if (selected) 20 else 9) * resources.displayMetrics.density).toInt()
            dot.layoutParams = params
        }
    }

    private fun finishOnboarding() {
        viewModel.finishOnboarding()
        Navigation.openGateway(this)
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageListener)
        super.onDestroy()
    }
}
