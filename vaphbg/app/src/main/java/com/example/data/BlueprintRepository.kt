package com.example.data

import kotlinx.coroutines.flow.Flow

class BlueprintRepository(private val appDatabase: AppDatabase) {
    private val userDao = appDatabase.userDao()
    private val blueprintDao = appDatabase.blueprintDao()

    val allBlueprints: Flow<List<BlueprintEntity>> = blueprintDao.getAllBlueprints()

    suspend fun getBlueprintById(id: Long): BlueprintEntity? {
        return blueprintDao.getBlueprintById(id)
    }

    suspend fun insertBlueprint(blueprint: BlueprintEntity): Long {
        return blueprintDao.insertBlueprint(blueprint)
    }

    fun getBlueprintsByCategory(category: String): Flow<List<BlueprintEntity>> {
        return blueprintDao.getBlueprintsByCategory(category)
    }

    fun getBlueprintsByTag(tag: String): Flow<List<BlueprintEntity>> {
        return blueprintDao.getBlueprintsByTag(tag)
    }

    suspend fun updateBlueprintTagsAndCategory(id: Long, category: String, tags: String) {
        blueprintDao.updateBlueprintTagsAndCategory(id, category, tags)
    }

    suspend fun deleteBlueprintById(id: Long) {
        blueprintDao.deleteBlueprintById(id)
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }
}
