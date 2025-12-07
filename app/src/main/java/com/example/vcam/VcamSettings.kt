package com.example.vcam

data class VcamSettings(
    val enabled: Boolean = true,
    val forceShowTips: Boolean = false,
    val enableShutterSound: Boolean = false,
    val forcePrivateStorage: Boolean = true,
    val suppressToast: Boolean = false,
    val mediaRoot: String = ""
)
