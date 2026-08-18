package com.echoos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.echoos.data.entity.AutomationEntity
import com.echoos.data.entity.CommitmentEntity
import com.echoos.data.entity.ContextEventEntity
import com.echoos.data.entity.ExecutionEntity
import com.echoos.data.entity.PatternEntity
import com.echoos.data.entity.PermissionEntity
import com.echoos.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Insert suspend fun insert(a: AutomationEntity): Long
    @Update suspend fun update(a: AutomationEntity)
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AutomationEntity>>
    @Query("SELECT * FROM automations WHERE status = 'active'")
    suspend fun active(): List<AutomationEntity>
    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun byId(id: Long): AutomationEntity?
    @Query("UPDATE automations SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)
    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ContextEventDao {
    @Insert suspend fun insert(e: ContextEventEntity)
    @Query("SELECT * FROM context_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ContextEventEntity>>
    @Query("SELECT * FROM context_events ORDER BY timestamp ASC")
    suspend fun all(): List<ContextEventEntity>
    @Query("DELETE FROM context_events")
    suspend fun clear()
}

@Dao
interface CommitmentDao {
    @Insert suspend fun insert(c: CommitmentEntity): Long
    @Update suspend fun update(c: CommitmentEntity)
    @Query("SELECT * FROM commitments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CommitmentEntity>>
    @Query("UPDATE commitments SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)
    @Query("SELECT * FROM commitments WHERE status = 'accepted'")
    suspend fun accepted(): List<CommitmentEntity>
    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM commitments")
    suspend fun clear()
}

@Dao
interface PatternDao {
    @Insert suspend fun insert(p: PatternEntity): Long
    @Query("SELECT * FROM patterns ORDER BY confidence DESC")
    fun observeAll(): Flow<List<PatternEntity>>
    @Query("UPDATE patterns SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)
    @Query("DELETE FROM patterns")
    suspend fun clear()
}

@Dao
interface ExecutionDao {
    @Insert suspend fun insert(e: ExecutionEntity)
    @Query("SELECT * FROM executions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExecutionEntity>>
    @Query("DELETE FROM executions")
    suspend fun clear()
}

@Dao
interface PermissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: PermissionEntity)
    @Query("SELECT * FROM permissions")
    fun observeAll(): Flow<List<PermissionEntity>>
    @Query("SELECT enabled FROM permissions WHERE capability = :capability")
    suspend fun isEnabled(capability: String): Boolean?
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(u: UserEntity)
    @Query("SELECT * FROM users WHERE id = 'local'")
    suspend fun get(): UserEntity?
    @Query("SELECT * FROM users WHERE id = 'local'")
    fun observe(): Flow<UserEntity?>
    @Query("DELETE FROM users")
    suspend fun clear()
}
