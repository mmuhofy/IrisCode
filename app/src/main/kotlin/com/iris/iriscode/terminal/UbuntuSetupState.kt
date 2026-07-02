package com.iris.iriscode.terminal

sealed class UbuntuSetupState {
    data object Idle : UbuntuSetupState()
    data object Extracting : UbuntuSetupState()
    data object Configuring : UbuntuSetupState()
    data object Ready : UbuntuSetupState()
    data class Failed(val error: String) : UbuntuSetupState()
}
