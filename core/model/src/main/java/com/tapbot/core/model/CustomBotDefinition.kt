package com.tapbot.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CustomHttpMethod {
    GET,
    POST
}

/**
 * A single no-code command or trigger rule executed by [com.tapbot.core.runner.runtime.CustomWorkflowBotRuntime].
 */
@Serializable
data class CommandRule(
    val id: String = "",
    val command: String,
    val description: String = "",
    val responseTemplate: String,
    val webhookUrl: String? = null,
    val webhookMethod: CustomHttpMethod = CustomHttpMethod.GET,
    val webhookHeaders: Map<String, String> = emptyMap(),
    val webhookBodyTemplate: String? = null,
    val buttonLabel: String? = null,
    val buttonCallbackData: String? = null,
    val matchPrefix: Boolean = true
)

/**
 * Definition of a user-built no-code custom Telegram bot stored locally in TapBot.
 */
@Serializable
data class CustomBotDefinition(
    val id: String,
    val name: String,
    val summary: String,
    val description: String = summary,
    val category: String = "Custom",
    val welcomeMessage: String = "Hello {user}! Send /help to see all available commands.",
    val fallbackMessage: String? = null,
    val rules: List<CommandRule> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toBotMetadata(): BotMetadata {
        return BotMetadata(
            id = id,
            name = name,
            summary = summary,
            description = description.ifBlank { summary },
            author = "Custom Builder",
            version = "1.0.0",
            iconUrl = "",
            category = category,
            tags = listOf("custom", "no-code", "workflow"),
            requiredCredentials = listOf(BotCredentialSpec.TELEGRAM_BOT_TOKEN),
            packageInfo = BotPackageInfo(
                packageUrl = "local://custom/$id.botpkg",
                sha256Checksum = "custom_local",
                runtimeType = "built-in",
                packageSizeBytes = 1024L,
                entryClass = "com.tapbot.core.runner.runtime.CustomWorkflowBotRuntime"
            ),
            permissionsRequired = listOf("INTERNET", "FOREGROUND_SERVICE"),
            releaseNotes = "Created locally with TapBot No-Code Custom Bot Builder (${rules.size} command rule(s))."
        )
    }
}
