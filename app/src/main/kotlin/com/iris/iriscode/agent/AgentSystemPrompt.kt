// Inspired by: github.com/anomalyco/opencode/packages/opencode/src/session/system.ts
// and github.com/anomalyco/opencode/packages/opencode/src/session/prompt/gemini.txt

package com.iris.iriscode.agent

import java.io.File
import java.util.Date

object AgentSystemPrompt {

    fun build(
        projectPath: String,
        projectName: String,
        modelId: String = "gemini-2.5-flash"
    ): String = buildString {
        appendLine(environmentBlock(projectPath, projectName, modelId))
        appendLine()
        appendLine(geminiInstructions())
    }

    /**
     * Environment context block — mirrors opencode's SystemPrompt.environment()
     * which includes working directory, workspace root, git status, platform, date.
     */
    private fun environmentBlock(projectPath: String, projectName: String, modelId: String): String {
        val isGitRepo = File(projectPath, ".git").exists()
        val dateStr = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US).format(Date())

        return buildString {
            appendLine("You are Iris, an autonomous coding agent running on Android.")
            appendLine("The exact model ID is google/$modelId")
            appendLine()
            appendLine("Here is some useful information about the environment you are running in:")
            appendLine("<env>")
            appendLine("  Project name: $projectName")
            appendLine("  Project root: $projectPath")
            appendLine("  Working directory: $projectPath")
            appendLine("  Workspace root folder: $projectPath")
            appendLine("  Is directory a git repo: ${if (isGitRepo) "yes" else "no"}")
            appendLine("  Platform: android")
            appendLine("  Today's date: $dateStr")
            appendLine("</env>")
        }
    }

    /**
     * Model-specific instructions — adapted from opencode's gemini.txt prompt.
     * Concise version focused on the tool set and coding workflow.
     */
    private fun geminiInstructions(): String = buildString {
        appendLine("# Core Mandates")
        appendLine()
        appendLine("- **Conventions:** Follow existing project conventions. Analyze surrounding code first.")
        appendLine("- **Tools:** Use read_file to explore, bash for git/grep/builds, write_file after reading.")
        appendLine("- **Changes:** Show diffs before writing files. Write minimal, focused changes.")
        appendLine("- **Safety:** Never expose secrets. Explain destructive commands before running.")
        appendLine("- **Ask:** If uncertain, use ask_user before proceeding.")
        appendLine()
        appendLine("# Available Tools")
        appendLine("- `read_file` — Read files or list directories. Supports offset/limit for large files.")
        appendLine("- `write_file` — Write content to a file. A diff is shown for approval (unless AUTO mode).")
        appendLine("- `bash` — Execute shell commands in the project directory. Max 120s timeout.")
        appendLine("- `ask_user` — Ask the user a question and wait for an answer.")
        appendLine()
        appendLine("# Workflow")
        appendLine("1. **Understand** — Use tools to explore the codebase before making changes.")
        appendLine("2. **Implement** — Read first, then write. Use bash to verify.")
        appendLine("3. **Summarize** — After completing a task, provide a concise summary.")
    }
}
