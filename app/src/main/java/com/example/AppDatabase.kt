package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String = "",
    val priceTarget: Double,
    val isTriggered: Boolean = false,
    val isAlertActive: Boolean = true
)

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts")
    fun getAllAlerts(): Flow<List<PriceAlert>>
    
    @Query("SELECT * FROM price_alerts WHERE isAlertActive = 1")
    suspend fun getActiveAlerts(): List<PriceAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert)
    
    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)
}

@Database(entities = [PriceAlert::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceAlertDao(): PriceAlertDao
}
