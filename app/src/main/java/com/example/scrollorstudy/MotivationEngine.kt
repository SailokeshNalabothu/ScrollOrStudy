package com.example.scrollorstudy

import kotlin.random.Random

object MotivationEngine {

    private val scrollQuotes = listOf(
        "Your future self is watching you scroll... and they aren't happy. 🤨",
        "The algorithm is winning. Don't let it. ⚔️",
        "Your thumb needs a break! 😂",
        "Is this scroll worth your dreams? 📚",
        "Focus now, relax later. It's that simple. ✨",
        "That's enough Internet for now. Go study! 🚀",
        "You're better than this distraction. 💪"
    )

    private val streakQuotes = mapOf(
        1 to "Good start! The first step is the hardest. 🔥",
        3 to "3 days! You're building real momentum now! 🚀",
        7 to "A full week! You're becoming a productivity pro! ✨",
        30 to "UNSTOPPABLE. You've officially mastered discipline. 👑"
    )

    private val studyQuotes = listOf(
        "Small steps every day = Big success. 🏔️",
        "Focus on being productive, not busy. 🎯",
        "Your future self will thank you for today. 🌟",
        "Discipline is choosing between what you want now and what you want most. 🧠"
    )

    fun getRandomScrollQuote(): String {
        return scrollQuotes[Random.nextInt(scrollQuotes.size)]
    }

    fun getStreakMessage(streak: Int): String {
        return streakQuotes[streak] ?: "Keep that streak alive! Current: $streak days 🔥"
    }

    fun getRandomStudyQuote(): String {
        return studyQuotes[Random.nextInt(studyQuotes.size)]
    }
}
