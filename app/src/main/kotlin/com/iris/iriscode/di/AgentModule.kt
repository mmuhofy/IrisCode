// UNTESTED — verify before use

package com.iris.iriscode.di

import com.iris.iriscode.agent.AgentLoop
import com.iris.iriscode.agent.ToolRegistry
import com.iris.iriscode.agent.tool.AskUserTool
import com.iris.iriscode.agent.tool.BashTool
import com.iris.iriscode.agent.tool.ReadFileTool
import com.iris.iriscode.agent.tool.WriteFileTool
import com.iris.iriscode.data.remote.gemini.GeminiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides @Singleton fun provideReadFileTool() = ReadFileTool()
    @Provides @Singleton fun provideWriteFileTool() = WriteFileTool()
    @Provides @Singleton fun provideBashTool() = BashTool()
    @Provides @Singleton fun provideAskUserTool() = AskUserTool()

    @Provides
    @Singleton
    fun provideToolRegistry(
        readFileTool: ReadFileTool,
        writeFileTool: WriteFileTool,
        bashTool: BashTool,
        askUserTool: AskUserTool
    ): ToolRegistry = ToolRegistry(readFileTool, writeFileTool, bashTool, askUserTool)

    @Provides
    @Singleton
    fun provideAgentLoop(
        geminiClient: GeminiClient,
        toolRegistry: ToolRegistry
    ): AgentLoop = AgentLoop(geminiClient, toolRegistry)
}