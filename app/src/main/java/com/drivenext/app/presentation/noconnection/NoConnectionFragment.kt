package com.drivenext.app.presentation.noconnection

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.drivenext.app.R
import com.drivenext.app.databinding.FragmentNoConnectionBinding
import com.drivenext.app.presentation.common.appContainer
import com.drivenext.app.presentation.navigation.openStartDestination
import kotlinx.coroutines.launch

class NoConnectionFragment : Fragment(R.layout.fragment_no_connection) {

    private var _binding: FragmentNoConnectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoConnectionViewModel by viewModels {
        NoConnectionViewModel.factory(appContainer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNoConnectionBinding.bind(view)

        binding.retryButton.setOnClickListener { viewModel.onRetryClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.destination.collect { destination ->
                        findNavController().openStartDestination(destination)
                    }
                }
            }
        }
    }

    private fun render(state: NoConnectionUiState) {
        binding.progress.isVisible = state.isLoading
        binding.retryButton.isEnabled = !state.isLoading
        binding.retryButton.text =
            if (state.isLoading) "" else getString(R.string.no_connection_retry)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
