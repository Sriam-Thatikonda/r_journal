// app/src/main/java/com/baverika/r_journal/data/remote/api/JournalApi.kt
package com.baverika.r_journal.data.remote.api

import com.baverika.r_journal.data.remote.dto.JournalEntryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface JournalApi {

    // MUST be this exact path: /api/journal/today
    @GET("api/journal/today")
    suspend fun getToday(): JournalEntryDto

    @POST("api/journal/today")
    suspend fun saveToday(@Body body: JournalEntryDto): JournalEntryDto
}
