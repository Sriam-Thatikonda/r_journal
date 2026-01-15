package com.baverika.r_journal.repository

import com.baverika.r_journal.data.local.dao.PasswordDao
import com.baverika.r_journal.data.local.entity.Password
import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {
    val allPasswords: Flow<List<Password>> = passwordDao.getAllPasswords()

    fun searchPasswords(query: String): Flow<List<Password>> {
        return passwordDao.searchPasswords(query)
    }

    suspend fun insertPassword(password: Password) {
        passwordDao.insertPassword(password)
    }

    suspend fun updatePassword(password: Password) {
        passwordDao.updatePassword(password)
    }

    suspend fun deletePassword(password: Password) {
        passwordDao.deletePassword(password)
    }
}
