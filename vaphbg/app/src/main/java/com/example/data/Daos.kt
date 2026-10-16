package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface BlueprintDao {
    @Query("SELECT * FROM blueprints ORDER BY createdAt DESC")
    fun getAllBlueprints(): Flow<List<BlueprintEntity>>

    @Query("SELECT * FROM blueprints WHERE id = :id LIMIT 1")
    suspend fun getBlueprintById(id: Long): BlueprintEntity?

    @Query("SELECT * FROM blueprints WHERE category = :category ORDER BY createdAt DESC")
    fun getBlueprintsByCategory(category: String): Flow<List<BlueprintEntity>>

    @Query("SELECT * FROM blueprints WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getBlueprintsByTag(tag: String): Flow<List<BlueprintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlueprint(blueprint: BlueprintEntity): Long

    @Query("UPDATE blueprints SET category = :category, tags = :tags WHERE id = :id")
    suspend fun updateBlueprintTagsAndCategory(id: Long, category: String, tags: String)

    @Delete
    suspend fun deleteBlueprint(blueprint: BlueprintEntity)

    @Query("DELETE FROM blueprints WHERE id = :id")
    suspend fun deleteBlueprintById(id: Long)
}
