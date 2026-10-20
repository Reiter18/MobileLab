package com.drivenext.app.presentation.onboarding

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.drivenext.app.R

class OnboardingPageFragment : Fragment(R.layout.fragment_onboarding_page) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        view.findViewById<ImageView>(R.id.image).setImageResource(args.getInt(ARG_IMAGE))
        view.findViewById<TextView>(R.id.title).setText(args.getInt(ARG_TITLE))
        view.findViewById<TextView>(R.id.description).setText(args.getInt(ARG_DESCRIPTION))
    }

    companion object {
        private const val ARG_IMAGE = "image"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"

        fun newInstance(page: OnboardingPage) = OnboardingPageFragment().apply {
            arguments = bundleOf(
                ARG_IMAGE to page.imageRes,
                ARG_TITLE to page.titleRes,
                ARG_DESCRIPTION to page.descriptionRes
            )
        }
    }
}
