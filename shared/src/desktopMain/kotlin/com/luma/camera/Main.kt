package com.luma.camera

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Luma Camera AI"
    ) {
        App()
    }
}
