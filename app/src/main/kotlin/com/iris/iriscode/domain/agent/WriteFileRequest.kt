package com.iris.iriscode.domain.agent

data class WriteFileRequest(
    val eventId: String,
    val path: String,
    val content: String,
    val existed: Boolean
)