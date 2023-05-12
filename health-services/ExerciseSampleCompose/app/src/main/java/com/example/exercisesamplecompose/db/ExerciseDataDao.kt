package com.example.exercisesamplecompose.db

import androidx.health.services.client.data.ExerciseState
import androidx.room.*

@Dao
interface ExerciseDataDao {
    @Upsert
    suspend fun upsertExerciseData(exerciseServiceData: ExerciseData)

    @Query("SELECT * FROM exercise_data WHERE startTime = :startTime ORDER BY startTime DESC LIMIT 1")
    suspend fun getPrevExerciseData(startTime: Long): ExerciseData?
}