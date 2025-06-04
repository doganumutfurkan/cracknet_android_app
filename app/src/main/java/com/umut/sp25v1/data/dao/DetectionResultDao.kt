package com.umut.sp25v1.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.umut.sp25v1.data.model.DetectionResult


@Dao
interface DetectionResultDao {

    @Query("SELECT * FROM detection_results WHERE id = :id")
    suspend fun getById(id: Long): DetectionResult?

    @Update
    suspend fun update(result: DetectionResult)



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: DetectionResult)

    @Insert
    suspend fun insertDetection(detectionResult: DetectionResult): Long

    @Query("SELECT * FROM detection_results ORDER BY timestamp DESC")
    suspend fun getAllDetections(): List<DetectionResult>

    @Delete
    suspend fun deleteDetection(detectionResult: DetectionResult)

    @Update
    suspend fun updateDetection(detectionResult: DetectionResult)

    @Query("DELETE FROM detection_results")
    suspend fun deleteAllDetections()

    @Query("SELECT * FROM detection_results ORDER BY timestamp DESC")
    fun getAllSync(): List<DetectionResult>

}



