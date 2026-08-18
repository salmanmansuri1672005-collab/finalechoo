package com.echoos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.echoos.data.dao.AutomationDao
import com.echoos.data.dao.CommitmentDao
import com.echoos.data.dao.ContextEventDao
import com.echoos.data.dao.ExecutionDao
import com.echoos.data.dao.PatternDao
import com.echoos.data.dao.PermissionDao
import com.echoos.data.dao.UserDao
import com.echoos.data.entity.AutomationEntity
import com.echoos.data.entity.CommitmentEntity
import com.echoos.data.entity.ContextEventEntity
import com.echoos.data.entity.ExecutionEntity
import com.echoos.data.entity.PatternEntity
import com.echoos.data.entity.PermissionEntity
import com.echoos.data.entity.UserEntity

/** Local-first storage (SRS §13.1). Owner: Tushar. */
@Database(
    entities = [
        UserEntity::class, AutomationEntity::class, ContextEventEntity::class,
        CommitmentEntity::class, PatternEntity::class, ExecutionEntity::class,
        PermissionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun contextEventDao(): ContextEventDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun patternDao(): PatternDao
    abstract fun executionDao(): ExecutionDao
    abstract fun permissionDao(): PermissionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var instance: EchoDatabase? = null

        fun get(context: Context): EchoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, EchoDatabase::class.java, "echoos.db"
                ).fallbackToDestructiveMigration()
                 .build().also { instance = it }
            }
    }
}
