package com.example.drivenext.presentation.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.drivenext.R
import com.example.drivenext.databinding.FragmentOnboardingPageBinding

class OnboardingPageFragment : Fragment(R.layout.fragment_onboarding_page) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOnboardingPageBinding.bind(view)
        val position = requireArguments().getInt(ARG_PAGE)

        val illustration: Int
        val title: Int
        val description: Int

        when (position) {
            0 -> {
                illustration = R.drawable.onboarding_1
                title = R.string.onboarding_title_1
                description = R.string.onboarding_description_1
            }
            1 -> {
                illustration = R.drawable.onboarding_2
                title = R.string.onboarding_title_2
                description = R.string.onboarding_description_2
            }
            else -> {
                illustration = R.drawable.onboarding_3
                title = R.string.onboarding_title_3
                description = R.string.onboarding_description_3
            }
        }
        binding.illustration.setImageResource(illustration)
        binding.illustration.contentDescription = getString(title)
        binding.pageTitle.setText(title)
        binding.pageDescription.setText(description)
    }

    companion object {
        private const val ARG_PAGE = "page_number"

        fun newInstance(position: Int) = OnboardingPageFragment().apply {
            arguments = Bundle().apply { putInt(ARG_PAGE, position) }
        }
    }
}
