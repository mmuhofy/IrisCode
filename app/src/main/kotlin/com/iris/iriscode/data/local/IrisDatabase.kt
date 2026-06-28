package com.iris.iriscode.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iris.iriscode.domain.model.Project

@Database(entities = [Project::class], version = 1, exportSchema = false)
abstract class IrisDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
