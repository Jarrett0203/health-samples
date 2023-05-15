package com.example.exercisesamplecompose.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.Instant

@Entity(tableName = "exercise_data")
data class ExerciseData(
    @PrimaryKey(autoGenerate = false)
    @TypeConverters(InstantConverter::class)
    var startTime: Instant? = null,
    var updateTime: Long = System.currentTimeMillis(),
    var duration: Long? = 0,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,
    var distance: Double? = 0.0,
    var calories: Double? = 0.0,
    var heartRate: Double? = 0.0,
    var heartRateAvg: Double? = 0.0,
    var steps: Int? = 0,
    var speed: Double? = 0.0,
)