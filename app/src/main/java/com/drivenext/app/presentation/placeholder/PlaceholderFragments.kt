package com.drivenext.app.presentation.placeholder

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.drivenext.app.R

abstract class PlaceholderFragment(
    @StringRes private val textRes: Int
) : Fragment(R.layout.fragment_placeholder) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.placeholderText).setText(textRes)
    }
}

class LoginPlaceholderFragment : PlaceholderFragment(R.string.placeholder_login)

class MainPlaceholderFragment : PlaceholderFragment(R.string.placeholder_main)
