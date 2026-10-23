package com.flan1e.schedule.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Хранимая пара. Время — минуты от полуночи, чтобы не зависеть от часового пояса
 * и не хранить даты: расписание повторяется каждую неделю.
 */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    @ColumnInfo(name = "start_minute")
    val startMinute: Int,
    @ColumnInfo(name = "end_minute")
    val endMinute: Int,
    val title: String,
    val room: String,
    val parity: String,
)
