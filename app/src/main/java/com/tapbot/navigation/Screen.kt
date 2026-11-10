package com.tapbot.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Catalog : Screen("catalog")

    object BotDetail : Screen("bot_detail/{botId}") {
        fun createRoute(botId: String) = "bot_detail/${Uri.encode(botId)}"
    }

    object CustomBotBuilder : Screen("custom_bot_builder?editBotId={editBotId}") {
        const val ARG_EDIT_BOT_ID = "editBotId"

        fun createRoute(editBotId: String? = null): String =
            if (editBotId.isNullOrBlank()) {
                "custom_bot_builder"
            } else {
                "custom_bot_builder?editBotId=${Uri.encode(editBotId)}"
            }
    }

    object Poc : Screen("poc")
}
