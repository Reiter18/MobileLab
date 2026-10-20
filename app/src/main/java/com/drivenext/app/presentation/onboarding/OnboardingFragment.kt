package com.drivenext.app.presentation.onboarding

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import com.drivenext.app.R
import com.drivenext.app.databinding.FragmentOnboardingBinding
import com.drivenext.app.domain.model.StartDestination
import com.drivenext.app.presentation.common.appContainer
import com.drivenext.app.presentation.navigation.openStartDestination
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val pages = OnboardingPages.items

    private val viewModel: OnboardingViewModel by viewModels {
        OnboardingViewModel.factory(appContainer, pages.size)
    }

    private var indicators: List<View> = emptyList()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = viewModel.onPageSelected(position)
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = viewModel.onBackClicked()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        binding.viewPager.adapter =
            OnboardingPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, pages)
        binding.viewPager.setCurrentItem(viewModel.uiState.value.currentPage, false)
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        createIndicators()

        binding.nextButton.setOnClickListener { viewModel.onNextClicked() }
        binding.skipButton.setOnClickListener { viewModel.onSkipClicked() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: OnboardingUiState) {
        updateIndicators(state.currentPage)
        binding.nextButton.setText(
            if (state.isLastPage) R.string.onboarding_start else R.string.onboarding_next
        )
        backCallback.isEnabled = state.currentPage > 0
    }

    private fun handleEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.ScrollToPage -> binding.viewPager.setCurrentItem(event.page, true)
            OnboardingEvent.NavigateToLogin ->
                findNavController().openStartDestination(StartDestination.LOGIN)
        }
    }


    private fun createIndicators() {
        binding.indicatorContainer.removeAllViews()
        indicators = pages.indices.map { index ->
            View(requireContext()).also { dot ->
                dot.layoutParams = LinearLayout.LayoutParams(dp(INACTIVE_WIDTH_DP), dp(HEIGHT_DP)).apply {
                    if (index != pages.lastIndex) marginEnd = dp(GAP_DP)
                }
                binding.indicatorContainer.addView(dot)
            }
        }
        updateIndicators(viewModel.uiState.value.currentPage, animate = false)
    }

    private fun updateIndicators(current: Int, animate: Boolean = true) {
        if (animate) TransitionManager.beginDelayedTransition(binding.indicatorContainer)
        indicators.forEachIndexed { index, dot ->
            val active = index == current
            dot.setBackgroundResource(
                if (active) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
            dot.layoutParams = (dot.layoutParams as ViewGroup.LayoutParams).apply {
                width = dp(if (active) ACTIVE_WIDTH_DP else INACTIVE_WIDTH_DP)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
        indicators = emptyList()
        super.onDestroyView()
    }

    private companion object {
        const val ACTIVE_WIDTH_DP = 40
        const val INACTIVE_WIDTH_DP = 16
        const val HEIGHT_DP = 8
        const val GAP_DP = 8
    }
}
