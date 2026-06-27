package com.baverika.r_journal.utils

import androidx.room.TypeConverter
import com.baverika.r_journal.data.ChallengeStatus
import com.baverika.r_journal.data.FrequencyType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ChallengeTypeConverters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.toString()
    }

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it) }
    }

    @TypeConverter
    fun fromChallengeStatus(status: ChallengeStatus): String {
        return status.name
    }

    @TypeConverter
    fun toChallengeStatus(statusString: String): ChallengeStatus {
        return ChallengeStatus.valueOf(statusString)
    }

    @TypeConverter
    fun fromFrequencyType(type: FrequencyType): String {
        return type.name
    }

    @TypeConverter
    fun toFrequencyType(typeString: String): FrequencyType {
        return FrequencyType.valueOf(typeString)
    }
}
