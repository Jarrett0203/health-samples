package com.example.exercisesamplecompose.db

import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExerciseDataRepository(
    private val exerciseDataDao: ExerciseDataDao,
    private val scope: CoroutineScope
) {
    private var saveDataJob: Job? = null
    private var isPaused: Boolean = false
    private var resumedTime: Long = 0L
    private var totalActiveDuration: Long = 0L

    suspend fun startSavingData(exerciseUpdate: ExerciseUpdate) {
        saveDataJob?.cancel()
        saveDataJob = scope.launch(Dispatchers.IO) {
            val prevExerciseData =
                exerciseDataDao.getPrevExerciseData(exerciseUpdate.startTime!!.toEpochMilli())
            while (isActive) {
                if (!isPaused) {
                    if (resumedTime == 0L) {
                        resumedTime = exerciseUpdate.startTime!!.toEpochMilli()
                    }
                    val locationDataList = exerciseUpdate.latestMetrics.getData(DataType.LOCATION)
                    val currentLocationData = locationDataList.lastOrNull()?.value
                    val updatedHeartRate: Double? =
                        if (exerciseUpdate.latestMetrics.getData(DataType.HEART_RATE_BPM).isNotEmpty()) {
                            exerciseUpdate.latestMetrics.getData(DataType.HEART_RATE_BPM)
                                .last().value
                        } else {
                            prevExerciseData?.heartRate
                        }
                    val updatedSpeed: Double? =
                        if (exerciseUpdate.latestMetrics.getData(DataType.SPEED).isNotEmpty()) {
                            exerciseUpdate.latestMetrics.getData(DataType.SPEED)
                                .last().value * 3.6 // Convert m/s to km/h
                        } else {
                            prevExerciseData?.speed
                        }
                    val exerciseData = ExerciseData(
                        startTime = exerciseUpdate.startTime,
                        updateTime = System.currentTimeMillis(),
                        duration = calculateActiveDuration(),
                        latitude = currentLocationData?.latitude ?: prevExerciseData?.latitude,
                        longitude = currentLocationData?.longitude ?: prevExerciseData?.longitude,
                        distance = exerciseUpdate.latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total
                            ?: prevExerciseData?.distance,
                        calories = exerciseUpdate.latestMetrics.getData(DataType.CALORIES_TOTAL)?.total
                            ?: prevExerciseData?.calories,
                        heartRate = updatedHeartRate,
                        heartRateAvg = exerciseUpdate.latestMetrics.getData(DataType.HEART_RATE_BPM_STATS)?.average
                            ?: prevExerciseData?.heartRateAvg,
                        steps = exerciseUpdate.latestMetrics.getData(DataType.STEPS_TOTAL)?.total?.toInt()
                            ?: prevExerciseData?.steps,
                        speed = updatedSpeed,
                        speedAvg = exerciseUpdate.latestMetrics.getData(DataType.SPEED_STATS)?.average?.times(
                            3.6
                        ) ?: prevExerciseData?.speedAvg // Convert m/s to km/h
                    )
                    exerciseDataDao.upsertExerciseData(exerciseData)
                }
            }
        }
    }

    fun stopSavingData() {
        saveDataJob?.cancel()
        saveDataJob = null
        resumedTime = 0L
        totalActiveDuration = 0L
        isPaused = false
    }

    fun pauseSavingData() {
        if (!isPaused) {
            isPaused = true
            totalActiveDuration += System.currentTimeMillis() - resumedTime
        }
    }

    fun resumeSavingData() {
        isPaused = false
        resumedTime = System.currentTimeMillis()
    }

    private fun calculateActiveDuration(): Long {
        return totalActiveDuration + (System.currentTimeMillis() - resumedTime)
    }
}