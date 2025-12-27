// app/src/main/java/com/baverika/r_journal/data/remote/RetrofitClient.kt
package com.baverika.r_journal.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.baverika.r_journal.data.remote.api.JournalApi
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var baseUrl: String = "http://127.0.0.1:5000/"

    @Volatile
    private var apiInstance: JournalApi? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun setHostPort(hostPort: String) {
        val trimmed = hostPort.trim()
        val url = if (trimmed.startsWith("http")) trimmed else "http://$trimmed"
        baseUrl = if (url.endsWith("/")) url else "$url/"
        apiInstance = null // force rebuild next time
    }

    fun getBaseUrl(): String = baseUrl

    fun getApi(): JournalApi {
        val existing = apiInstance
        if (existing != null) return existing

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val created = retrofit.create(JournalApi::class.java)
        apiInstance = created
        return created
    }
}
