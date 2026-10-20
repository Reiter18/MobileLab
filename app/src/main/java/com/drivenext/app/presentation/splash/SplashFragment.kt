package com.drivenext.app.presentation.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.drivenext.app.R
import com.drivenext.app.presentation.common.appContainer
import com.drivenext.app.presentation.navigation.openStartDestination
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels { SplashViewModel.factory(appContainer) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.destination.collect { destination ->
                    findNavController().openStartDestination(destination)
                }
            }
        }
    }
}
