package com.iris.iriscode.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iris.iriscode.domain.model.Project
import com.iris.iriscode.domain.model.Session

@Database(entities = [Project::class, Session::class], version = 2, exportSchema = false)
abstract class IrisDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
}
