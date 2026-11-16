package com.workoutplanner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workoutplanner.data.seed.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [Exercise::class, WorkoutPlan::class, PlanExercise::class, WorkoutSession::class, SetLog::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao

    companion object {
        /**
         * Builds the database and adds any missing default plans each time it is opened.
         * Seeding runs on [scope]; screens observe Flows, so they update as soon as the
         * rows land.
         */
        fun build(context: Context, scope: CoroutineScope): AppDatabase {
            lateinit var database: AppDatabase
            database = Room.databaseBuilder(context, AppDatabase::class.java, "workout_planner.db")
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        scope.launch { DatabaseSeeder.seed(database) }
                    }
                })
                .build()
            return database
        }
    }
}
