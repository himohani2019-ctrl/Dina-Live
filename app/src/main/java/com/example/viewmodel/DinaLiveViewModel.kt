package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.DinaLiveDatabase
import com.example.data.GiftLog
import com.example.data.HostEntity
import com.example.data.LiveRepository
import com.example.data.UserAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveGiftAnimation(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val giftIcon: String,
    val giftName: String,
    val cost: Int,
    val isLion: Boolean = false
)

data class LiveChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val text: String,
    val level: Int = 1,
    val isSystem: Boolean = false,
    val isPremium: Boolean = false,
    val avatarId: Int = 2
)

data class GiftItem(
    val name: String,
    val cost: Int,
    val icon: String,
    val description: String,
    val reaction: String
)

data class WithdrawalRequest(
    val id: String = UUID.randomUUID().toString(),
    val amountMAD: Int,
    val diamondsExchanged: Int,
    val status: String, // e.g., "تم الدفع بنجاح 🟢" / "قيد المراجعة ⏳"
    val paymentMethod: String, // "كاش بلوس Cash Plus" / "CIH Bank" / "Attijariwafa"
    val date: String,
    val referenceCode: String
)

class DinaLiveViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize Database & Repository
    private val database: DinaLiveDatabase by lazy {
        Room.databaseBuilder(
            application,
            DinaLiveDatabase::class.java,
            "dinalive_db"
        ).fallbackToDestructiveMigration().build()
    }

    val repository: LiveRepository by lazy {
        LiveRepository(database.liveDao())
    }

    // Exposed Flows from Room
    val userAccount: StateFlow<UserAccount?> by lazy {
        repository.userFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    val hosts: StateFlow<List<HostEntity>> by lazy {
        repository.allHostsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val recentGiftLogs: StateFlow<List<GiftLog>> by lazy {
        repository.recentGiftsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Stream Active State
    private val _activeHost = MutableStateFlow<HostEntity?>(null)
    val activeHost: StateFlow<HostEntity?> = _activeHost.asStateFlow()

    private val _viewerCount = MutableStateFlow(100)
    val viewerCount: StateFlow<Int> = _viewerCount.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<LiveChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<LiveChatMessage>> = _chatMessages.asStateFlow()

    // Active screen gifting animation triggers
    private val _activeGiftAnimation = MutableStateFlow<ActiveGiftAnimation?>(null)
    val activeGiftAnimation: StateFlow<ActiveGiftAnimation?> = _activeGiftAnimation.asStateFlow()

    private val _hostReactionText = MutableStateFlow<String?>(null)
    val hostReactionText: StateFlow<String?> = _hostReactionText.asStateFlow()

    // PK Battle and Moroccan Zgharit Sound effects triggers
    private val _pkBattleScore = MutableStateFlow(50)
    val pkBattleScore: StateFlow<Int> = _pkBattleScore.asStateFlow()

    private val _zgharitTrigger = MutableStateFlow<String?>(null)
    val zgharitTrigger: StateFlow<String?> = _zgharitTrigger.asStateFlow()

    // Host Earnings & Diamonds states (Addition)
    private val _hostEarnedDiamonds = MutableStateFlow(845200)
    val hostEarnedDiamonds: StateFlow<Int> = _hostEarnedDiamonds.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<WithdrawalRequest>>(listOf(
        WithdrawalRequest(
            amountMAD = 2500,
            diamondsExchanged = 250000,
            status = "تم الدفع بنجاح 🟢",
            paymentMethod = "كاش بلوس Cash Plus",
            date = "2026-05-15",
            referenceCode = "CP-824-119-09"
        ),
        WithdrawalRequest(
            amountMAD = 4000,
            diamondsExchanged = 400000,
            status = "تم الدفع بنجاح 🟢",
            paymentMethod = "CIH Bank (تحويل)",
            date = "2026-04-30",
            referenceCode = "CIH-TR-772911"
        )
    ))
    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    // Simulation Background Jobs
    private var simulationJob: Job? = null
    private var viewerCountJob: Job? = null

    val giftCatalog = listOf(
        GiftItem("وردة", 1, "🌹", "هدية بسيطة وجميلة", "شكرا على الوردة الزوينة! ربي يخليك 🌹"),
        GiftItem("براد أتاي", 20, "🍵", "أتاي مغربي منعنع مشحر", "يا سلام على براد د أتاي مغربي غزال كنشكرك بزااف! 🍵"),
        GiftItem("قلب حب", 50, "❤️", "عبر على الحب ديالك", "أويلي على قلب كنموت عليك يا سيدي! ❤️"),
        GiftItem("طوموبيل سبور", 250, "🏎️", "بسرعة فائقة فالشات", "وااو طوموبيل هادي ههه شكرا بزااف كسيري بيا! 🏎️💨"),
        GiftItem("التاج الملكي", 500, "👑", "للملوك والملكات", "التاج الملكي يا غالي منور البث ديالي كامل! 👑👑"),
        GiftItem("السبع", 1000, "🦁", "أسطورة الدعم المباشر", "السبع جاو السبع! ربي يخليك ليا يا أغلى وأكبر داعم Himo! 🦁👑😱"),
        GiftItem("خنجر الذهب", 1500, "🗡️✨", "خنجر أمازيغي عريق من الذهب", "يا سـيد الدعم! هادا خنجر الذهب الأمازيغي الحر! همة وشان يا هيمو غالي! 🗡️👑✨🇲🇦"),
        GiftItem("العمارية المغربية", 3000, "👰🏰", "زفة وهمة في اللايف", "أويلي يماااا! العمارية المغربية الملكية فقصري! ربي يحفظك يا مول السخاء Himo! 👰💍🎪🇲🇦")
    )

    private val moroccanViewerNames = listOf(
        "ياسين الكازاوي", "فاطمة_الزهراء", "أمين الرباطي", "جمال مراكش", "غيثة طنجة", 
        "VIP_سفيان", "خليل كينغ", "ليلى فلوغ", "حمزة أكادير", "سامية تيفي", 
        "أسماء الوجدية", "مراد سلا", "سارة مكناس", "سيمو تيفلت"
    )

    private val moroccanChatPhrases = listOf(
        "تبارك الله عليك كمل كتعجبني بزاف! 😍",
        "سلام خوت كاملين منورين البث ✨",
        "مستحيل هاد التحدي هههه شكون غيفوز؟",
        "ورود ورود يا شباب! 🌹🌹🌹",
        "شي أغنية زوينة الله يحفظك 🎙️",
        "DinaLive غادي يولي أحسن تطبيق فالمغرب 🇲🇦",
        "أحسن مضيفة شفتها اليوم صراحة",
        "دراري سيفطو الهدايا دعمو البث!",
        "كاين شي ترحيب بالناس الجداد؟ 👋",
        "يا ربي السلامة السبع مطلوق هاد الليلة 😂",
        "شيماء تبارك الله عليك ديما ناشطة",
        "النشااااط مع سلمى كوين كالعادة 👑"
    )

    init {
        // Initialize Database Data on start
        viewModelScope.launch {
            repository.initializeHostsIfEmpty()
            repository.getUser() // Ensures user is row is generated
        }
    }

    fun triggerZgharit() {
        viewModelScope.launch {
            _zgharitTrigger.value = "يووويووويووويووو! 💃✨🎉"
            delay(3500)
            _zgharitTrigger.value = null
        }
    }

    fun selectHost(host: HostEntity) {
        _activeHost.value = host
        _pkBattleScore.value = (40..60).random()
        
        viewModelScope.launch {
            val user = repository.getUser()
            val welcomeText = if (user.isOwner) {
                "🚨 تـرحـيـب ملكي مـهـيـب: الـداعـم الأكـبـر ومـول الـتـطـبـيـق Himo 👑 يـدخـل الـبـث الآن! 👑🚨"
            } else {
                "مرحبا بك في بث ${host.name}! المرجو احترام القوانين والأخلاق العامة."
            }
            _chatMessages.value = listOf(
                LiveChatMessage(senderName = "نظام DinaLive", text = welcomeText, level = 99, isSystem = true)
            )
            
            if (user.isOwner) {
                _hostReactionText.value = "مـرحـبـا بـسـيد الـلايف وهـمـة الـتـطـبـيـق Himo 👑! نـورتـيـنـي بـالـحـضـور الـغـالي دياالك! ❤️"
                triggerZgharit()
            } else {
                _hostReactionText.value = "مرحبا بيكم كاملين فاللايف ديالي! منورين الشات 🌟"
            }
        }
        
        _viewerCount.value = (300..1200).random()

        // Start Simulated Stream Loop
        startStreamSimulation(host)
    }

    fun leaveHost() {
        stopStreamSimulation()
        _activeHost.value = null
        _hostReactionText.value = null
        _activeGiftAnimation.value = null
        _zgharitTrigger.value = null
    }

    private fun startStreamSimulation(host: HostEntity) {
        // Cancel first
        simulationJob?.cancel()
        viewerCountJob?.cancel()

        // Simulator: simulated chat messages & PK Battle updates
        simulationJob = viewModelScope.launch {
            while (true) {
                delay((1200..3800).random().toLong())
                
                // Adjust PK battle score slightly towards the Lebanese rival (making it dynamic)
                val currentPk = _pkBattleScore.value
                val drift = if ((1..100).random() < 45) -3 else 2
                _pkBattleScore.value = (currentPk + drift).coerceIn(10, 95)
                
                // 15% chance a simulated spectator sends a gift!
                val sendMockGift = (1..100).random() < 15
                if (sendMockGift) {
                    val mockSender = moroccanViewerNames.random()
                    val randomGift = giftCatalog.take(3).random() // Mock viewers send cheaper gifts (Rose, Tea, Heart)
                    
                    // Add chat message
                    val systemGiftMessage = LiveChatMessage(
                        senderName = mockSender,
                        text = "أرسل ${randomGift.name} ${randomGift.icon}",
                        level = (1..20).random(),
                        isSystem = true,
                        avatarId = (2..10).random()
                    )
                    _chatMessages.value = _chatMessages.value.takeLast(40) + systemGiftMessage

                    // Update host diamonds (simulated)
                    repository.addDiamondsToHost(host.name, randomGift.cost)

                    // Update local active host diamonds representation for PK score / viewer support impact
                    _pkBattleScore.value = (_pkBattleScore.value + 1).coerceAtMost(100)

                    // Trigger Host Reaction text
                    _hostReactionText.value = "${host.name.split(" ")[0]}: \"${randomGift.reaction.replace("ربي يخليك", mockSender)}\""
                    
                    // Run a small screen gift animation
                    _activeGiftAnimation.value = ActiveGiftAnimation(
                        senderName = mockSender,
                        giftIcon = randomGift.icon,
                        giftName = randomGift.name,
                        cost = randomGift.cost,
                        isLion = false
                    )
                } else {
                    // Regular message
                    val mockSender = moroccanViewerNames.random()
                    val level = (1..25).random()
                    val text = moroccanChatPhrases.random()
                    val newMessage = LiveChatMessage(
                        senderName = mockSender,
                        text = text,
                        level = level,
                        isPremium = level > 12,
                        avatarId = (2..10).random()
                    )
                    _chatMessages.value = _chatMessages.value.takeLast(40) + newMessage
                }
            }
        }

        // Simulator: Fluctuating viewer count
        viewerCountJob = viewModelScope.launch {
            while (true) {
                delay((3000..6000).random().toLong())
                val change = (-25..35).random()
                _viewerCount.value = (_viewerCount.value + change).coerceAtLeast(15)
            }
        }
    }

    private fun stopStreamSimulation() {
        simulationJob?.cancel()
        viewerCountJob?.cancel()
        simulationJob = null
        viewerCountJob = null
    }

    fun sendGift(gift: GiftItem) {
        val host = _activeHost.value ?: return
        viewModelScope.launch {
            val user = repository.getUser()
            val success = repository.sendGift(host.name, gift.name, gift.cost)
            if (success) {
                // Post local chat system message
                val nameForChat = user.nickname
                val userLevel = user.level
                val userGiftMessage = LiveChatMessage(
                    senderName = nameForChat,
                    text = "أرسل ${gift.name} ${gift.icon} للـمـضـيـفـة!",
                    level = userLevel,
                    isSystem = true,
                    avatarId = user.avatarId
                )
                _chatMessages.value = _chatMessages.value.takeLast(40) + userGiftMessage

                // Show dynamic animation
                _activeGiftAnimation.value = ActiveGiftAnimation(
                    senderName = nameForChat,
                    giftIcon = gift.icon,
                    giftName = gift.name,
                    cost = gift.cost,
                    isLion = gift.cost >= 1000
                )

                // Update local PK battle score based on gift cost
                val scorePlus = when {
                    gift.cost >= 3000 -> 35
                    gift.cost >= 1500 -> 25
                    gift.cost >= 1000 -> 18
                    gift.cost >= 500 -> 12
                    gift.cost >= 250 -> 7
                    gift.cost >= 50 -> 4
                    gift.cost >= 20 -> 2
                    else -> 1
                }
                _pkBattleScore.value = (_pkBattleScore.value + scorePlus).coerceAtMost(100)

                // If high value gift sent (>= 500), auto trigger Zgharit!
                if (gift.cost >= 500) {
                    triggerZgharit()
                }

                // Trigger Vocal Host response
                _hostReactionText.value = "${host.name.split(" ")[0]}: \"${gift.reaction}\""

                // Update current cached active Host diamonds so local display syncs immediately
                _activeHost.value = _activeHost.value?.copy(
                    totalDiamonds = _activeHost.value!!.totalDiamonds + gift.cost
                )
            } else {
                // Not enough coins! System system notification message
                _chatMessages.value = _chatMessages.value + LiveChatMessage(
                    senderName = "تنبيه الشات",
                    text = "الرصيد ديالك ما كافيش باش تسيفط ${gift.name}! رجاء اشحن رصيدك ⚠️",
                    level = 1,
                    isSystem = true
                )
            }
        }
    }

    fun sendUserChatMessage(text: String) {
        val host = _activeHost.value ?: return
        viewModelScope.launch {
            val user = repository.getUser()
            val userMsg = LiveChatMessage(
                senderName = user.nickname,
                text = text,
                level = user.level,
                isPremium = user.isOwner || user.level > 10,
                avatarId = user.avatarId
            )
            _chatMessages.value = _chatMessages.value.takeLast(40) + userMsg
        }
    }

    fun rechargeCoins(amount: Int) {
        viewModelScope.launch {
            repository.rechargeCoins(amount)
        }
    }

    fun updateNickname(newName: String) {
        viewModelScope.launch {
            val user = repository.getUser()
            repository.updateUser(user.copy(nickname = newName))
        }
    }

    fun toggleRole() {
        viewModelScope.launch {
            val user = repository.getUser()
            repository.updateUser(user.copy(isHost = !user.isHost))
        }
    }

    fun claimDailyCheckin() {
        viewModelScope.launch {
            val user = repository.getUser()
            // Add 1000 Dinar free reward!
            repository.updateUser(user.copy(coins = user.coins + 1500))
        }
    }

    fun registerNewUser(nickname: String, avatarId: Int, isHost: Boolean, isOwnerSetting: Boolean) {
        viewModelScope.launch {
            val defaultCoins = if (isOwnerSetting || nickname.contains("Himo", ignoreCase = true) || nickname.contains("هيمو")) 4500000 else 5000
            val defaultLvl = if (isOwnerSetting || nickname.contains("Himo", ignoreCase = true) || nickname.contains("هيمو")) 95 else 1
            val defaultXp = if (isOwnerSetting || nickname.contains("Himo", ignoreCase = true) || nickname.contains("هيمو")) 350000 else 0
            
            val registered = UserAccount(
                id = 1,
                nickname = nickname,
                coins = defaultCoins,
                xp = defaultXp,
                level = defaultLvl,
                isHost = isHost,
                isOwner = isOwnerSetting || nickname.contains("Himo", ignoreCase = true) || nickname.contains("هيمو"),
                avatarId = avatarId,
                isRegistered = true
            )
            repository.updateUser(registered)
        }
    }

    fun updateAvatar(newAvatarId: Int) {
        viewModelScope.launch {
            val user = repository.getUser()
            repository.updateUser(user.copy(avatarId = newAvatarId))
        }
    }

    fun logout() {
        viewModelScope.launch {
            val freshUser = UserAccount(
                id = 1,
                nickname = "",
                coins = 5000,
                xp = 0,
                level = 1,
                isHost = false,
                isOwner = false,
                avatarId = 2,
                isRegistered = false
            )
            repository.updateUser(freshUser)
        }
    }

    fun clearAnimation() {
        _activeGiftAnimation.value = null
    }

    // Host Earnings Operations (Addition)
    fun withdrawHostEarnings(diamonds: Int, rate: Int, method: String, receiverDetails: String): Boolean {
        val current = _hostEarnedDiamonds.value
        if (diamonds <= 0 || current < diamonds) {
            return false
        }
        val amountMAD = diamonds / rate
        _hostEarnedDiamonds.value = current - diamonds
        val formattedDate = "2026-05-27" // current date mock
        val randomRef = "REF-" + (100000..999999).random().toString()
        val newRequest = WithdrawalRequest(
            amountMAD = amountMAD,
            diamondsExchanged = diamonds,
            status = "قيد المراجعة ⏳",
            paymentMethod = method,
            date = formattedDate,
            referenceCode = randomRef
        )
        _withdrawalRequests.value = listOf(newRequest) + _withdrawalRequests.value
        return true
    }

    fun simulateLiveStreamGiftReceived(diamonds: Int) {
        _hostEarnedDiamonds.value = _hostEarnedDiamonds.value + diamonds
    }

    override fun onCleared() {
        super.onCleared()
        stopStreamSimulation()
    }
}
