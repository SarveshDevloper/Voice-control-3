package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "voice_macros")
data class VoiceMacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerPhrase: String,
    val targetActionCode: String,
    val descriptionHi: String,
    val category: String,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_logs")
data class CommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val spokenPhrase: String,
    val matchedActionTitle: String,
    val responseMessage: String,
    val category: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VoiceControlDao {
    @Query("SELECT * FROM voice_macros ORDER BY usageCount DESC, createdAt DESC")
    fun getAllMacros(): Flow<List<VoiceMacroEntity>>

    @Query("SELECT * FROM voice_macros")
    suspend fun getAllMacrosSnapshot(): List<VoiceMacroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: VoiceMacroEntity)

    @Query("UPDATE voice_macros SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementMacroUsage(id: Int)

    @Query("DELETE FROM voice_macros WHERE id = :id")
    suspend fun deleteMacroById(id: Int)

    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<CommandLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommandLogEntity)

    @Query("DELETE FROM command_logs")
    suspend fun clearAllLogs()
}

@Database(
    entities = [VoiceMacroEntity::class, CommandLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AwaazDatabase : RoomDatabase() {
    abstract fun voiceControlDao(): VoiceControlDao

    companion object {
        @Volatile
        private var INSTANCE: AwaazDatabase? = null

        fun getInstance(context: Context): AwaazDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AwaazDatabase::class.java,
                    "awaaz_control_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class VoiceControlRepository(private val dao: VoiceControlDao) {
    val allMacros: Flow<List<VoiceMacroEntity>> = dao.getAllMacros()
    val recentLogs: Flow<List<CommandLogEntity>> = dao.getRecentLogs()

    suspend fun getAllMacrosSnapshot(): List<VoiceMacroEntity> = dao.getAllMacrosSnapshot()

    suspend fun insertMacro(macro: VoiceMacroEntity) = dao.insertMacro(macro)

    suspend fun incrementMacroUsage(id: Int) = dao.incrementMacroUsage(id)

    suspend fun deleteMacro(id: Int) = dao.deleteMacroById(id)

    suspend fun logCommand(
        spokenPhrase: String,
        matchedActionTitle: String,
        responseMessage: String,
        category: String,
        isSuccess: Boolean
    ) {
        dao.insertLog(
            CommandLogEntity(
                spokenPhrase = spokenPhrase,
                matchedActionTitle = matchedActionTitle,
                responseMessage = responseMessage,
                category = category,
                isSuccess = isSuccess
            )
        )
    }

    suspend fun clearLogs() = dao.clearAllLogs()

    suspend fun seedDefaultMacrosIfEmpty() {
        val current = dao.getAllMacrosSnapshot()
        if (current.isEmpty()) {
            val defaults = listOf(
                VoiceMacroEntity(
                    triggerPhrase = "andhera hai",
                    targetActionCode = "TORCH_ON",
                    descriptionHi = "Roshni ke liye तुरंत Torch / Flashlight ON karein",
                    category = "Hardware",
                    usageCount = 5
                ),
                VoiceMacroEntity(
                    triggerPhrase = "roshni band karo",
                    targetActionCode = "TORCH_OFF",
                    descriptionHi = "Torch / Flashlight ko turant OFF karein",
                    category = "Hardware",
                    usageCount = 3
                ),
                VoiceMacroEntity(
                    triggerPhrase = "subah uthao",
                    targetActionCode = "OPEN_ALARM",
                    descriptionHi = "Alarm & Clock settings turant kholein",
                    category = "Productivity",
                    usageCount = 4
                ),
                VoiceMacroEntity(
                    triggerPhrase = "selfie time",
                    targetActionCode = "OPEN_CAMERA",
                    descriptionHi = "Photo lene ke liye Camera launch karein",
                    category = "Media",
                    usageCount = 6
                ),
                VoiceMacroEntity(
                    triggerPhrase = "meeting mode",
                    targetActionCode = "VOLUME_MUTE",
                    descriptionHi = "Phone ki media aur ringtone awaaz mute karein",
                    category = "Audio",
                    usageCount = 2
                ),
                VoiceMacroEntity(
                    triggerPhrase = "net chalu karo",
                    targetActionCode = "OPEN_WIFI",
                    descriptionHi = "Wi-Fi aur Internet settings kholein",
                    category = "Settings",
                    usageCount = 3
                )
            )
            defaults.forEach { dao.insertMacro(it) }
        }
    }
}
