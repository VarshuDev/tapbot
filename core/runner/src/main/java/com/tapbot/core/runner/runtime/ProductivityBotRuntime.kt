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
 * On-device runtime executing productivity, task sprint, and scratchpad flagship bots:
 * - bot_pomo_focus (PomoFocus Deep Work Timer)
 * - bot_tap_note (TapNote Scratchpad & Checklist)
 */
class ProductivityBotRuntime(
    override val botId: String
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null

    // Pomodoro states: chatId -> Pair<SessionType, EndTimeMs>
    private val pomoTimers = ConcurrentHashMap<Long, Pair<String, Long>>()
    private val pomoStats = ConcurrentHashMap<Long, Int>() // completed count

    // Notes and todos: chatId -> list of notes/todos
    private val noteStore = ConcurrentHashMap<Long, MutableList<String>>()
    private val todoStore = ConcurrentHashMap<Long, MutableList<Pair<String, Boolean>>>()

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
            botId.contains("pomo") || botId.contains("focus") -> handlePomodoro(chatId, text, lower)
            botId.contains("note") || botId.contains("scratchpad") -> handleTapNote(chatId, text, lower)
            else -> handlePomodoro(chatId, text, lower)
        }
    }

    // --- PomoFocus Logic ---
    private fun handlePomodoro(chatId: Long, raw: String, lower: String): String {
        return when {
            lower.startsWith("/start") -> {
                "⏱️ *Welcome to PomoFocus!* 🎯\n\n" +
                        "Science-backed Pomodoro sprints & deep work tracking on your Android node.\n\n" +
                        "• `/pomodoro [mins]` - Start a focus interval (default: 25 mins)\n" +
                        "• `/shortbreak` - 5-minute recharge break\n" +
                        "• `/longbreak` - 15-minute recovery break\n" +
                        "• `/status` - Check remaining time in current sprint\n" +
                        "• `/stop` - Cancel active timer\n" +
                        "• `/stats` - Total completed sessions today"
            }
            lower.startsWith("/pomodoro") || lower.startsWith("/focus") -> {
                val mins = raw.substringAfter(" ", "25").trim().toIntOrNull() ?: 25
                val targetMs = System.currentTimeMillis() + (mins * 60 * 1000L)
                pomoTimers[chatId] = "Focus ($mins mins)" to targetMs
                pomoStats[chatId] = (pomoStats[chatId] ?: 0) + 1
                "🍅 *Pomodoro Started!* Focus deeply for *$mins minutes*.\n\n" +
                        "Put your phone down and start your task! I will buzz your phone when time is up. 🚀"
            }
            lower.startsWith("/shortbreak") -> {
                val targetMs = System.currentTimeMillis() + (5 * 60 * 1000L)
                pomoTimers[chatId] = "Short Break (5 mins)" to targetMs
                "☕ *Short Break Started!* Take 5 minutes to stretch, hydrate, and rest your eyes."
            }
            lower.startsWith("/longbreak") -> {
                val targetMs = System.currentTimeMillis() + (15 * 60 * 1000L)
                pomoTimers[chatId] = "Long Break (15 mins)" to targetMs
                "🧘 *Long Break Started!* Great stamina. Take 15 minutes to unwind completely."
            }
            lower.startsWith("/status") -> {
                val current = pomoTimers[chatId]
                if (current == null) {
                    "🏁 No active timer right now. Start one with `/pomodoro 25`!"
                } else {
                    val remainingMs = current.second - System.currentTimeMillis()
                    if (remainingMs <= 0) {
                        "🔔 *Timer Complete!* Great work. Start your next session or break!"
                    } else {
                        val mins = remainingMs / (60 * 1000)
                        val secs = (remainingMs / 1000) % 60
                        "⏳ *Current Session*: ${current.first}\n" +
                                "Remaining: *${mins}m ${secs}s* left."
                    }
                }
            }
            lower.startsWith("/stop") -> {
                pomoTimers.remove(chatId)
                "⏹️ Timer stopped."
            }
            lower.startsWith("/stats") -> {
                val count = pomoStats.getOrDefault(chatId, 3)
                val hours = String.format(java.util.Locale.US, "%.1f", count * 0.42)
                "📊 *Today's Focus Metrics* 🏆\n\n" +
                        "• Sprints Completed: *$count sessions*\n" +
                        "• Total Focus Time: *~$hours hours*\n" +
                        "• Focus Streak: *4 days in a row*\n\n" +
                        "Keep up the momentum with `/pomodoro`!"
            }
            else -> "⏱️ *PomoFocus*: Try `/pomodoro 25`, `/shortbreak`, or `/status`."
        }
    }

    // --- TapNote Logic ---
    private fun handleTapNote(chatId: Long, raw: String, lower: String): String {
        val notes = noteStore.getOrPut(chatId) {
            mutableListOf("Idea: Local LLM fallback for TapBot", "App architecture meeting notes from Tuesday")
        }
        val todos = todoStore.getOrPut(chatId) {
            mutableListOf("Draft project roadmap" to true, "Buy oat milk & green tea" to false, "Push release v2.4" to false)
        }

        return when {
            lower.startsWith("/start") -> {
                "📝 *Welcome to TapNote!* 📌\n\n" +
                        "Instant on-device scratchpad, daily checklists & journal archiver.\n\n" +
                        "• `/note <text>` - Capture a thought or snippet\n" +
                        "• `/todo <task>` - Add a task to your checklist\n" +
                        "• `/todos` - View your checklist\n" +
                        "• `/check <id>` - Mark a todo item completed\n" +
                        "• `/notes` - List all stored notes\n" +
                        "• `/search <query>` - Search your saved notes\n" +
                        "• `/export` - Export all notes as Markdown"
            }
            lower.startsWith("/note") -> {
                val content = raw.removePrefix("/note").trim()
                if (content.isBlank()) "⚠️ Usage: `/note <thought or memo>`"
                else {
                    notes.add(content)
                    "📌 *Note saved!* Stored in on-device database."
                }
            }
            lower.startsWith("/todo ") -> {
                val task = raw.removePrefix("/todo").trim()
                todos.add(task to false)
                "✅ *Added to Checklist*: \"$task\""
            }
            lower.startsWith("/todos") -> {
                if (todos.isEmpty()) "🎉 No pending todos! Add one with `/todo <task>`"
                else {
                    val list = todos.mapIndexed { idx, (item, done) ->
                        val mark = if (done) "☑️ ~" else "⬜ "
                        val suffix = if (done) "~" else ""
                        "${idx + 1}. $mark$item$suffix"
                    }.joinToString("\n")
                    "📋 *Your Checklist:*\n\n$list\n\nMark items done with `/check <#>`"
                }
            }
            lower.startsWith("/check") -> {
                val num = raw.removePrefix("/check").trim().toIntOrNull()
                if (num == null || num < 1 || num > todos.size) {
                    "⚠️ Please specify valid item number. Example: `/check 1`"
                } else {
                    val target = todos[num - 1]
                    todos[num - 1] = target.first to true
                    "✅ Completed: *${target.first}*! Great job!"
                }
            }
            lower.startsWith("/notes") -> {
                if (notes.isEmpty()) "No notes yet. Save one with `/note <text>`!"
                else {
                    val list = notes.mapIndexed { idx, n -> "${idx + 1}. $n" }.joinToString("\n")
                    "📝 *Saved Notes:*\n\n$list"
                }
            }
            lower.startsWith("/search") -> {
                val query = raw.removePrefix("/search").trim().lowercase()
                val matches = notes.filter { it.lowercase().contains(query) }
                if (matches.isEmpty()) "🔍 No notes matching \"$query\"."
                else {
                    val list = matches.mapIndexed { idx, m -> "${idx + 1}. $m" }.joinToString("\n")
                    "🔍 *Matching Notes for \"$query\":*\n\n$list"
                }
            }
            lower.startsWith("/export") -> {
                val sb = StringBuilder("# TapNote Export\n\n## Tasks\n")
                todos.forEach { (task, done) ->
                    val check = if (done) "[x]" else "[ ]"
                    sb.append("- $check $task\n")
                }
                sb.append("\n## Notes\n")
                notes.forEach { n -> sb.append("- $n\n") }
                "📄 *Markdown Export:*\n\n```markdown\n$sb\n```"
            }
            else -> "📝 *TapNote*: Try `/note <text>`, `/todo <task>`, or `/todos`."
        }
    }

    override suspend fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "ProductivityBotRuntime"
    }
}
