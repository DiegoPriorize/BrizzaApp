package com.priorizedev.brizza

import android.app.Application
import androidx.room.Room
import com.priorizedev.brizza.data.db.AppDatabase
import com.priorizedev.brizza.data.repository.AppRepository

class ClimaGestApp : Application() {
    
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "climagest_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val repository: AppRepository by lazy {
        AppRepository(database)
    }
}
