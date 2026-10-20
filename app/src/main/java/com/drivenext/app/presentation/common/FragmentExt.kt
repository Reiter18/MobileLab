package com.drivenext.app.presentation.common

import androidx.fragment.app.Fragment
import com.drivenext.app.AppContainer
import com.drivenext.app.DriveNextApp

val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as DriveNextApp).container
