package com.flan1e.schedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LessonEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
}
