package com.flan1e.schedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LessonEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao

    companion object {
        /** Добавлен тип пары: старые записи получают «Другое». */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lessons ADD COLUMN kind TEXT NOT NULL DEFAULT 'OTHER'")
            }
        }
    }
}
