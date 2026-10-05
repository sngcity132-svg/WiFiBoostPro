package com.wifiboost.pro.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long, val ssid: String?, val rssi: Int?, val ping: Double?, val jitter: Double?,
    val loss: Double?, val down: Double?, val up: Double?, val band: String?, val quality: String?,
)

@Dao
interface MeasurementDao {
    @Insert suspend fun insert(m: Measurement)
    @Query("SELECT * FROM measurements ORDER BY ts DESC") fun all(): Flow<List<Measurement>>
    @Query("DELETE FROM measurements WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM measurements") suspend fun clear()
}

@Database(entities = [Measurement::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): MeasurementDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "wifiboost.db").build().also { inst = it }
        }
    }
}
