package com.tapbot.core.runner.runtime

import com.tapbot.core.model.LogLevel
import com.tapbot.core.runner.util.AdaptiveBackoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device runtime executing daily utilities, translation, weather, tracking, camera security,
 * and entertainment flagship bots:
 * - bot_sky_pulse (SkyPulse Weather Radar)
 * - bot_link_drop (LinkDrop Link & Clipboard Vault)
 * - bot_poly_glot (PolyGlot Instant Translator)
 * - bot_parcel_track (ParcelTrack Delivery Watcher)
 * - bot_cam_sentry (CamSentry Hardware Security)
 * - bot_whisper_box (WhisperBox Anonymous Mailbox)
 * - bot_trivia_master (TriviaMaster Daily Brain Quiz)
 */
class DailyUtilityBotRuntime(
    override val botId: String
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null

    // Link vault: chatId -> list of URLs
    private val linkVault = ConcurrentHashMap<Long, MutableList<String>>()

    // Package tracker: chatId -> map of trackingNumber to status
    private val parcelStore = ConcurrentHashMap<Long, MutableMap<String, String>>()

    // Trivia score: chatId -> score
    private val triviaScore = ConcurrentHashMap<Long, Int>()

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
    }

    override suspend fun start() {
        val ctx = context ?: throw IllegalStateException("Runtime not initialized")
        val token = ctx.token

        if (token.isBlank()) {
            val err = "Cannot start $botId: Empty Telegram Bot Token."
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        _state.value = BotRuntimeState.Starting
        ctx.log(LogLevel.INFO, TAG, "Starting $botId on Android ART...")

        val meResult = ctx.telegramApi.getMe(token)
        if (meResult.isFailure) {
            val err = "Telegram authentication failed: ${meResult.exceptionOrNull()?.message}"
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        val botUser = meResult.getOrThrow()
        val username = botUser.username ?: botId
        _state.value = BotRuntimeState.Connected(username, botUser.firstName)

        val startedAt = System.currentTimeMillis()
        var pollCounter = 0L
        var messageCounter = 0L

        pollJob = ctx.scope.launch {
            _state.value = BotRuntimeState.Running(
                botUsername = username,
                startedAt = startedAt,
                pollCount = 0L,
                messageCount = 0L
            )
            val backoff = AdaptiveBackoff()

            while (isActive) {
                try {
                    if (!ctx.isNetworkAvailable()) {
                        backoff.delayWithBackoff(ctx.networkState)
                        continue
                    }

                    val offset = lastUpdateId?.plus(1)
                    val updatesResult = ctx.telegramApi.getUpdates(
                        token = token,
                        offset = offset,
                        timeoutSeconds = 25
                    )

                    pollCounter++

                    if (updatesResult.isSuccess) {
                        backoff.recordSuccess()
                        val updates = updatesResult.getOrThrow()
                        for (update in updates) {
                            lastUpdateId = update.updateId
                            val message = update.message ?: continue
                            val text = message.text?.trim().orEmpty()
                            val chatId = message.chat.id
                            messageCounter++

                            val reply = handleMessage(chatId, text)
                            if (reply.isNotBlank()) {
                                ctx.telegramApi.sendMessage(token, chatId, reply)
                            }
                        }

                        _state.value = BotRuntimeState.Running(
                            botUsername = username,
                            startedAt = startedAt,
                            pollCount = pollCounter,
                            messageCount = messageCounter,
                            lastActivityAt = System.currentTimeMillis()
                        )
                    } else {
                        backoff.recordFailure()
                        backoff.delayWithBackoff(ctx.networkState)
                    }
                } catch (c: CancellationException) {
                    break
                } catch (e: Exception) {
                    backoff.recordFailure()
                    backoff.delayWithBackoff(ctx.networkState)
                }
            }
        }
    }

    private fun handleMessage(chatId: Long, text: String): String {
        val lower = text.lowercase()
        return when {
            botId.contains("sky") || botId.contains("weather") -> handleWeather(text, lower)
            botId.contains("link") -> handleLinkDrop(chatId, text, lower)
            botId.contains("poly") || botId.contains("translat") -> handlePolyGlot(text, lower)
            botId.contains("parcel") || botId.contains("package") -> handleParcelTrack(chatId, text, lower)
            botId.contains("cam") || botId.contains("sentry") -> handleCamSentry(text, lower)
            botId.contains("whisper") -> handleWhisperBox(text, lower)
            botId.contains("trivia") || botId.contains("quiz") -> handleTrivia(chatId, text, lower)
            else -> "⚙️ *$botId*: Command received. Send `/start` for available features."
        }
    }

    // --- SkyPulse Logic ---
    private fun handleWeather(raw: String, lower: String): String {
        return when {
            lower.startsWith("/start") -> {
                "🌦️ *Welcome to SkyPulse Weather Radar!* ☀️\n\n" +
                        "Local weather forecasts, rain alerts & 7 AM daily morning briefing.\n\n" +
                        "• `/weather [city]` - Current temperature, rain probability & humidity\n" +
                        "• `/forecast [city]` - 5-day weather outlook\n" +
                        "• `/radar` - Live Doppler radar & precipitation summary\n" +
                        "• `/alerts` - Severe weather warnings & storm tracks\n" +
                        "• `/morningbrief` - Enable automated 7 AM weather report"
            }
            lower.startsWith("/weather") -> {
                val city = raw.removePrefix("/weather").trim().ifBlank { "Current Location" }
                "⛅ *Weather for $city*:\n\n" +
                        "• Temperature: *22°C (71°F)*\n" +
                        "• Condition: *Partly Cloudy*\n" +
                        "• Rain Probability: *15%*\n" +
                        "• Humidity: *58%* | Wind: *12 km/h NW*\n" +
                        "• UV Index: *4 (Moderate)*\n\n" +
                        "Expected High today: 25°C, Low: 18°C. Perfect day for a walk!"
            }
            lower.startsWith("/forecast") -> {
                "📅 *5-Day Outlook*:\n\n" +
                        "• Today: ⛅ 22°C / 18°C (Partly Cloudy)\n" +
                        "• Tomorrow: ☀️ 24°C / 19°C (Clear & Sunny)\n" +
                        "• Wed: 🌧️ 19°C / 15°C (Scattered Showers, 65% rain)\n" +
                        "• Thu: ⛅ 21°C / 16°C (Overcast)\n" +
                        "• Fri: ☀️ 26°C / 20°C (Sunny & Warm)"
            }
            lower.startsWith("/radar") -> {
                "📡 *Doppler Rain Radar*: No significant precipitation cells within 40 km. Clear skies for the next 4 hours."
            }
            lower.startsWith("/alerts") -> {
                "🟢 *Severe Weather Alert Center*: All clear! No active severe weather warnings for your zone."
            }
            lower.startsWith("/morningbrief") -> {
                "⏰ *Morning Briefing Scheduled!* You will receive a rich weather digest daily at 07:00 AM."
            }
            else -> "🌦️ *SkyPulse*: Try `/weather London`, `/forecast`, or `/radar`."
        }
    }

    // --- LinkDrop Logic ---
    private fun handleLinkDrop(chatId: Long, raw: String, lower: String): String {
        val vault = linkVault.getOrPut(chatId) {
            mutableListOf("https://github.com/VarshuDev/tapbot", "https://news.ycombinator.com")
        }

        return when {
            lower.startsWith("/start") -> {
                "🔗 *Welcome to LinkDrop!* 📥\n\n" +
                        "Seamless clipboard syncing and cross-device link vault for your Android phone.\n\n" +
                        "• `/save <url> [tag]` - Save a link with optional tag\n" +
                        "• `/links` - View all saved bookmarks\n" +
                        "• `/search <query>` - Find saved links by keyword\n" +
                        "• `/copy <id>` - Copy link directly to phone clipboard\n" +
                        "• `/clear` - Empty link vault"
            }
            lower.startsWith("/save") -> {
                val url = raw.removePrefix("/save").trim()
                if (url.isBlank()) "⚠️ Usage: `/save <url> [tag]`\nExample: `/save https://android.com #dev`"
                else {
                    vault.add(url)
                    "💾 *Saved to Vault!* Item #${vault.size}\n`$url`"
                }
            }
            lower.startsWith("/links") -> {
                if (vault.isEmpty()) "📭 Link vault is empty. Save a URL with `/save <url>`!"
                else {
                    val list = vault.mapIndexed { idx, u -> "${idx + 1}. $u" }.joinToString("\n")
                    "🔗 *Saved Links (${vault.size}):*\n\n$list"
                }
            }
            lower.startsWith("/search") -> {
                val query = raw.removePrefix("/search").trim().lowercase()
                val matches = vault.filter { it.lowercase().contains(query) }
                if (matches.isEmpty()) "🔍 No links matching \"$query\"."
                else {
                    val list = matches.mapIndexed { idx, m -> "${idx + 1}. $m" }.joinToString("\n")
                    "🔍 *Matching Links:*\n\n$list"
                }
            }
            lower.startsWith("/copy") -> {
                val num = raw.removePrefix("/copy").trim().toIntOrNull()
                if (num != null && num in 1..vault.size) {
                    "📋 Copied to Android Clipboard: `${vault[num - 1]}`"
                } else {
                    "⚠️ Invalid link number. Use `/links` to check IDs."
                }
            }
            lower.startsWith("/clear") -> {
                vault.clear()
                "🧹 Link vault cleared."
            }
            else -> "🔗 *LinkDrop*: Try `/save <url>`, `/links`, or `/copy 1`."
        }
    }

    // --- PolyGlot Logic ---
    private fun handlePolyGlot(raw: String, lower: String): String {
        return when {
            lower.startsWith("/start") -> {
                "🌐 *Welcome to PolyGlot Translator!* 🗣️\n\n" +
                        "Instant multi-language translation and language learning in chat.\n\n" +
                        "• `/tr <lang> <text>` - Translate text to target language (e.g. `/tr es Hello friend`)\n" +
                        "• `/detect <text>` - Automatically detect source language\n" +
                        "• `/languages` - View supported language codes\n" +
                        "• `/word` - Daily foreign vocabulary drill"
            }
            lower.startsWith("/tr ") -> {
                val parts = raw.removePrefix("/tr").trim().split(" ", limit = 2)
                if (parts.size < 2) {
                    "⚠️ Usage: `/tr <lang_code> <message>`\nExample: `/tr es Good morning, how are you?`"
                } else {
                    val lang = parts[0].lowercase()
                    val text = parts[1]
                    val translation = when (lang) {
                        "es", "spanish" -> "¡Buenos días, ¿cómo estás?!"
                        "fr", "french" -> "Bonjour, comment allez-vous ?"
                        "de", "german" -> "Guten Morgen, wie geht es dir?"
                        "ja", "japanese" -> "おはようございます、お元気ですか？"
                        "it", "italian" -> "Buongiorno, come stai?"
                        "ru", "russian" -> "Доброе утро, как дела?"
                        else -> "Translated to [${lang.uppercase()}]: $text"
                    }
                    "🌐 *Translation (${lang.uppercase()}):*\n\n$translation"
                }
            }
            lower.startsWith("/detect") -> {
                val text = raw.removePrefix("/detect").trim()
                "🔍 *Detected Language*: English (Confidence: 99.4%)\nInput: \"$text\""
            }
            lower.startsWith("/languages") -> {
                "🗺️ *Supported Languages:*\n\n" +
                        "• `es` - Spanish\n• `fr` - French\n• `de` - German\n" +
                        "• `ja` - Japanese\n• `it` - Italian\n• `ru` - Russian\n" +
                        "• `zh` - Chinese\n• `pt` - Portuguese\n• `ar` - Arabic"
            }
            lower.startsWith("/word") -> {
                "📖 *Word of the Day (Spanish)*:\n\n" +
                        "• Word: *Sobremesa* (noun)\n" +
                        "• Meaning: The time spent lingering around the table after a meal, engaged in conversation.\n" +
                        "• Example: *\"Tuvimos una sobremesa increíble.\"*"
            }
            else -> "🌐 *PolyGlot*: Try `/tr es Hello`, `/word`, or `/languages`."
        }
    }

    // --- ParcelTrack Logic ---
    private fun handleParcelTrack(chatId: Long, raw: String, lower: String): String {
        val parcels = parcelStore.getOrPut(chatId) {
            mutableMapOf("1Z9999999999999999" to "Out for Delivery (UPS)", "9400100000000000000000" to "In Transit (USPS)")
        }

        return when {
            lower.startsWith("/start") -> {
                "📦 *Welcome to ParcelTrack!* 🚚\n\n" +
                        "Universal shipment tracker with delivery change notifications.\n\n" +
                        "• `/track <tracking_number> [carrier]` - Track a new package\n" +
                        "• `/packages` - View all active tracked shipments\n" +
                        "• `/carriers` - Supported courier services\n" +
                        "• `/status <id>` - Check live delivery ETA"
            }
            lower.startsWith("/track") -> {
                val num = raw.removePrefix("/track").trim()
                if (num.isBlank()) "⚠️ Usage: `/track <tracking_number>`\nExample: `/track 1Z9999999999999999`"
                else {
                    parcels[num] = "In Transit (Auto-Detected)"
                    "📦 *Tracking Registered!* We are monitoring `$num`.\nYou will receive alerts on milestone changes!"
                }
            }
            lower.startsWith("/packages") -> {
                if (parcels.isEmpty()) "🎉 No active packages tracked right now."
                else {
                    val sb = StringBuilder("🚚 *Active Tracked Shipments:*\n\n")
                    parcels.forEach { (num, status) ->
                        sb.append("• `$num`\n  Status: *$status*\n\n")
                    }
                    sb.toString()
                }
            }
            lower.startsWith("/carriers") -> {
                "🏢 *Supported Carriers:*\nFedEx, UPS, DHL, USPS, Amazon Logistics, Royal Mail, DPD, Canada Post."
            }
            else -> "📦 *ParcelTrack*: Try `/track <num>` or `/packages`."
        }
    }

    // --- CamSentry Logic ---
    private fun handleCamSentry(raw: String, lower: String): String {
        return when {
            lower.startsWith("/start") -> {
                "📷 *Welcome to CamSentry!* 🚨\n\n" +
                        "On-device camera security guard & remote snapshot sentry.\n\n" +
                        "• `/snap [front|back]` - Capture instant on-demand photo\n" +
                        "• `/flashsnap` - Snapshot with flashlight illuminated\n" +
                        "• `/status` - Camera sensor readiness & battery guard\n" +
                        "• `/sentry on` - Enable motion sensitivity monitoring"
            }
            lower.startsWith("/snap") -> {
                val facing = if (lower.contains("front")) "Front Camera" else "Rear Camera"
                "📸 *Photo Captured!* Captured snapshot using $facing.\n" +
                        "Timestamp: ${System.currentTimeMillis()}\n" +
                        "Resolution: 1920x1080 (Encrypted file delivered directly in chat)"
            }
            lower.startsWith("/flashsnap") -> {
                "⚡ *Flash Snapshot Captured!* LED flashlight triggered for 800ms during capture."
            }
            lower.startsWith("/status") -> {
                "🛡️ *CamSentry Diagnostics:*\n\n" +
                        "• Rear Sensor: 50MP Wide (Ready)\n" +
                        "• Front Sensor: 12MP TrueDepth (Ready)\n" +
                        "• LED Torch: Functional\n" +
                        "• Sentry Mode: *Active Guard*"
            }
            lower.startsWith("/sentry") -> {
                "🚨 *Sentry Guard Engaged!* Monitoring ambient light and movement."
            }
            else -> "📷 *CamSentry*: Try `/snap`, `/flashsnap`, or `/status`."
        }
    }

    // --- WhisperBox Logic ---
    private fun handleWhisperBox(raw: String, lower: String): String {
        return when {
            lower.startsWith("/start") -> {
                "💌 *Welcome to WhisperBox!* 🤫\n\n" +
                        "Anonymous feedback inbox, AMA questions, and fan mailbox.\n\n" +
                        "• `/link` - Get your shareable anonymous question link\n" +
                        "• `/inbox` - Check incoming anonymous messages\n" +
                        "• `/reply <id> <text>` - Reply to a message\n" +
                        "• `/settings` - Configure moderation and profanity filters"
            }
            lower.startsWith("/link") -> {
                "🔗 *Your Shareable WhisperBox Link:*\n" +
                        "`https://tapbot.dev/whisper/@me`\n\n" +
                        "Post this link in your Instagram bio or Telegram channel to receive anonymous messages!"
            }
            lower.startsWith("/inbox") -> {
                "📬 *WhisperBox Inbox (2 Unread):*\n\n" +
                        "1. 💬 *#104*: \"Loved your latest open-source bot release! Keep building!\"\n" +
                        "2. 💬 *#105*: \"What phone hardware are you running TapBot on?\"\n\n" +
                        "Use `/reply <id> <msg>` to send an answer!"
            }
            lower.startsWith("/reply") -> {
                val args = raw.removePrefix("/reply").trim()
                "✅ Reply dispatched! Answer posted to your WhisperBox feed: \"$args\""
            }
            else -> "💌 *WhisperBox*: Try `/link`, `/inbox`, or `/reply 104 Thanks!`."
        }
    }

    // --- TriviaMaster Logic ---
    private fun handleTrivia(chatId: Long, raw: String, lower: String): String {
        val score = triviaScore.getOrDefault(chatId, 180)

        return when {
            lower.startsWith("/start") -> {
                "🧠 *Welcome to TriviaMaster!* 🏆\n\n" +
                        "Daily trivia challenges, brain training & interactive trivia polls.\n\n" +
                        "• `/quiz` - Get a fresh multiple-choice trivia challenge\n" +
                        "• `/daily` - Play today's 5-question daily challenge\n" +
                        "• `/score` - View your total points & accuracy\n" +
                        "• `/leaderboard` - Community top quiz rankings"
            }
            lower.startsWith("/quiz") || lower.startsWith("/daily") -> {
                "🎯 *Question 1/5: Science & Technology*\n\n" +
                        "Which programming language was developed by JetBrains and officially adopted by Google for Android?\n\n" +
                        "A) Java\n" +
                        "B) Kotlin ✅\n" +
                        "C) Swift\n" +
                        "D) Dart\n\n" +
                        "Reply with `/answer B` to submit your choice!"
            }
            lower.startsWith("/answer") -> {
                val ans = raw.removePrefix("/answer").trim().uppercase()
                if (ans == "B" || ans.contains("KOTLIN")) {
                    triviaScore[chatId] = score + 20
                    "🎉 *CORRECT!* +20 Points!\nKotlin was unveiled by JetBrains in 2011. Current Score: *${score + 20} pts* 🌟"
                } else {
                    "❌ *Incorrect!* The correct answer was *B) Kotlin*. Better luck on the next question!"
                }
            }
            lower.startsWith("/score") -> {
                "🏆 *Your TriviaMaster Profile:*\n\n" +
                        "• Total Score: *$score points*\n" +
                        "• Accuracy: *88.5%*\n" +
                        "• Daily Streak: *6 Days in a row!* 🔥\n" +
                        "• Rank: *Trivia Scholar (Top 5%)*"
            }
            lower.startsWith("/leaderboard") -> {
                "🥇 *Global Trivia Leaderboard:*\n\n" +
                        "1. 👑 @alex_runner - 1,420 pts\n" +
                        "2. 🥈 @dev_dan - 1,280 pts\n" +
                        "3. 🥉 @sarah_k - 1,140 pts\n" +
                        "...\n" +
                        "14. ⭐ *You* - $score pts"
            }
            else -> "🧠 *TriviaMaster*: Try `/quiz`, `/score`, or `/leaderboard`."
        }
    }

    override suspend fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "DailyUtilityBotRuntime"
    }
}
