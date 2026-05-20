package dev.karipap.app.launcher

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class CoreNotInstalled(val coreName: String) : LaunchResult
    data class AppNotInstalled(val packageName: String) : LaunchResult
    data class Error(val message: String) : LaunchResult
}
