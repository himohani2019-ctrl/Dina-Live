package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey val id: Int = 1,
    val nickname: String = "الداعم الأكبر Himo 👑",
    val coins: Int = 4500000,
    val xp: Int = 350000,
    val level: Int = 95,
    val isHost: Boolean = false,
    val isOwner: Boolean = true,
    val avatarId: Int = 1,
    val isRegistered: Boolean = false
)

@Entity(tableName = "hosts")
data class HostEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val tag: String,
    val followers: Int,
    val totalDiamonds: Int, // increased when getting gifts
    val isLive: Boolean = true,
    val bio: String = "",
    val avatarId: Int // index/resource pointer for avatar styling
)

@Entity(tableName = "gift_logs")
data class GiftLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hostName: String,
    val giftName: String,
    val cost: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface LiveDao {
    @Query("SELECT * FROM user_account WHERE id = 1 LIMIT 1")
    fun getUserFlow(): Flow<UserAccount?>

    @Query("SELECT * FROM user_account WHERE id = 1 LIMIT 1")
    suspend fun getUserSync(): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Query("SELECT * FROM hosts ORDER BY totalDiamonds DESC")
    fun getAllHostsFlow(): Flow<List<HostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHosts(hosts: List<HostEntity>)

    @Query("UPDATE hosts SET totalDiamonds = totalDiamonds + :amount WHERE name = :hostName")
    suspend fun addDiamondsToHost(hostName: String, amount: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftLog(log: GiftLog)

    @Query("SELECT * FROM gift_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentGiftLogsFlow(): Flow<List<GiftLog>>
}

@Database(entities = [UserAccount::class, HostEntity::class, GiftLog::class], version = 1, exportSchema = false)
abstract class DinaLiveDatabase : RoomDatabase() {
    abstract fun liveDao(): LiveDao
}

class LiveRepository(private val dao: LiveDao) {
    val userFlow: Flow<UserAccount?> = dao.getUserFlow()
    val allHostsFlow: Flow<List<HostEntity>> = dao.getAllHostsFlow()
    val recentGiftsFlow: Flow<List<GiftLog>> = dao.getRecentGiftLogsFlow()

    suspend fun getUser(): UserAccount {
        val user = dao.getUserSync()
        if (user == null) {
            val newUser = UserAccount()
            dao.insertUser(newUser)
            return newUser
        }
        // Force upgrade to Himo if coins are low or it's the old account
        if (user.coins < 4000000 || user.nickname == "الداعم المغري🇲🇦") {
            val updatedUser = user.copy(
                nickname = "الداعم الأكبر Himo 👑",
                coins = 4500000,
                xp = 350000,
                level = 95,
                isOwner = true,
                avatarId = 1,
                isRegistered = true
            )
            dao.insertUser(updatedUser)
            return updatedUser
        }
        return user
    }

    suspend fun updateUser(user: UserAccount) {
        dao.insertUser(user)
    }

    suspend fun addDiamondsToHost(hostName: String, amount: Int) {
        dao.addDiamondsToHost(hostName, amount)
    }

    suspend fun initializeHostsIfEmpty() {
        val initialHosts = listOf(
            HostEntity(1, "دنيا الأطلس 🇲🇦", "طرب وكلام زين", 25300, 15000, true, "مرحبا بيكم فالبث ديالي، كنموت عليكم!", 1),
            HostEntity(2, "شيماء بيوتي ✨", "فلوغات ونشاط", 41200, 32000, true, "يا هلا بالداعمين الغاليين، منورين ديما!", 2),
            HostEntity(3, "سلمى كوين 👑", "تحديات وألعاب", 87000, 54000, true, "التاج ديالي ما يطيحش، شكون السبع اللي غيطلع التحدي؟", 3),
            HostEntity(4, "سهام كازا 🔥", "كلشي ناشط ديما", 12500, 8900, true, "بث مباشر من البيضاء، شات ونشاط وضحك بدون حدود!", 4),
            HostEntity(5, "نهى الورود 🌹", "شعر وبوح الخواطر", 9400, 12000, true, "الكلمة الطيبة هي الهدية الحقيقية، شكرا لأرقى داعمين.", 5)
        )
        dao.insertHosts(initialHosts)
    }

    suspend fun sendGift(hostName: String, giftName: String, amount: Int): Boolean {
        val currentUser = getUser()
        if (currentUser.coins >= amount) {
            // Deduct coins & add XP (1 XP per coin spent)
            val newXp = currentUser.xp + amount
            val newLevel = calculateLevel(newXp)
            val updatedUser = currentUser.copy(
                coins = currentUser.coins - amount,
                xp = newXp,
                level = newLevel
            )
            dao.insertUser(updatedUser)

            // Add diamonds to host
            dao.addDiamondsToHost(hostName, amount)

            // Log gift
            dao.insertGiftLog(GiftLog(hostName = hostName, giftName = giftName, cost = amount))
            return true
        }
        return false
    }

    suspend fun rechargeCoins(amount: Int) {
        val currentUser = getUser()
        val updatedUser = currentUser.copy(coins = currentUser.coins + amount)
        dao.insertUser(updatedUser)
    }

    private fun calculateLevel(xp: Int): Int {
        // Level threshold increases with level: e.g. Lvl 1: 0-100xp, Lvl 2: 100-300xp, Lvl 3: 300-600xp, etc.
        var level = 1
        var threshold = 100
        var tempXp = xp
        while (tempXp >= threshold) {
            tempXp -= threshold
            level++
            threshold += 200 // Higher levels require more XP
        }
        return level
    }
}
