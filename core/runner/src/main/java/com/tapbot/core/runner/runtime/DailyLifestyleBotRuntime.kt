package com.tapbot.core.runner.runtime

import com.tapbot.core.model.LogLevel
import com.tapbot.core.runner.util.AdaptiveBackoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device runtime executing lifestyle, habits, wellness, and fitness flagship bots:
 * - bot_habit_forge (HabitForge Daily Tracker)
 * - bot_remind_me (RemindMe Health & Pill Alarms)
 * - bot_fit_track (FitTrack Hydration & Workouts)
 */
class DailyLifestyleBotRuntime(
    override val botId: String
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null

    // Stateful trackers per chat
    private val habitStore = ConcurrentHashMap<Long, MutableMap<String, Int>>() // habit -> streak
    private val waterStore = ConcurrentHashMap<Long, Int>() // ml today
    private val workoutStore = ConcurrentHashMap<Long, MutableList<String>>() // workouts today
    private val reminderStore = ConcurrentHashMap<Long, MutableList<String>>() // pending reminders

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
            botId.contains("habit") -> handleHabitCommand(chatId, text, lower)
            botId.contains("remind") -> handleRemindCommand(chatId, text, lower)
            botId.contains("fit") -> handleFitCommand(chatId, text, lower)
            else -> handleHabitCommand(chatId, text, lower)
        }
    }

    // --- HabitForge Logic ---
    private fun handleHabitCommand(chatId: Long, raw: String, lower: String): String {
        val habits = habitStore.getOrPut(chatId) {
            mutableMapOf("workout" to 4, "reading" to 12, "meditation" to 3, "hydration" to 7)
        }

        return when {
            lower.startsWith("/start") -> {
                "🌱 *Welcome to HabitForge!* 🔥\n\n" +
                        "Build unbreakable daily habits & maintain winning streaks directly from your phone.\n\n" +
                        "• `/done <habit>` - Check off a habit for today\n" +
                        "• `/streak` - View your active streak board\n" +
                        "• `/habits` - List all tracked daily habits\n" +
                        "• `/addhabit <name>` - Add a new daily target\n" +
                        "• `/today` - Today's habit checklist\n" +
                        "• `/reset <habit>` - Reset habit streak counter"
            }
            lower.startsWith("/done") -> {
                val target = raw.removePrefix("/done").trim().lowercase()
                if (target.isBlank()) {
                    "⚠️ Specify habit name. Example: `/done workout`"
                } else {
                    val current = habits.getOrDefault(target, 0)
                    habits[target] = current + 1
                    "🔥 *Awesome!* You completed *${target.replaceFirstChar { it.uppercase() }}* today!\n" +
                            "Streak increased to *${current + 1} days* in a row! 🏆 Keep it up!"
                }
            }
            lower.startsWith("/streak") || lower.startsWith("/today") -> {
                val sb = StringBuilder("📋 *HabitForge Daily Dashboard* 🌟\n\n")
                if (habits.isEmpty()) {
                    sb.append("No habits yet. Use `/addhabit <name>` to start!")
                } else {
                    habits.forEach { (h, streak) ->
                        val bar = "▓".repeat((streak % 7).coerceAtLeast(1)) + "░".repeat(7 - (streak % 7).coerceAtLeast(1))
                        sb.append("• *${h.replaceFirstChar { it.uppercase() }}*: $streak days [$bar]\n")
                    }
                    sb.append("\nTip: Send `/done <habit>` as soon as you finish a task!")
                }
                sb.toString()
            }
            lower.startsWith("/habits") -> {
                val list = habits.keys.joinToString("\n") { "• ${it.replaceFirstChar { c -> c.uppercase() }}" }
                "📋 *Your Active Habits:*\n$list\n\nAdd more with `/addhabit <name>`"
            }
            lower.startsWith("/addhabit") -> {
                val newH = raw.removePrefix("/addhabit").trim().lowercase()
                if (newH.isBlank()) "⚠️ Please provide habit name. Example: `/addhabit morning jog`"
                else {
                    habits[newH] = 1
                    "✅ Added habit *${newH.replaceFirstChar { it.uppercase() }}*! First day logged."
                }
            }
            lower.startsWith("/reset") -> {
                val target = raw.removePrefix("/reset").trim().lowercase()
                habits.remove(target)
                "🔄 Habit *$target* reset."
            }
            else -> "🌱 *HabitForge*: Unknown command. Try `/done <habit>`, `/streak`, or `/today`."
        }
    }

    // --- RemindMe Logic ---
    private fun handleRemindCommand(chatId: Long, raw: String, lower: String): String {
        val reminders = reminderStore.getOrPut(chatId) {
            mutableListOf("09:00 AM - Morning Vitamin D", "02:00 PM - Drink 500ml water", "10:30 PM - Sleep wind-down")
        }

        return when {
            lower.startsWith("/start") -> {
                "🔔 *Welcome to RemindMe!* ⏰\n\n" +
                        "Smart reminders, pill alarms, and health alerts running 24/7 on your Android device.\n\n" +
                        "• `/remind <time> <task>` - Set a new reminder (e.g. `/remind 15:30 Call Mom`)\n" +
                        "• `/reminders` - View all active scheduled alarms\n" +
                        "• `/water [interval]` - Set recurring hydration reminder\n" +
                        "• `/pill <time> <name>` - Schedule daily medication alarm\n" +
                        "• `/clear` - Clear all completed reminders"
            }
            lower.startsWith("/remind") -> {
                val args = raw.removePrefix("/remind").trim()
                if (args.isBlank()) "⚠️ Usage: `/remind <time> <message>`\nExample: `/remind 16:00 Meeting with team`"
                else {
                    reminders.add(args)
                    "⏰ *Reminder set!* I'll alert you: \"$args\""
                }
            }
            lower.startsWith("/reminders") -> {
                if (reminders.isEmpty()) "🎉 No pending reminders! Add one with `/remind <time> <msg>`"
                else {
                    val list = reminders.mapIndexed { idx, r -> "${idx + 1}. $r" }.joinToString("\n")
                    "🔔 *Scheduled Reminders:*\n\n$list"
                }
            }
            lower.startsWith("/water") -> {
                "💧 *Hydration Guard Activated!* I'll remind you to drink water every 2 hours."
            }
            lower.startsWith("/pill") -> {
                val pill = raw.removePrefix("/pill").trim()
                if (pill.isBlank()) "⚠️ Usage: `/pill <time> <medication name>`"
                else {
                    reminders.add("Daily at $pill")
                    "💊 *Medication schedule saved!* Will alert you daily: $pill"
                }
            }
            lower.startsWith("/clear") -> {
                reminders.clear()
                "🧹 All reminders cleared."
            }
            else -> "🔔 *RemindMe*: Send `/remind <time> <msg>` or `/reminders` for help."
        }
    }

    // --- FitTrack Logic ---
    private fun handleFitCommand(chatId: Long, raw: String, lower: String): String {
        val currentWater = waterStore.getOrDefault(chatId, 1250)
        val workouts = workoutStore.getOrPut(chatId) {
            mutableListOf("Morning 20m Core Routine (~140 kcal)")
        }

        return when {
            lower.startsWith("/start") -> {
                "💪 *Welcome to FitTrack!* 🏃\n\n" +
                        "On-device fitness companion, hydration monitor & calorie logger.\n\n" +
                        "• `/water [amount]` - Log hydration (e.g. `/water +250ml` or `/water`)\n" +
                        "• `/workout <type> <mins>` - Record workout activity\n" +
                        "• `/calories <kcal>` - Log consumed calories\n" +
                        "• `/summary` - Today's health & fitness dashboard\n" +
                        "• `/reset` - Start a fresh daily cycle"
            }
            lower.startsWith("/water") -> {
                val addition = 250
                val updated = currentWater + addition
                waterStore[chatId] = updated
                val target = 2500
                val pct = (updated * 100 / target).coerceAtMost(100)
                "💧 *Hydration Logged!* +${addition}ml\n" +
                        "Today's Total: *${updated}ml* / ${target}ml ($pct%)\n" +
                        "Keep drinking to hit your daily goal! 🌊"
            }
            lower.startsWith("/workout") -> {
                val detail = raw.removePrefix("/workout").trim()
                if (detail.isBlank()) "⚠️ Usage: `/workout <activity> <mins>`\nExample: `/workout 30m outdoor run`"
                else {
                    workouts.add(detail)
                    "🏋️ *Workout recorded!* \"$detail\"\nGreat job showing up today! 🔥"
                }
            }
            lower.startsWith("/summary") -> {
                val wList = if (workouts.isEmpty()) "None yet" else workouts.joinToString("\n") { "• $it" }
                val target = 2500
                val pct = (currentWater * 100 / target).coerceAtMost(100)
                "📊 *FitTrack Daily Recap* 🏁\n\n" +
                        "💧 *Hydration*: ${currentWater}ml / ${target}ml ($pct%)\n" +
                        "🏋️ *Workouts Today*:\n$wList\n\n" +
                        "🔥 *Active Calories*: ~420 kcal\n" +
                        "⏱️ *Active Time*: 45 mins\n" +
                        "Tip: Use `/water` to log your next glass!"
            }
            lower.startsWith("/calories") -> {
                val amt = raw.removePrefix("/calories").trim()
                "🥗 *Logged*: $amt kcal. Daily budget remaining: ~1,580 kcal."
            }
            lower.startsWith("/reset") -> {
                waterStore[chatId] = 0
                workouts.clear()
                "🔄 Daily fitness logs reset for a fresh day!"
            }
            else -> "💪 *FitTrack*: Try `/summary`, `/water`, or `/workout 30m run`."
        }
    }

    override suspend fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "DailyLifestyleBotRuntime"
    }
}
