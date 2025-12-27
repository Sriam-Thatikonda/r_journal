package com.baverika.r_journal.data.local.dao

import androidx.room.*
import com.baverika.r_journal.data.local.entity.Habit
import com.baverika.r_journal.data.local.entity.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    // --- Habits ---
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // --- Logs ---
    @Query("SELECT * FROM habit_logs WHERE dateMillis = :dateMillis")
    fun getHabitLogsForDate(dateMillis: Long): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE dateMillis BETWEEN :startMillis AND :endMillis")
    fun getHabitLogsBetween(startMillis: Long, endMillis: Long): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateMillis = :dateMillis")
    suspend fun deleteHabitLog(habitId: String, dateMillis: Long)
    
    // Synchronous methods for widget
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveHabitsSync(): List<Habit>
    
    @Query("SELECT * FROM habit_logs WHERE dateMillis = :dateMillis")
    fun getHabitLogsForDateSync(dateMillis: Long): List<HabitLog>
}
