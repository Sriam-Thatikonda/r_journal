package com.baverika.r_journal.data.local.converters

import android.net.Uri
import androidx.room.TypeConverter
import com.baverika.r_journal.data.local.entity.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    // -------------------------------
    // ChatMessage converters
    // -------------------------------
    @TypeConverter
    fun fromMessages(messages: List<ChatMessage>?): String {
        if (messages.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (msg in messages) {
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("role", msg.role)
            obj.put("content", msg.content)
            obj.put("timestamp", msg.timestamp)
            obj.put("imageUri", msg.imageUri) // nullable

            // NEW: write reply fields (may be null)
            obj.put("replyToMessageId", msg.replyToMessageId)
            obj.put("replyPreview", msg.replyPreview)

            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toMessages(json: String?): List<ChatMessage> {
        if (json.isNullOrEmpty() || json == "[]") return emptyList()
        val array = JSONArray(json)
        val messages = mutableListOf<ChatMessage>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            // backwards-compatible reads:
            val id = obj.optString("id", java.util.UUID.randomUUID().toString())
            val role = obj.optString("role", "user")
            val content = obj.optString("content", "")
            val timestamp = if (obj.has("timestamp")) obj.getLong("timestamp") else System.currentTimeMillis()
            val imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() }

            // NEW: read reply fields using optString so missing keys result in null
            val replyToMessageId = obj.optString("replyToMessageId").takeIf { it.isNotBlank() }
            val replyPreview = obj.optString("replyPreview").takeIf { it.isNotBlank() }

            messages.add(
                ChatMessage(
                    id = id,
                    role = role,
                    content = content,
                    timestamp = timestamp,
                    imageUri = imageUri,
                    replyToMessageId = replyToMessageId,
                    replyPreview = replyPreview
                )
            )
        }
        return messages
    }

    // -------------------------------
    // Tags converters (List<String>)
    // -------------------------------
    @TypeConverter
    fun fromTags(tags: List<String>?): String {
        if (tags.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        tags.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toTags(json: String?): List<String> {
        if (json.isNullOrEmpty() || json == "[]") return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { i -> array.getString(i) }
    }

    // -------------------------------
    // ImageUris converters (List<Uri>)
    // -------------------------------
    @TypeConverter
    fun fromImageUris(uris: List<Uri>?): String {
        if (uris.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        uris.forEach { array.put(it.toString()) }
        return array.toString()
    }

    @TypeConverter
    fun toImageUris(json: String?): List<Uri> {
        if (json.isNullOrEmpty() || json == "[]") return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { i -> Uri.parse(array.getString(i)) }
    }

    // -------------------------------
    // Int List converters (List<Int>)
    // -------------------------------
    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        if (list.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int> {
        if (json.isNullOrEmpty() || json == "[]") return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { i -> array.getInt(i) }
    }
}
