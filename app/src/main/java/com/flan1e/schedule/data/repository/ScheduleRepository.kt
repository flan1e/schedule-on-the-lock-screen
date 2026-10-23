package com.flan1e.schedule.data.repository

import com.flan1e.schedule.data.local.LessonDao
import com.flan1e.schedule.data.local.LessonEntity
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.WeekParity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

interface ScheduleRepository {
    fun observeLessons(): Flow<List<Lesson>>
    suspend fun getLessons(): List<Lesson>
    suspend fun saveLesson(lesson: Lesson): Long
    suspend fun deleteLesson(id: Long)

    /** Заменяет всё расписание одним действием — для импорта резервной копии. */
    suspend fun replaceAll(lessons: List<Lesson>)
}

class RoomScheduleRepository(
    private val dao: LessonDao,
) : ScheduleRepository {

    override fun observeLessons(): Flow<List<Lesson>> =
        dao.observeAll().map { entities -> entities.map(LessonEntity::toDomain) }

    override suspend fun getLessons(): List<Lesson> =
        dao.getAll().map(LessonEntity::toDomain)

    override suspend fun saveLesson(lesson: Lesson): Long =
        if (lesson.id == 0L) {
            dao.insert(lesson.toEntity())
        } else {
            dao.update(lesson.toEntity())
            lesson.id
        }

    override suspend fun deleteLesson(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun replaceAll(lessons: List<Lesson>) {
        dao.replaceAll(lessons.map { it.copy(id = 0L).toEntity() })
    }
}

internal fun LessonEntity.toDomain(): Lesson = Lesson(
    id = id,
    dayOfWeek = DayOfWeek.of(dayOfWeek.coerceIn(1, 7)),
    start = LocalTime.of(startMinute / 60, startMinute % 60),
    end = LocalTime.of(endMinute / 60, endMinute % 60),
    title = title,
    room = room,
    parity = WeekParity.entries.firstOrNull { it.name == parity } ?: WeekParity.EVERY_WEEK,
)

internal fun Lesson.toEntity(): LessonEntity = LessonEntity(
    id = id,
    dayOfWeek = dayOfWeek.value,
    startMinute = start.hour * 60 + start.minute,
    endMinute = end.hour * 60 + end.minute,
    title = title,
    room = room,
    parity = parity.name,
)
