package com.flan1e.schedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons")
    suspend fun deleteAll()

    /** Импорт: либо новое расписание целиком, либо прежнее без изменений. */
    @Transaction
    suspend fun replaceAll(lessons: List<LessonEntity>) {
        deleteAll()
        insertAll(lessons)
    }
}
