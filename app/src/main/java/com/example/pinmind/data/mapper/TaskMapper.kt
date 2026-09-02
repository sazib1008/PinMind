package com.example.pinmind.data.mapper

import com.example.pinmind.data.local.TaskEntity
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.Task

/**
 * Maps a Room [TaskEntity] to a pure domain [Task].
 */
fun TaskEntity.toDomain(): Task {
    val location = if (latitude != null && longitude != null) {
        GeoLocation(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters ?: 100f,
            locationName = locationName ?: "",
            address = address
        )
    } else {
        null
    }

    return Task(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = priority,
        status = status,
        geoLocation = location,
        transitionType = transitionType,
        dwellTimeSeconds = dwellTimeSeconds,
        dueDate = dueDate,
        createdAt = createdAt,
        completedAt = completedAt
    )
}

/**
 * Maps a pure domain [Task] to a Room [TaskEntity].
 */
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = priority,
        status = status,
        latitude = geoLocation?.latitude,
        longitude = geoLocation?.longitude,
        radiusMeters = geoLocation?.radiusMeters,
        locationName = geoLocation?.locationName,
        address = geoLocation?.address,
        transitionType = transitionType,
        dwellTimeSeconds = dwellTimeSeconds,
        dueDate = dueDate,
        createdAt = createdAt,
        completedAt = completedAt
    )
}

/**
 * Maps a list of entities to a list of domain models.
 */
fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }
