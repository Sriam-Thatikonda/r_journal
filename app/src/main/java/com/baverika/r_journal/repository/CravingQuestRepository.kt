package com.baverika.r_journal.repository

import com.baverika.r_journal.data.local.dao.CravingLogDao
import com.baverika.r_journal.data.local.entity.CravingLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class CravingQuestRepository(private val cravingLogDao: CravingLogDao) {

    private val junkKeywords = setOf(
        "pizza", "burger", "chips", "icecream", "chocolate", "ice cream", "soda", "fries", "cake",
        "candy", "donut", "cookies", "biscuit", "brownie", "muffin", "pastry", 
        "hot dog", "taco", "nachos", "popcorn", "coke", "pepsi", "sprite", "fanta", 
        "milkshake", "smoothie", "syrup", "waffle", "pancake", "crepe", "gelato", 
        "sorbet", "custard", "pudding", "junk", "fast food", "snack"
    )

    private val indoorQuests = mapOf(
        "Easy" to listOf(
            "Do 10 jumping jacks before eating.",
            "Drink 1 glass of water.",
            "Wait 5 minutes before your first bite.",
            "Stand up and stretch for 2 minutes.",
            "Write down how you feel right now.",
            "Write python code for a simple problem",
            "Cleanp recycle bin in VAHL"
        ),
        "Medium" to listOf(
            "Do 20 squats.",
            "Drink 2 full glasses of water.",
            "Tidy up your workspace/room for 5 minutes.",
            "Do a 30-second plank.",
            "List 5 things you're grateful for.",
            "Uninstall apps that are not using",
            "Learn new technology basic things",
            "Clean Laptop and phone"
        ),
        "Hard" to listOf(
            "Do 15 pushups and 20 situps.",
            "Plank for 5 minutes.",
            "Drink 2 glasses of water and wait 10 minutes.",
            "Clean the dishes or a shared space.",
            "Do a 1-minute wall sit.",
            "Meditate silently for 3 minutes.",
            "Deploy a pod in kubernetes cluster.",
            "Brainstorm minimum 10 minutes for Journal App features",
            "Clear the storage, delete unwanted photos, videos"
        )
    )

    private val outdoorQuests = mapOf(
        "Easy" to listOf(
            "Walk to the next intersection and back.",
            "Take 5 deep breaths and wait 2 minutes.",
            "Look for three different types of birds or trees.",
            "Do 10 calf raises while waiting.",
            "Stand perfectly still for 1 minute.",
            "Read latest news.",
            "Read article about latest technology"
        ),
        "Medium" to listOf(
            "Walk around the block once.",
            "Read a nutritional label and wait 5 minutes.",
            "Find a park bench and sit for 3 minutes without your phone.",
            "Walk 100 steps in the opposite direction first.",
            "Identify 5 different colors in your surroundings.",
            "Read latest news and research about it.",
            "Read reviews about latest phone"
        ),
        "Hard" to listOf(
            "Walk for 10 minutes before eating.",
            "Perform 20 lunges on the sidewalk.",
            "Find a trash bin and pick up 3 pieces of litter.",
            "Climb two flights of stairs and come back.",
            "Jog in place for 2 minutes."
        )
    )

    private val recentQuests = mutableListOf<String>()
    private val maxRecentSize = 10

    fun isJunkFood(food: String): Boolean {
        val lowerFood = food.lowercase()
        return junkKeywords.any { lowerFood.contains(it) }
    }

    suspend fun getTodayCount(): Int {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return cravingLogDao.getLogCountToday(startOfDay)
    }

    fun getAllLogs(): Flow<List<CravingLogEntity>> = cravingLogDao.getAllLogs()

    suspend fun insertLog(log: CravingLogEntity) = cravingLogDao.insertLog(log)
    
    suspend fun updateLog(log: CravingLogEntity) = cravingLogDao.updateLog(log)

    suspend fun generateQuest(location: String): Pair<String, String> {
        val todayCount = getTodayCount()
        val difficulty = when {
            todayCount == 0 -> "Easy"
            todayCount == 1 -> "Medium"
            todayCount == 2 -> "Hard"
            else -> "Boss"
        }

        val pool = if (location.equals("Indoor", ignoreCase = true)) indoorQuests else outdoorQuests
        
        val questText = when (difficulty) {
            "Easy", "Medium" -> {
                getRandomFromPool(pool[difficulty] ?: pool["Easy"]!!)
            }
            "Hard" -> {
                val q1 = getRandomFromPool(pool["Easy"]!!)
                val q2 = getRandomFromPool(pool["Medium"]!!)
                "$q1 AND $q2"
            }
            "Boss" -> {
                val q1 = getRandomFromPool(pool["Easy"]!!)
                val q2 = getRandomFromPool(pool["Medium"]!!)
                val q3 = getRandomFromPool(pool["Hard"]!!)
                "$q1, $q2, AND $q3"
            }
            else -> getRandomFromPool(pool["Easy"]!!)
        }

        return difficulty to questText
    }

    private fun getRandomFromPool(pool: List<String>): String {
        val available = pool.filter { !recentQuests.contains(it) }
        val choice = if (available.isNotEmpty()) available.random() else pool.random()
        
        recentQuests.add(choice)
        if (recentQuests.size > maxRecentSize) {
            recentQuests.removeAt(0)
        }
        return choice
    }
}
