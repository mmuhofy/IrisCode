package com.iris.iriscode.terminal

sealed class UbuntuSetupState {
    data object Idle : UbuntuSetupState()
    data object Checking : UbuntuSetupState()
    data class DownloadingProot(val progress: Float) : UbuntuSetupState()
    data class DownloadingRootfs(val progress: Float) : UbuntuSetupState()
    data object Extracting : UbuntuSetupState()
    data object Configuring : UbuntuSetupState()
    data object Ready : UbuntuSetupState()
    data class Failed(val error: String) : UbuntuSetupState()
}
