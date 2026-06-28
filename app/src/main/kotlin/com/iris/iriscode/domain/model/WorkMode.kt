package com.iris.iriscode.domain.model

enum class WorkMode(val displayName: String) {
    PLAN("PLAN"),
    BUILD("BUILD"),
    AUTO("AUTO");

    companion object {
        val DEFAULT = BUILD
    }

    val canWriteFile: Boolean get() = this != PLAN
    val canRunBash: Boolean get() = this != PLAN
    val autoApprove: Boolean get() = this == AUTO
    val autoRunBash: Boolean get() = this == AUTO
}
