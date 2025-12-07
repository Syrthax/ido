package com.ido.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Natural language date/time parser
 * Supports common phrases like "tomorrow at 3pm", "next monday 9am", etc.
 */
object NaturalDateParser {
    
    /**
     * Parse natural language input to ISO-8601 timestamp
     * Returns null if parsing fails
     */
    fun parse(input: String): Instant? {
        if (input.isBlank()) return null
        
        val normalized = input.trim().lowercase()
        val now = LocalDateTime.now()
        
        return try {
            when {
                // Today variations
                normalized.contains("today") -> parseToday(normalized, now)
                normalized.contains("tonight") -> parseTonight(now)
                
                // Tomorrow
                normalized.contains("tomorrow") -> parseTomorrow(normalized, now)
                
                // Day names
                normalized.contains("monday") -> parseDayOfWeek(normalized, now, DayOfWeek.MONDAY)
                normalized.contains("tuesday") -> parseDayOfWeek(normalized, now, DayOfWeek.TUESDAY)
                normalized.contains("wednesday") -> parseDayOfWeek(normalized, now, DayOfWeek.WEDNESDAY)
                normalized.contains("thursday") -> parseDayOfWeek(normalized, now, DayOfWeek.THURSDAY)
                normalized.contains("friday") -> parseDayOfWeek(normalized, now, DayOfWeek.FRIDAY)
                normalized.contains("saturday") -> parseDayOfWeek(normalized, now, DayOfWeek.SATURDAY)
                normalized.contains("sunday") -> parseDayOfWeek(normalized, now, DayOfWeek.SUNDAY)
                
                // Relative time
                normalized.contains("in") && normalized.contains("hour") -> parseInHours(normalized, now)
                normalized.contains("in") && normalized.contains("minute") -> parseInMinutes(normalized, now)
                
                // Next week
                normalized.contains("next week") -> parseNextWeek(normalized, now)
                
                else -> null
            }?.atZone(ZoneId.systemDefault())?.toInstant()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse "today" with optional time
     */
    private fun parseToday(input: String, now: LocalDateTime): LocalDateTime {
        val time = extractTime(input)
        return if (time != null) {
            now.withHour(time.first).withMinute(time.second).truncatedTo(ChronoUnit.MINUTES)
        } else {
            now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
        }
    }
    
    /**
     * Parse "tonight" (defaults to 8 PM)
     */
    private fun parseTonight(now: LocalDateTime): LocalDateTime {
        return now.withHour(20).withMinute(0).truncatedTo(ChronoUnit.MINUTES)
    }
    
    /**
     * Parse "tomorrow" with optional time
     */
    private fun parseTomorrow(input: String, now: LocalDateTime): LocalDateTime {
        val time = extractTime(input)
        val tomorrow = now.plusDays(1)
        return if (time != null) {
            tomorrow.withHour(time.first).withMinute(time.second).truncatedTo(ChronoUnit.MINUTES)
        } else {
            tomorrow.withHour(9).withMinute(0).truncatedTo(ChronoUnit.MINUTES)
        }
    }
    
    /**
     * Parse specific day of week
     */
    private fun parseDayOfWeek(input: String, now: LocalDateTime, targetDay: DayOfWeek): LocalDateTime {
        val time = extractTime(input)
        val isNext = input.contains("next")
        
        var target = now.with(TemporalAdjusters.nextOrSame(targetDay))
        
        // If "next" is specified or target day is today/past, move to next week
        if (isNext || target.isBefore(now) || target.toLocalDate() == now.toLocalDate()) {
            target = now.with(TemporalAdjusters.next(targetDay))
        }
        
        return if (time != null) {
            target.withHour(time.first).withMinute(time.second).truncatedTo(ChronoUnit.MINUTES)
        } else {
            target.withHour(9).withMinute(0).truncatedTo(ChronoUnit.MINUTES)
        }
    }
    
    /**
     * Parse "in X hours"
     */
    private fun parseInHours(input: String, now: LocalDateTime): LocalDateTime {
        val hours = extractNumber(input) ?: 1
        return now.plusHours(hours.toLong()).truncatedTo(ChronoUnit.MINUTES)
    }
    
    /**
     * Parse "in X minutes"
     */
    private fun parseInMinutes(input: String, now: LocalDateTime): LocalDateTime {
        val minutes = extractNumber(input) ?: 15
        return now.plusMinutes(minutes.toLong()).truncatedTo(ChronoUnit.MINUTES)
    }
    
    /**
     * Parse "next week"
     */
    private fun parseNextWeek(input: String, now: LocalDateTime): LocalDateTime {
        val time = extractTime(input)
        val nextWeek = now.plusWeeks(1).with(DayOfWeek.MONDAY)
        return if (time != null) {
            nextWeek.withHour(time.first).withMinute(time.second).truncatedTo(ChronoUnit.MINUTES)
        } else {
            nextWeek.withHour(9).withMinute(0).truncatedTo(ChronoUnit.MINUTES)
        }
    }
    
    /**
     * Extract time from input (e.g., "3pm", "09:30", "15:00")
     */
    private fun extractTime(input: String): Pair<Int, Int>? {
        // Match patterns like "3pm", "3:30pm", "15:00", "9am"
        val patterns = listOf(
            Regex("""(\d{1,2}):(\d{2})\s*(am|pm)?"""),
            Regex("""(\d{1,2})\s*(am|pm)"""),
            Regex("""(\d{1,2})(?::(\d{2}))?""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(input) ?: continue
            
            try {
                var hour = match.groupValues[1].toInt()
                val minute = if (match.groups[2] != null) {
                    match.groupValues[2].toIntOrNull() ?: 0
                } else {
                    0
                }
                
                val meridiem = match.groups[3]?.value
                
                // Handle AM/PM
                if (meridiem != null) {
                    when (meridiem.lowercase()) {
                        "pm" -> if (hour < 12) hour += 12
                        "am" -> if (hour == 12) hour = 0
                    }
                }
                
                // Validate
                if (hour in 0..23 && minute in 0..59) {
                    return Pair(hour, minute)
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    /**
     * Extract number from input
     */
    private fun extractNumber(input: String): Int? {
        val match = Regex("""\d+""").find(input)
        return match?.value?.toIntOrNull()
    }
    
    /**
     * Format instant to human-readable string
     */
    fun formatHumanReadable(instant: Instant): String {
        val now = Instant.now()
        val target = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val nowDate = LocalDateTime.ofInstant(now, ZoneId.systemDefault())
        
        val daysDiff = ChronoUnit.DAYS.between(nowDate.toLocalDate(), target.toLocalDate())
        val timeStr = target.format(DateTimeFormatter.ofPattern("h:mm a"))
        
        return when {
            daysDiff < 0 -> "Overdue - $timeStr"
            daysDiff == 0L -> "Today at $timeStr"
            daysDiff == 1L -> "Tomorrow at $timeStr"
            daysDiff < 7 -> "${target.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} at $timeStr"
            else -> target.format(DateTimeFormatter.ofPattern("MMM d")) + " at $timeStr"
        }
    }
}
