package com.flan1e.schedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons ORDER BY day_of_week, start_minute")
    fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons ORDER BY day_of_week, start_minute")
    suspend fun getAll(): List<LessonEntity>

    @Insert
    suspend fun insert(lesson: LessonEntity): Long

    @Update
    suspend fun update(lesson: LessonEntity)

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteById(id: Long)
}
