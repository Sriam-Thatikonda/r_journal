package com.baverika.r_journal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.baverika.r_journal.data.local.entity.Password
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY siteName ASC")
    fun getAllPasswords(): Flow<List<Password>>

    @Query("SELECT * FROM passwords WHERE siteName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY siteName ASC")
    fun searchPasswords(query: String): Flow<List<Password>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: Password)

    @Update
    suspend fun updatePassword(entry: Password)

    @Delete
    suspend fun deletePassword(entry: Password)
}
