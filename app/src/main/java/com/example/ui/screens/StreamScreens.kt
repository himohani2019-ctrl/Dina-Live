package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HostEntity
import com.example.ui.theme.*
import com.example.viewmodel.ActiveGiftAnimation
import com.example.viewmodel.DinaLiveViewModel
import com.example.viewmodel.GiftItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinaLiveMainApp(viewModel: DinaLiveViewModel) {
    val activeHost by viewModel.activeHost.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LiveBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (userAccount == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrightPink)
                }
            } else if (!userAccount!!.isRegistered) {
                RegistrationScreen(viewModel = viewModel)
            } else if (activeHost != null) {
                // If a host resides in state, render the full-screen interactive livestream room
                LiveStreamRoom(
                    viewModel = viewModel,
                    host = activeHost!!,
                    onClose = { viewModel.leaveHost() }
                )
            } else {
                // Normal Dashboard Mode
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = CardPurple,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("main_navigation")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrightPink,
                                    unselectedIconColor = MutedText,
                                    indicatorColor = GlassWhite
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                                        contentDescription = "الرئيسية"
                                    )
                                },
                                label = { Text("الرئيسية", color = if (selectedTab == 0) BrightPink else MutedText, fontSize = 11.sp) }
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Gold,
                                    unselectedIconColor = MutedText,
                                    indicatorColor = GlassWhite
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Default.EmojiEvents else Icons.Outlined.EmojiEvents,
                                        contentDescription = "الترتيب"
                                    )
                                },
                                label = { Text("البورد", color = if (selectedTab == 1) Gold else MutedText, fontSize = 11.sp) }
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    unselectedIconColor = MutedText,
                                    indicatorColor = GlassWhite
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Default.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "المحفظة"
                                    )
                                },
                                label = { Text("المحفظة", color = if (selectedTab == 2) NeonCyan else MutedText, fontSize = 11.sp) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> ExploreScreen(viewModel)
                            1 -> LeaderboardScreen(viewModel)
                            2 -> ProfileScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ============== SUB-SCREEN: EXPLORE HUB =============
@Composable
fun ExploreScreen(viewModel: DinaLiveViewModel) {
    val hosts by viewModel.hosts.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    
    val pulseState = rememberInfiniteTransition(label = "live_pulse")
    val alphaAnim by pulseState.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1500 },
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "DinaLive دينا لايف",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightPink
                )
                Text(
                    text = "عالم المضيفات والداعمين المباشر 🇲🇦",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }

            // Simple User Coin Widget quick link
            userAccount?.let {
                Row(
                    modifier = Modifier
                        .background(CardPurple, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Coins",
                        tint = Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${it.coins}",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("دينار", color = Gold, fontSize = 10.sp)
                }
            }
        }

        // Daily greeting banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("explore_banner"),
            colors = CardDefaults.cardColors(containerColor = CardPurple),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                userAccount?.let { user ->
                    MoroccanAvatar(avatarId = user.avatarId, size = 48.dp)
                } ?: Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Brush.linearGradient(listOf(BrightPink, HotOrange)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مرحبا بك يا ${userAccount?.nickname ?: "داعم ديما"}!",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "دعم المضيفات ديالك المفضلين بالهدايا واطلع فالترتيب الملكي 👑",
                        color = MutedText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Green.copy(alpha = alphaAnim), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "المضيفات المتصلات حالياً (لايف)",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Hosts Grid
        if (hosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrightPink)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("hosts_grid"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hosts) { host ->
                    HostCard(host = host, onClick = { viewModel.selectHost(host) })
                }
            }
        }
    }
}

@Composable
fun HostCard(host: HostEntity, onClick: () -> Unit) {
    // Elegant container design mimicking SuperLive Streamer list item
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick)
            .testTag("host_card_${host.id}"),
        colors = CardDefaults.cardColors(containerColor = CardPurple),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Dynamic Host Abstract Avatar Background drawing (Luxury Aura instead of thin image placeholders)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val colorsList = when (host.avatarId) {
                    1 -> listOf(Color(0xFF8A2BE2), Color(0xFFFF1493))
                    2 -> listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
                    3 -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
                    4 -> listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                    else -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
                }
                drawRect(
                    brush = Brush.radialGradient(
                        colors = colorsList,
                        center = Offset(size.width / 2, size.height * 0.4f),
                        radius = size.width * 0.9f
                    )
                )
            }

            // Shading Overlay to make text legible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Live Badge Emitter
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(BrightPink, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(PureWhite, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "بث مباشر",
                    color = PureWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Diamonds Metric
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Diamonds",
                    tint = Gold,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${host.totalDiamonds}",
                    color = Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Profile Stack
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                // Host Initial Accent Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PureWhite.copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = host.name.substring(0, 1),
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = host.name,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "# " + host.tag,
                    color = Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "${host.followers} متابع",
                    color = MutedText,
                    fontSize = 9.sp
                )
            }
        }
    }
}


// ============== SUB-SCREEN: GLOBAL LEADERBOARD =============
@Composable
fun LeaderboardScreen(viewModel: DinaLiveViewModel) {
    val hosts by viewModel.hosts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Broad Header
        Text(
            text = "لائحة الشهرة والداعمين 🏆",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "المضيفات الأكثر شعبية وتلقياً للهدايا من الداعمين",
            fontSize = 12.sp,
            color = MutedText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (hosts.size >= 3) {
            // Podium drawing for top 3 hosts
            val sortedHosts = hosts.sortedByDescending { it.totalDiamonds }
            val first = sortedHosts.getOrNull(0)
            val second = sortedHosts.getOrNull(1)
            val third = sortedHosts.getOrNull(2)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 2nd Place (Left)
                second?.let {
                    PodiumSpot(
                        host = it,
                        rank = 2,
                        podiumHeight = 90.dp,
                        accentColor = Color(0xFFC0C0C0),
                        onClick = { viewModel.selectHost(it) }
                    )
                }

                // 1st Place (Center)
                first?.let {
                    PodiumSpot(
                        host = it,
                        rank = 1,
                        podiumHeight = 130.dp,
                        accentColor = Gold,
                        onClick = { viewModel.selectHost(it) }
                    )
                }

                // 3rd Place (Right)
                third?.let {
                    PodiumSpot(
                        host = it,
                        rank = 3,
                        podiumHeight = 70.dp,
                        accentColor = Color(0xFFCD7F32),
                        onClick = { viewModel.selectHost(it) }
                    )
                }
            }

            // Rest list
            Text(
                text = "باقي الترتيب العام",
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("leaderboard_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedHosts.drop(3)) { host ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardPurple, RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectHost(host) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sortedHosts.indexOf(host) + 1}",
                            color = MutedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.width(28.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BrightPink.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, BrightPink.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = host.name.substring(0, 1),
                                color = BrightPink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = host.name,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = host.tag,
                                color = MutedText,
                                fontSize = 10.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = "Sim",
                                tint = Gold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${host.totalDiamonds}",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد مـضيفات ببيانات كافية حالياً", color = MutedText)
            }
        }
    }
}

@Composable
fun PodiumSpot(
    host: HostEntity,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        // Host Circle
        Box(contentAlignment = Alignment.TopCenter) {
            // Crown for #1
            if (rank == 1) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Rank 1 Key",
                    tint = Gold,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(y = (-14).dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(CardPurple, CircleShape)
                    .border(2.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = host.name.substring(0, 1),
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = host.name,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${host.totalDiamonds}",
                color = Gold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Visual Block Column represent podium standard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.5f),
                            CardPurple
                        )
                    ),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
        }
    }
}


// ============== SUB-SCREEN: DECORATED PROFILE & WALLET =============
@Composable
fun ProfileScreen(viewModel: DinaLiveViewModel) {
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    var nicknameState by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // Segmented tabs state
    var activeTab by remember { mutableStateOf(0) } // 0: Recharge Coins, 1: Host Earnings / Salaries

    // Secure recharge variables
    var showRechargeGateway by remember { mutableStateOf<Pair<Int, Int>?>(null) } // Pair(Coins Granted, Cost in MAD)
    var selectedGatewayMode by remember { mutableStateOf(0) } // 0: Visa/Mastercard, 1: Cash Plus
    
    // Secure Payment Form Input fields
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCcv by remember { mutableStateOf("") }
    
    // Secure Payment Flow steps
    var paymentStep by remember { mutableStateOf(0) } // 0: Card input, 1: Securing CMI 3D Secure link, 2: OTP verification, 3: Completed Success
    var simulatedOtpCode by remember { mutableStateOf("") }
    var cardVerificationError by remember { mutableStateOf("") }
    var randomCashPlusRef by remember { mutableStateOf("CP-824-712-901") }
    var showCashPlusReceipt by remember { mutableStateOf(false) }

    // Host earnings & Withdrawal variables
    val hostEarnedDiamonds by viewModel.hostEarnedDiamonds.collectAsStateWithLifecycle()
    val withdrawalRequests by viewModel.withdrawalRequests.collectAsStateWithLifecycle()
    
    var showWithdrawPortal by remember { mutableStateOf(false) }
    var withdrawAmountCoins by remember { mutableStateOf("") }
    var withdrawMethod by remember { mutableStateOf("CIH Bank") } // CIH Bank / Attijariwafa / Cash Plus
    var withdrawFullName by remember { mutableStateOf("") }
    var withdrawIdNumber by remember { mutableStateOf("") } // CNIE for Cash Plus
    var withdrawPhone by remember { mutableStateOf("") } // SMS target phone for Cash Plus
    var withdrawRib by remember { mutableStateOf("") } // RIB for banks
    var withdrawSuccessMsg by remember { mutableStateOf<String?>(null) }
    var withdrawErrorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userAccount) {
        userAccount?.let {
            nicknameState = it.nickname
        }
    }

    Scaffold(
        containerColor = LiveBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "المحفظة والحساب 💰",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            userAccount?.let { user ->
                // User Details Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        Brush.sweepGradient(listOf(BrightPink, HotOrange, NeonCyan)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(CardPurple, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💎",
                                        fontSize = 24.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.nickname,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit name",
                                        tint = MutedText,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { showDialog = true }
                                    )
                                }

                                if (user.isOwner) {
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 4.dp)
                                            .background(
                                                Brush.horizontalGradient(listOf(Gold, HotOrange)),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, PureWhite.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = PureWhite,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "مـول الـتـطـبـيـق والـداعم الأصـلـي 👑",
                                                color = PureWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(BrightPink, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "مستوى الداعم ${user.level}",
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${user.xp} XP تفاعل",
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== CULTURAL TAB SELECTOR SEGMENT ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .background(CardPurple.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .border(1.dp, GlassWhite.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    val tabItems = listOf("شـحـن كـويـنـزات 💳🇲🇦", "أربـاح الـمـضـيـفـات 💰📊")
                    tabItems.forEachIndexed { idx, title ->
                        val isSel = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isSel) Modifier.background(Brush.horizontalGradient(listOf(BrightPink, HotOrange)))
                                    else Modifier
                                )
                                .clickable { activeTab = idx }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSel) PureWhite else MutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==================== TAB CONTENT SWITCHING ====================
                if (activeTab == 0) {
                    // TAB 0: COINS STORE RECHARGE (VISA, MASTERCARD & CASH PLUS)
                    Column(modifier = Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardPurple.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("رصيدك الحالي من الكوينزات", color = MutedText, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${user.coins}",
                                            color = Gold,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("دينار ذهبي", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.claimDailyCheckin() },
                                    colors = ButtonDefaults.buttonColors(containerColor = HotOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("هدية يومية 🎁", fontSize = 11.sp)
                                }
                            }
                        }

                        Text(
                            text = "باقات الشحن المغربية وعروض دينا لايف ✨",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Recharge Packages List
                        val storeOffers = listOf(
                            Triple(1000, 10, "1,000 Dinar (التجربة السريعة)"),
                            Triple(3000, 25, "3,000 Dinar (العرض الاقتصادي)"),
                            Triple(7000, 50, "7,000 Dinar (رتبة الملوك المتميزة)"),
                            Triple(15000, 100, "15,000 Dinar (الأسد الأسطوري المغربي 🦁)"),
                            Triple(50000, 300, "50,000 Dinar (الخزنة الملكية الكبرى 👑)")
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(storeOffers) { offer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardPurple, RoundedCornerShape(16.dp))
                                        .border(0.5.dp, GlassWhite, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = offer.third,
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("السعر بالدرهم: ", color = MutedText, fontSize = 11.sp)
                                            Text("${offer.second} MAD", color = NeonCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("• آمن 100%", color = Gold, fontSize = 10.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            cardNumber = ""
                                            cardExpiry = ""
                                            cardCcv = ""
                                            cardHolder = user.nickname
                                            paymentStep = 0
                                            simulatedOtpCode = ""
                                            cardVerificationError = ""
                                            // Generate distinct Cash Plus reference for this specific invoice
                                            randomCashPlusRef = "CP-${(100..999).random()}-${(100..999).random()}-${(100..999).random()}"
                                            showRechargeGateway = Pair(offer.first, offer.second)
                                            selectedGatewayMode = 0 // default Visa/MC
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("شـحن 💳", color = LiveBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: HOST STREAMING EARNINGS & SALARIES (أرباح المضيفات ورواتب كاش بلوس والروابط البنكية)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardPurple),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, BrightPink.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "لوحة حساب أرباح المضيفات (شهرياً) 📊",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "تجمع المضيفات هدايا الجمهور وتحولها للراتب بالدرهم المغربي حسب صيغة 100 كوينز كرت = 1 درهم.",
                                    color = MutedText,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("رصيد الهدايا الحالية", color = MutedText, fontSize = 11.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = String.format("%,d", hostEarnedDiamonds),
                                                color = BrightPink,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("كوينز", color = BrightPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المقابل بالدرهم المغربي", color = MutedText, fontSize = 11.sp)
                                        val madEquivalent = hostEarnedDiamonds / 100
                                        Text(
                                            text = "${String.format("%,d", madEquivalent)} درهم",
                                            color = Gold,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Dynamic Target milestone progress indicator
                                val targetTier1 = 200000 // Basic
                                val targetTier2 = 500000 // Gold
                                val targetTier3 = 1000000 // Royal
                                
                                val activeTarget = if (hostEarnedDiamonds < targetTier1) targetTier1 
                                                   else if (hostEarnedDiamonds < targetTier2) targetTier2 
                                                   else targetTier3
                                
                                val activeTargetLabel = if (activeTarget == targetTier1) "هدف الباقة الأساسية (2,000 درهم)"
                                                        else if (activeTarget == targetTier2) "هدف الباقة الذهبية (5,000 درهم) 👑"
                                                        else "هدف الباقة الملكية الأسطورية (10,000 درهم) 💎"
                                
                                val progressRatio = (hostEarnedDiamonds.toFloat() / activeTarget.toFloat()).coerceIn(0f, 1f)
                                
                                Text(
                                    text = "$activeTargetLabel: (${(progressRatio * 100).toInt()}%)",
                                    color = PureWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = HotOrange,
                                    trackColor = LiveBackground
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Simulation Booster Button to easily test
                                    Button(
                                        onClick = {
                                            viewModel.simulateLiveStreamGiftReceived(100000)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LiveBackground),
                                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        Text("تلقي دعم محاكاة 🎁 (+100k)", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Real withdrawal trigger
                                    Button(
                                        onClick = {
                                            withdrawSuccessMsg = null
                                            withdrawErrorMsg = null
                                            withdrawAmountCoins = hostEarnedDiamonds.toString()
                                            withdrawFullName = user.nickname
                                            showWithdrawPortal = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrightPink),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Text("سحب الراتب 💸", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Moroccan Financial Agencies Information Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = LiveBackground.copy(alpha = 0.5f)),
                            border = BorderStroke(0.5.dp, GlassWhite.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🇲🇦", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("شروط الرواتب والشركاء الرسميين بالملكة :", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(
                                    text = "• شريك الدفع الفوري النقدي: وكالة Cash Plus وعبر فروعها الممتدة بالمدن والقرى المغربية.\n" +
                                            "• التحويل البنكي المباشر: CIH Bank, Attijariwafa Bank, Banque Populaire.\n" +
                                            "• دورة المعالجة الأساسية: تسوى الأرباح شهرياً ويتم مراجعة الطلبات والتحويل بين 25 و 30 من كل شهر ميلادي.",
                                    color = MutedText,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // PAYOUTS HISTORY SECTION
                        Text(
                            text = "سجل رواتب المضيفة وتوزيعات الدفع 📜",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        if (withdrawalRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardPurple, RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد طلبات سحب راتب مسبقة بعد.", color = MutedText, fontSize = 11.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                withdrawalRequests.forEach { req ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CardPurple, RoundedCornerShape(14.dp))
                                            .border(0.5.dp, GlassWhite.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${String.format("%,d", req.amountMAD)} درهم",
                                                    color = Gold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (req.status.contains("نجاح")) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                            else Color(0xFFFF9800).copy(alpha = 0.2f),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = req.status,
                                                        color = if (req.status.contains("نجاح")) Color(0xFF81C784) else Color(0xFFFFB74D),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "الوسيلة: ${req.paymentMethod} • مرجع: ${req.referenceCode}",
                                                color = MutedText,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "-${String.format("%,d", req.diamondsExchanged)} كوينز",
                                                color = BrightPink,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(req.date, color = MutedText, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 1: MOROCCAN SECURE RECHARGE GATEWAY WITH SIMULATED CMI / VISA / MC & CASH PLUS
    // ==========================================
    showRechargeGateway?.let { gatewayData ->
        val coinsGranted = gatewayData.first
        val costMAD = gatewayData.second

        Dialog(onDismissRequest = { showRechargeGateway = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardPurple),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, BrightPink.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Gateway header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بوابة الدفع الآمنة المغربية 🔒🇲🇦",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { showRechargeGateway = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
                        }
                    }

                    Text(
                        text = "تفاصيل العملية: شحن $coinsGranted كوينز بمبلغ $costMAD درهم مغربي",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Inner dialog selector (Visa/Mastercard vs Cash Plus)
                    if (paymentStep < 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LiveBackground, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            listOf("بطاقة مصرفية 💳", "وكالة كاش بلوس 💛").forEachIndexed { idx, title ->
                                val selected = selectedGatewayMode == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) BrightPink else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedGatewayMode = idx; paymentStep = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // MODE 0: BANK CARD SECURE GATEWAY (VIA CMI SIMULATION)
                    if (selectedGatewayMode == 0) {
                        when (paymentStep) {
                            0 -> { // STEP 0: CARD FIELDS ENTRY
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Visual card design display (cyberpunk neon design)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(
                                                Brush.linearGradient(listOf(BrightPink, HotOrange)),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .border(1.dp, PureWhite.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("DINA LIVE BANK", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("VISA / MC", color = PureWhite.copy(alpha = 0.8f), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                            }
                                            Text(
                                                text = if (cardNumber.isBlank()) "•••• •••• •••• ••••" else cardNumber.chunked(4).joinToString(" "),
                                                color = PureWhite,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Haimo : ${cardHolder.take(15)}", color = PureWhite, fontSize = 9.sp)
                                                Text("Exp: ${if (cardExpiry.isBlank()) "MM/YY" else cardExpiry}", color = PureWhite, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Real Secure Inputs
                                    OutlinedTextField(
                                        value = cardNumber,
                                        onValueChange = { if (it.length <= 16) cardNumber = it.filter { c -> c.isDigit() } },
                                        label = { Text("رقم البطاقة الوطنية / البنكية (16 رقم)", color = MutedText, fontSize = 11.sp) },
                                        placeholder = { Text("4514300011112222", color = MutedText.copy(alpha = 0.4f)) },
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = PureWhite,
                                            focusedContainerColor = LiveBackground,
                                            unfocusedContainerColor = LiveBackground
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = cardExpiry,
                                            onValueChange = { if (it.length <= 5) cardExpiry = it },
                                            placeholder = { Text("12/29", color = MutedText.copy(alpha = 0.4f)) },
                                            label = { Text("تاريخ الصلاحية (MM/YY)", color = MutedText, fontSize = 10.sp) },
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = PureWhite,
                                                unfocusedTextColor = PureWhite,
                                                focusedContainerColor = LiveBackground,
                                                unfocusedContainerColor = LiveBackground
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = cardCcv,
                                            onValueChange = { if (it.length <= 3) cardCcv = it.filter { c -> c.isDigit() } },
                                            placeholder = { Text("123", color = MutedText.copy(alpha = 0.4f)) },
                                            label = { Text("الرمز السري (CCV)", color = MutedText, fontSize = 10.sp) },
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = PureWhite,
                                                unfocusedTextColor = PureWhite,
                                                focusedContainerColor = LiveBackground,
                                                unfocusedContainerColor = LiveBackground
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (cardVerificationError.isNotBlank()) {
                                        Text(cardVerificationError, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            if (cardNumber.length < 16 || cardCcv.length < 3 || cardExpiry.length < 4) {
                                                cardVerificationError = "المرجو كتابة معلومات بطاقة مغربية صحيحة ومكتملة ⚠️"
                                            } else {
                                                cardVerificationError = ""
                                                paymentStep = 1
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("أداء آمن عن طريق CMI 🔏", color = LiveBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            1 -> { // STEP 1: SECUR-CMI 3D SECURE MODAL SIMULATOR LAUNCHER
                                LaunchedEffect(Unit) {
                                    delay(2000)
                                    paymentStep = 2 // Move to OTP
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    CircularProgressIndicator(color = NeonCyan)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        "جاري التحقق وتوجيه المعاملة للقناة الآمنة...",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "نحن نستخدم بروتوكول CMI 3D Secure بالتعاون مع البنك المركزي المغربي.",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            2 -> { // STEP 2: SECURE OTP ENTRY
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "التحقق من هوية الدفع بالبطاقة 🛡️",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "تم إرسال كود التحقق OTP المكون من 4 أرقام في رسالة نصية قصيرة SMS لهاتفك المسجل بالبنك المغربي لضمان سلامة حسابك.",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = simulatedOtpCode,
                                        onValueChange = { textValue -> if (textValue.length <= 4) simulatedOtpCode = textValue.filter { c -> c.isDigit() } },
                                        placeholder = { Text("مثال: 9021", color = MutedText.copy(alpha = 0.4f)) },
                                        label = { Text("أدخل رمز الهاتف المستلم", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = PureWhite,
                                            focusedContainerColor = LiveBackground,
                                            unfocusedContainerColor = LiveBackground
                                        ),
                                        modifier = Modifier.width(180.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    )

                                    if (cardVerificationError.isNotBlank()) {
                                        Text(cardVerificationError, color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                if (simulatedOtpCode.length < 4) {
                                                    cardVerificationError = "المرجو إدخال كود OTP صالح ذو 4 أرقام ⚠️"
                                                } else {
                                                    cardVerificationError = ""
                                                    viewModel.rechargeCoins(coinsGranted)
                                                    paymentStep = 3 // Success!
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("تأكيد وخصم الرصيد 🔐", color = LiveBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { paymentStep = 0 },
                                            colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("تراجع", color = PureWhite, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            3 -> { // STEP 3: CONGRATULATION PAYMENT RECHARGE COMPLETED
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🎉", fontSize = 54.sp)
                                    Text(
                                        text = "تم الشحن بنجاح تام! 👑",
                                        color = Gold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "تم شحن رصيدك فوراً بمقدار $coinsGranted كوينز ذهبي. شكراً على اختيارك دينا لايف المباشر ودعمك للمضيفين بالنشاط والنشامى!",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )

                                    Button(
                                        onClick = { showRechargeGateway = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrightPink),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("الرجوع للمحفظة 💰", color = PureWhite, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // MODE 1: CASH PLUS MOROCCAN OFFICE PAYMENT PROTOCOL
                        if (!showCashPlusReceipt) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("💛", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "كود الإيداع الفوري لوكالة كاش بلوس :",
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = randomCashPlusRef,
                                            color = PureWhite,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        )
                                        Text(
                                            text = "قدم هذا الكود لوكيل كاش بلوس عند زيارتك لشحن حسابك وتجربة الدعم الفوري.",
                                            color = MutedText,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "خطوات الشحن النقدي بالوكالة 📌:",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )

                                Text(
                                    text = "1. تفضل بزيارة أقرب نقطة بيع أو وكالة كاش بلوس Cash Plus بالمملكة.\n" +
                                            "2. قدم كود الإيداع المرجعي أعلاه للوكيل الدفع.\n" +
                                            "3. تسليم قيمة الفاتورة وهي ($costMAD درهم مغربي) نقداً للوكالة.\n" +
                                            "4. سيقوم الخادم بربط العملية أوتوماتيكياً تزامناً مع دفعك وتلقي كوينزاتك.",
                                    color = MutedText,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.rechargeCoins(coinsGranted)
                                            showCashPlusReceipt = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrightPink),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("محاكاة دفع بالوكالة فورياً 💰", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = { showRechargeGateway = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.8f)
                                    ) {
                                        Text("رجوع", color = PureWhite, fontSize = 10.sp)
                                    }
                                }
                            }
                        } else {
                            // SHOW SIMULATED INDIVIDUAL BILL RECEIPT
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardPurple, RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Text("🧾", fontSize = 48.sp)
                                Text("كاش بلوس - وصل الدفع الرسمي", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("تم الدفع بنجاح وتحصيل الرصيد ☑️", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("رقم مرجع المعاملة:", color = MutedText, fontSize = 10.sp)
                                        Text(randomCashPlusRef, color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("تاريخ وأوان التحويل:", color = MutedText, fontSize = 10.sp)
                                        Text("2026-05-27 01:10", color = PureWhite, fontSize = 10.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المستفيد النهائي:", color = MutedText, fontSize = 10.sp)
                                        Text("مول الطبيق Himo (Dina Live)", color = PureWhite, fontSize = 10.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("الكوينزات المستلمة:", color = MutedText, fontSize = 10.sp)
                                        Text("+$coinsGranted دينار ذهبي", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("القيمة المسددة نقداً:", color = MutedText, fontSize = 10.sp)
                                        Text("$costMAD درهم مغربي (MAD)", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        showCashPlusReceipt = false
                                        showRechargeGateway = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("حفظ وإغلاق الوصل 📄", color = LiveBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 2: HOST WITHDRAWAL AND MONTHLY SALARY SUBMISSION PORTAL
    // ==========================================
    if (showWithdrawPortal) {
        Dialog(onDismissRequest = { showWithdrawPortal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardPurple),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "طلب سحب الربح وتحويل راتب المضيفة 💸",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showWithdrawPortal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
                        }
                    }

                    Text(
                        text = "تفاصيل الحساب: رصيدك الحالي من هدايا اللايف هو ${String.format("%,d", hostEarnedDiamonds)} كوينز يعادل ${hostEarnedDiamonds / 100} درهم.",
                        color = MutedText,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Error & Success indicators
                    withdrawSuccessMsg?.let {
                        Text(it, color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    withdrawErrorMsg?.let {
                        Text(it, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }

                    // Bank & Cash Plus Methods Tab Row within dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LiveBackground, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        val methods = listOf("CIH Bank", "Attijariwafa", "Cash Plus")
                        methods.forEach { m ->
                            val isSel = withdrawMethod == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) BrightPink else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { 
                                        withdrawMethod = m
                                        withdrawSuccessMsg = null
                                        withdrawErrorMsg = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = m,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fields based on selected method
                    OutlinedTextField(
                        value = withdrawFullName,
                        onValueChange = { withdrawFullName = it },
                        label = { Text("الاسم الكامل للمستفيد (بالأحرف اللاتينية)", color = MutedText, fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = LiveBackground,
                            unfocusedContainerColor = LiveBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (withdrawMethod == "Cash Plus") {
                        OutlinedTextField(
                            value = withdrawIdNumber,
                            onValueChange = { withdrawIdNumber = it },
                            label = { Text("رقم البطاقة الوطنية للتعريف (CNIE)", color = MutedText, fontSize = 11.sp) },
                            placeholder = { Text("مثال: BE190288", color = MutedText.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = LiveBackground,
                                unfocusedContainerColor = LiveBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = withdrawPhone,
                            onValueChange = { withdrawPhone = it },
                            label = { Text("رقم الهاتف المغربي (لتلقي رسالة السحب SMS)", color = MutedText, fontSize = 11.sp) },
                            placeholder = { Text("0612345678", color = MutedText.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = LiveBackground,
                                unfocusedContainerColor = LiveBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = withdrawRib,
                            onValueChange = { if (it.length <= 24) withdrawRib = it.filter { c -> c.isDigit() } },
                            label = { Text("رقم الحساب البنكي R.I.B (24 رقماً)", color = MutedText, fontSize = 11.sp) },
                            placeholder = { Text("230 011 00021980004122 ...", color = MutedText.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = LiveBackground,
                                unfocusedContainerColor = LiveBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = withdrawAmountCoins,
                        onValueChange = { withdrawAmountCoins = it.filter { c -> c.isDigit() } },
                        label = { Text("عدد الكوينزات المراد صرفها (أقل شيء 50,000 كوينز)", color = MutedText, fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = LiveBackground,
                            unfocusedContainerColor = LiveBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick calculation metrics
                    val requestedCoins = withdrawAmountCoins.toIntOrNull() ?: 0
                    if (requestedCoins > 0) {
                        val requestedMAD = requestedCoins / 100
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LiveBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المبلغ المحول بالدرهم المغبي:", color = MutedText, fontSize = 11.sp)
                                Text("$requestedMAD درهم", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val coins = withdrawAmountCoins.toIntOrNull() ?: 0
                                if (coins < 50000) {
                                    withdrawErrorMsg = "عذراً، أقل حد أدنى لسحب الرواتب والربح في دينا لايف هو 50,000 كوينز هدايا ⚠️"
                                    withdrawSuccessMsg = null
                                } else if (coins > hostEarnedDiamonds) {
                                    withdrawErrorMsg = "عذراً عتبة رصيد الهدايا الحالية لديك غير كافية لإتمام هذا الطلب ⚠️"
                                    withdrawSuccessMsg = null
                                } else if (withdrawFullName.isBlank() || (withdrawMethod != "Cash Plus" && withdrawRib.length < 24) || (withdrawMethod == "Cash Plus" && (withdrawIdNumber.isBlank() || withdrawPhone.isBlank()))) {
                                    withdrawErrorMsg = "المرجو تعبئة كافة الحقول والمعلومات البنكية أو الهوياتية بدقة لتفادي رفض الملف ⚠️"
                                    withdrawSuccessMsg = null
                                } else {
                                    val methodDetails = if (withdrawMethod == "Cash Plus") "CNIE: $withdrawIdNumber • Phone: $withdrawPhone" else "RIB: $withdrawRib"
                                    val wasSuccess = viewModel.withdrawHostEarnings(
                                        diamonds = coins,
                                        rate = 100,
                                        method = "$withdrawMethod (طلب تحويل)",
                                        receiverDetails = "Beneficiary: $withdrawFullName • $methodDetails"
                                    )
                                    if (wasSuccess) {
                                        withdrawSuccessMsg = "تم تسجيل طلب تحويل راتبك بنجاح! 🟢 ستتم معالجته وتحويله في أقرب وقت."
                                        withdrawErrorMsg = null
                                    } else {
                                        withdrawErrorMsg = "فشل في تسجيل المعاملة، يرجى المحاولة لاحقاً."
                                        withdrawSuccessMsg = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("تأكيد طلب السحب والتسجيل 🔒", color = LiveBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showWithdrawPortal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("تراجع", color = PureWhite, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Name Editor dialog
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تبديل اللقب المغربي ✏️",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nicknameState,
                        onValueChange = { nicknameState = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nickname_input"),
                        label = { Text("اللقب المفضل عندك", color = MutedText) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = LiveBackground,
                            unfocusedContainerColor = LiveBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("رجوع", color = MutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nicknameState.isNotBlank()) {
                                    viewModel.updateNickname(nicknameState)
                                }
                                showDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightPink)
                        ) {
                            Text("حفظ اللقب")
                        }
                    }
                }
            }
        }
    }
}


// ============== LIVESTREAMING FULLROOM SCREEN =============
@Composable
fun LiveStreamRoom(
    viewModel: DinaLiveViewModel,
    host: HostEntity,
    onClose: () -> Unit
) {
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val viewersCount by viewModel.viewerCount.collectAsStateWithLifecycle()
    val activeGiftAnim by viewModel.activeGiftAnimation.collectAsStateWithLifecycle()
    val hostSpeechBubble by viewModel.hostReactionText.collectAsStateWithLifecycle()
    val pkBattleScore by viewModel.pkBattleScore.collectAsStateWithLifecycle()
    val zgharitTrigger by viewModel.zgharitTrigger.collectAsStateWithLifecycle()
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showGiftSheet by remember { mutableStateOf(false) }

    // Dialog & Profile state extensions
    var showProfilePopup by remember { mutableStateOf(false) }
    var popupName by remember { mutableStateOf("") }
    var popupAvatarId by remember { mutableStateOf(2) }
    var popupLevel by remember { mutableStateOf(1) }
    var popupBio by remember { mutableStateOf("") }
    var popupDiamonds by remember { mutableStateOf(120) }
    var popupFollowers by remember { mutableStateOf(1500) }
    var popupIsHost by remember { mutableStateOf(false) }

    var showChatInputDialog by remember { mutableStateOf(false) }
    var customChatText by remember { mutableStateOf("") }

    // Beauty Filters state variables (Addition)
    var activeCameraFilterIndex by remember { mutableStateOf(0) }
    var showFilterSelector by remember { mutableStateOf(false) }
    var simulatedToastMessage by remember { mutableStateOf<String?>(null) }

    // Screen shaking triggers when the LION is sent
    val shakeTrigger = remember { Animatable(0f) }
    
    // Auto Scroll Comments to Bottom
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    // Trigger Screen Shake if a gifted SUNG is Lion
    LaunchedEffect(activeGiftAnim) {
        if (activeGiftAnim != null && activeGiftAnim!!.isLion) {
            scope.launch {
                // Shake back and forth
                repeat(8) {
                    shakeTrigger.animateTo(12f, spring(dampingRatio = 0.2f))
                    shakeTrigger.animateTo(-12f, spring(dampingRatio = 0.2f))
                }
                shakeTrigger.animateTo(0f, spring())
                
                // Clear state
                delay(3000)
                viewModel.clearAnimation()
            }
        } else if (activeGiftAnim != null) {
            scope.launch {
                delay(3500)
                viewModel.clearAnimation()
            }
        }
    }

    LaunchedEffect(simulatedToastMessage) {
        if (simulatedToastMessage != null) {
            delay(2000)
            simulatedToastMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveBackground)
            .graphicsLayer {
                // Shift screen layout slightly dynamically under lion power
                translationX = shakeTrigger.value
                translationY = shakeTrigger.value * 0.7f
            }
    ) {
        // 1. Live Camera Simulation Background Graphics
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseColors = when (host.avatarId) {
                    1 -> listOf(Color(0xFF2E0854), Color(0xFF140D36), Color(0xFF030008))
                    2 -> listOf(Color(0xFF0B3A4D), Color(0xFF051C2C), Color(0xFF01020F))
                    3 -> listOf(Color(0xFF5E0B25), Color(0xFF2A0311), Color(0xFF060004))
                    4 -> listOf(Color(0xFF552200), Color(0xFF220900), Color(0xFF080200))
                    else -> listOf(Color(0xFF0B4228), Color(0xFF031A0F), Color(0xFF010403))
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = baseColors
                    )
                )
            }

            // Animated breathing camera lens flare ring and status particle
            val breathingState = rememberInfiniteTransition(label = "breathing")
            val radiusScale by breathingState.animateFloat(
                initialValue = 0.82f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breath_radius"
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.Center)
                    .scale(radiusScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BrightPink.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Huge centered host avatar with beautiful glowing aura
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-30).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(CardPurple, CircleShape)
                        .border(3.dp, Brush.linearGradient(listOf(BrightPink, Gold)), CircleShape)
                        .shadow(16.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = host.name.substring(0, 1),
                        color = Gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 38.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = host.name,
                    color = PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "# " + host.tag,
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Real-time camera beauty filter overlay (Addition)
            CameraFilterOverlay(filterId = activeCameraFilterIndex)
        }

        // 2. Top Stream Headers Overlay & PK Battle Arena (Addition)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Host Badge details
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .clickable {
                            popupName = host.name
                            popupAvatarId = 3 // Kaftan Dress represent host
                            popupBio = "المضيفة المبدعة ${host.name} تقدم لكم أفضل بث مباشر للموسيقى والنشاط الثقافي المغربي الأصيل في دينا لايف! مرحباً بالجميع 🎤🇲🇦"
                            popupDiamonds = host.totalDiamonds
                            popupFollowers = 54800
                            popupIsHost = true
                            popupLevel = 18
                            showProfilePopup = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoroccanAvatar(avatarId = 3, size = 28.dp)
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Column {
                        Text(
                            text = host.name,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, null, tint = Gold, modifier = Modifier.size(8.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${host.totalDiamonds}",
                                color = Gold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right: Viewers tracker counters
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$viewersCount",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Close livestream exit button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("exit_room_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Leave",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            PkBattleWidget(pkScore = pkBattleScore, rivalName = "ميرة بنت لـبـنـان 🇱🇧")
        }

        // 3. Simulated Host Speech Bubble (reaction dialog text near host)
        hostSpeechBubble?.let { speech ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrightPink),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 0.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = speech,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 4. Dynamic Live Chats Scrolling overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .height(180.dp)
                .padding(start = 14.dp, bottom = 64.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                    )
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chatMessages) { msg ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (msg.isSystem) HotOrange.copy(alpha = 0.25f)
                                else Color.Black.copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!msg.isSystem) {
                                    popupName = msg.senderName
                                    popupLevel = msg.level
                                    popupAvatarId = msg.avatarId
                                    popupIsHost = false
                                    popupDiamonds = if (msg.senderName.contains("Himo")) 4500000 else msg.level * 1520
                                    popupBio = "عضو داعم في دينا لايف. يعشق إرسال الهدايا للنشامى والموسيقى الحية والمنافسات الحميسية 🇲🇦💎"
                                    popupFollowers = msg.level * 80
                                    showProfilePopup = true
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom avatar before the level tag
                        if (!msg.isSystem) {
                            MoroccanAvatar(avatarId = msg.avatarId, size = 18.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Supporter Badge marker
                        if (!msg.isSystem) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (msg.isPremium) Gold else NeonCyan,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Lvl ${msg.level}",
                                    color = LiveBackground,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = msg.senderName,
                            color = if (msg.isSystem) Gold else BrightPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = msg.text,
                            color = if (msg.isSystem) PureWhite else MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 5. Gift Animation overlay flies from bottom left
        activeGiftAnim?.let { giftAnim ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-110).dp, x = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(CardPurple, HotOrange.copy(alpha = 0.6f))),
                            RoundedCornerShape(30.dp)
                        )
                        .border(1.5.dp, Gold, RoundedCornerShape(30.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = giftAnim.senderName,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "أرسل ${giftAnim.giftName}",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = giftAnim.giftIcon,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Big Full Screen Screen-Shaking Lion Overlay if cost >= 1000
            if (giftAnim.isLion) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Pulsing Lion Emoji
                        Text(
                            text = "🦁",
                            fontSize = 90.sp,
                            modifier = Modifier
                                .graphicsLayer {
                                    val scaleVal = 1.0f + sin(System.currentTimeMillis() / 150.0).toFloat() * 0.15f
                                    scaleX = scaleVal
                                    scaleY = scaleVal
                                }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "السبع جـااااا! 🦁👑",
                            color = Gold,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "شكرا جزيلا الداعم الأسطوري ${giftAnim.senderName}!",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 5.5 Moroccan Traditional Zgharit / Wedding Party Animation Overlay (Addition)
        zgharitTrigger?.let { trillText ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "zgharit")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(450, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "zgharit_scale"
                    )
                    
                    Text(
                        text = "🥳🎷 طــرب ونـشـاط مـغـربـي أصـيـل! 🎷🥳",
                        color = Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.scale(pulseScale)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HotOrange),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .scale(pulseScale)
                            .shadow(16.dp, RoundedCornerShape(24.dp))
                            .border(2.dp, Gold, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = trillText,
                                color = PureWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "👏👏 تصفيق حار لمول الجود الحركي 👑👏👏",
                                color = PureWhite.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. Streaming bottom controllers tray
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Simulated chat action text input field trigger
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable {
                        showChatInputDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "كتب شي كلمة معقولة...",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Exclusive VIP button for Owner Himo to trigger traditional Zgharit trills live
            if (userAccount?.isOwner == true) {
                Button(
                    onClick = { viewModel.triggerZgharit() },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                ) {
                    Text("👑 زغاريد Himo", color = LiveBackground, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Gifting Panel floating action trigger button
            FloatingActionButton(
                onClick = { showGiftSheet = true },
                containerColor = BrightPink,
                contentColor = PureWhite,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("gift_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = "Gifts Shop",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4.5 Floating Stream Utility Overlay (Beauty Wand, Camera Flip, Voice FX) (Addition)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Beauty Wand Filter Selector
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(1.2.dp, Gold.copy(alpha = 0.5f), CircleShape)
                    .clickable { showFilterSelector = !showFilterSelector }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "مؤثرات التجميل",
                    tint = if (activeCameraFilterIndex > 0) Gold else PureWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Camera Flip Simulator
            var isFrontCamera by remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(1.2.dp, GlassWhite.copy(alpha = 0.2f), CircleShape)
                    .clickable { 
                        isFrontCamera = !isFrontCamera
                        simulatedToastMessage = if (isFrontCamera) "تم التحويل للكاميرا الأمامية 🤳" else "تم التحويل للكاميرا الخلفية 🔄"
                    }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "قلب الكاميرا",
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Voice FX selector
            var activeVoiceFx by remember { mutableStateOf("عادي") }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(1.2.dp, GlassWhite.copy(alpha = 0.2f), CircleShape)
                    .clickable { 
                        val effects = listOf("عادي", "استوديو دافئ 🎙️", "صدى شعبي 🎤", "صوت سنجاب 🐿️", "صوت جهوري 📢")
                        val currentIdx = effects.indexOf(activeVoiceFx)
                        val nextIdx = (currentIdx + 1) % effects.size
                        activeVoiceFx = effects[nextIdx]
                        simulatedToastMessage = "مؤثر الصوت: ${activeVoiceFx}"
                    }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "مؤثرصوتي",
                    tint = if (activeVoiceFx != "عادي") NeonCyan else PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 5.8 Simulated Camera/Audio Toast System (Addition)
        simulatedToastMessage?.let { toast ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .animateContentSize()
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪄", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = toast,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Gift Shop bottom drawer sheet popup
    if (showGiftSheet) {
        Dialog(onDismissRequest = { showGiftSheet = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardPurple),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("gift_shop_sheet")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سيفط هدية مغربية للمضيفة 🎁",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "الرصيد: ${userAccount?.coins ?: 0}",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("ذهب", color = Gold, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gifts Grid catalog list
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(230.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.giftCatalog) { gift ->
                            val isAffordable = (userAccount?.coins ?: 0) >= gift.cost
                            
                            Column(
                                modifier = Modifier
                                    .background(
                                        if (gift.cost >= 500) HotOrange.copy(alpha = 0.15f)
                                        else LiveBackground,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        if (gift.cost >= 500) 1.dp else 0.dp,
                                        if (gift.cost >= 500) Gold else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.sendGift(gift)
                                        showGiftSheet = false // Auto close drawer
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = gift.icon,
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = gift.name,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Savings, null, tint = Gold, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${gift.cost}",
                                        color = if (isAffordable) Gold else Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شحن الرصيد مجاني وسريع",
                            color = MutedText,
                            fontSize = 9.sp
                        )

                        TextButton(
                            onClick = { showGiftSheet = false }
                        ) {
                            Text("إغلاق المتجر", color = BrightPink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Trigger Profile Dialog overlay
    if (showProfilePopup) {
        HostProfileDialog(
            name = popupName,
            avatarId = popupAvatarId,
            level = popupLevel,
            bio = popupBio,
            diamonds = popupDiamonds,
            followers = popupFollowers,
            isHost = popupIsHost,
            onDismiss = { showProfilePopup = false },
            onSendGiftDirect = {
                showGiftSheet = true
            }
        )
    }

    // Trigger Custom Type Inbox Dialog
    if (showChatInputDialog) {
        Dialog(onDismissRequest = { showChatInputDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardPurple),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "شارك بالتعليق فالحين 🎤",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = customChatText,
                        onValueChange = { customChatText = it },
                        placeholder = { Text("كتب شي لعيبة زوينة... 🇲🇦", color = MutedText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_text_field"),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = LiveBackground,
                            unfocusedContainerColor = LiveBackground
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showChatInputDialog = false }) {
                            Text("إلغاء", color = MutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customChatText.isNotBlank()) {
                                    viewModel.sendUserChatMessage(customChatText)
                                    customChatText = ""
                                }
                                showChatInputDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ارسل 🚀", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============== PK BATTLE ARENA WIDGET (ADDITION) =============
@Composable
fun PkBattleWidget(pkScore: Int, rivalName: String) {
    val animatedScore by animateFloatAsState(
        targetValue = pkScore.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "pk_score_anim"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            // PK labels and VS emblem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side (We / Our Streamer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(HotOrange, BrightPink)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("فريقنا 🇲🇦", color = PureWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${pkScore * 120} نقطة", color = BrightPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Center VS
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(BrightPink, NeonCyan)),
                            CircleShape
                        )
                        .border(1.dp, Gold, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text("PK تحدي الملوك", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Right Side (Rival)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${(100 - pkScore) * 120} نقطة", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, NeonCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = rivalName, color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar dividing Team Morocco vs Rival
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(NeonCyan)
            ) {
                // Pink portion (Team Morocco)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedScore)
                        .background(
                            Brush.horizontalGradient(
                                listOf(HotOrange, BrightPink)
                            )
                        )
                )
            }
        }
    }
}

// ============== MOROCCAN CHIC AVATARS & ONBOARDING PROFILES (ADDITION) =============
data class AvatarPreset(
    val id: Int,
    val emoji: String,
    val label: String,
    val bgColors: List<Color>
)

val AvatarList = listOf(
    AvatarPreset(1, "👑", "الملك هيمو 👑", listOf(Gold, HotOrange)),
    AvatarPreset(2, "🦁", "السبع الأسطوري 🦁", listOf(HotOrange, BrightPink)),
    AvatarPreset(3, "💃", "القفطان المغربي 💃", listOf(BrightPink, CardPurple)),
    AvatarPreset(4, "💰", "الداعم السخي 💰", listOf(Gold, Color(0xFFFF9E00))),
    AvatarPreset(5, "👰", "العروسة الأنيقة 👰", listOf(BrightPink, NeonCyan)),
    AvatarPreset(6, "🕶️", "المدير الوقور 🕶️", listOf(Color.DarkGray, Color.Black)),
    AvatarPreset(7, "🏎️", "الداعم السريع 🏎️", listOf(NeonCyan, CardPurple)),
    AvatarPreset(8, "🌹", "وردة الأطلس 🌹", listOf(BrightPink, HotOrange)),
    AvatarPreset(9, "🇲🇦", "ولد البلاد الأصيل 🇲🇦", listOf(Color(0xFFE63946), Color(0xFF1D3557))),
    AvatarPreset(10, "🎪", "النشايطي المرح 🎪", listOf(NeonCyan, HotOrange))
)

@Composable
fun MoroccanAvatar(avatarId: Int, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val preset = AvatarList.find { it.id == avatarId } ?: AvatarList[2]
    
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.linearGradient(preset.bgColors),
                CircleShape
            )
            .border(
                width = if (size > 60.dp) 3.dp else 1.5.dp,
                color = if (avatarId == 1) Gold else GlassWhite.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .shadow(4.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = preset.emoji,
            fontSize = (size.value * 0.45f).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RegistrationScreen(viewModel: DinaLiveViewModel) {
    var nickname by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf(2) }
    var isHostRole by remember { mutableStateOf(false) }
    var isOwnerMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LiveBackground, DarkPurple, CardPurple)
                )
            )
    ) {
        // Decorative cultural stars / shadows in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = NeonCyan.copy(alpha = 0.08f), radius = 350f, center = Offset(0f, 200f))
            drawCircle(color = BrightPink.copy(alpha = 0.08f), radius = 400f, center = Offset(size.width, size.height * 0.6f))
            drawCircle(color = Gold.copy(alpha = 0.05f), radius = 250f, center = Offset(size.width * 0.5f, size.height * 0.2f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logo Icon & App Title
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Brush.sweepGradient(listOf(Gold, BrightPink, NeonCyan)), CircleShape)
                    .border(2.dp, Gold, CircleShape)
                    .shadow(12.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🇲🇦", fontSize = 42.sp)
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "ديـنـا لايـف 👑 DinaLive",
                color = PureWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "المنصة المغربية الأولى للبث والنشاط الشعبي 🎤",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Onboarding Card Form
            Card(
                colors = CardDefaults.cardColors(containerColor = CardPurple.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "تسجيل حساب جديد بالثانية 🚀",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Avatar Selection carousel
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "اختر صورة بروفايلك المفضلة 👤",
                                color = Gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(AvatarList) { av ->
                                    val isSelected = av.id == selectedAvatarId
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { 
                                                selectedAvatarId = av.id
                                                // If king selected, default nickname to Himo
                                                if (av.id == 1) {
                                                    nickname = "الداعم الأكبر Himo 👑"
                                                    isOwnerMode = true
                                                } else {
                                                    isOwnerMode = false
                                                }
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .background(
                                                    if (isSelected) BrightPink.copy(alpha = 0.2f) else Color.Transparent,
                                                    CircleShape
                                                )
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) Gold else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MoroccanAvatar(avatarId = av.id, size = 52.dp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = av.label.split(" ")[0],
                                            color = if (isSelected) Gold else MutedText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Nickname field
                    item {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("اكتب كنيتك أو اسم الشهرة", color = MutedText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = LiveBackground,
                                unfocusedContainerColor = LiveBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Preconfigured options for testing / instant owner selection!
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .border(1.dp, if (isOwnerMode) Gold else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { 
                                    isOwnerMode = !isOwnerMode
                                    if (isOwnerMode) {
                                        nickname = "الداعم الأكبر Himo 👑"
                                        selectedAvatarId = 1
                                    } else {
                                        nickname = ""
                                        selectedAvatarId = 2
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تفعيل رتبة: مول التطبيق والداعم الأكبر Himo 👑",
                                    color = Gold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "غادي يعطيك كتر من 4 مليون كوينز ورتبة الـVIP مباشرة!",
                                    color = MutedText,
                                    fontSize = 9.sp
                                )
                            }
                            Checkbox(
                                checked = isOwnerMode,
                                onCheckedChange = {
                                    isOwnerMode = it
                                    if (it) {
                                        nickname = "الداعم الأكبر Himo 👑"
                                        selectedAvatarId = 1
                                    } else {
                                        nickname = ""
                                        selectedAvatarId = 2
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = MutedText)
                            )
                        }
                    }

                    // Choice of Role
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { isHostRole = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isHostRole) BrightPink else CardPurple
                                ),
                                border = BorderStroke(1.dp, if (!isHostRole) BrightPink else GlassWhite),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("داعم كريم 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Button(
                                onClick = { isHostRole = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHostRole) BrightPink else CardPurple
                                ),
                                border = BorderStroke(1.dp, if (isHostRole) BrightPink else GlassWhite),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("مضيف بـث 🎤", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Sign-in launch action
                    item {
                        Button(
                            onClick = {
                                val finalNickName = nickname.ifBlank { "ولد البلاد 🇲🇦" }
                                viewModel.registerNewUser(
                                    nickname = finalNickName,
                                    avatarId = selectedAvatarId,
                                    isHost = isHostRole,
                                    isOwnerSetting = isOwnerMode
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                text = "دخل وسيطر على اللايفات دابا! 🚀",
                                color = LiveBackground,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1.5f))
            Text(
                text = "جميع الحقوق محفوظة © دينا لايف للمغاربة والأشقاء",
                color = MutedText.copy(alpha = 0.5f),
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun HostProfileDialog(
    name: String,
    avatarId: Int,
    level: Int,
    bio: String,
    diamonds: Int,
    followers: Int,
    isHost: Boolean,
    onDismiss: () -> Unit,
    onSendGiftDirect: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardPurple),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.sweepGradient(listOf(BrightPink, Gold, NeonCyan)),
                            CircleShape
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MoroccanAvatar(avatarId = avatarId, size = 92.dp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name & Badge
                Text(
                    text = name,
                    color = PureWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Level / Role Badge
                Box(
                    modifier = Modifier
                        .background(if (isHost) BrightPink else Gold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isHost) "مضيفة معتمدة 🎤" else "داعم رتبة ${level} 💎",
                        color = LiveBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Statistics Columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isHost) "الهدايا المقبولة" else "الرصيد المشحون",
                            color = MutedText,
                            fontSize = 9.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, null, tint = Gold, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "$diamonds",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(GlassWhite)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isHost) "المتابعين" else "القيمة التقريبية",
                            color = MutedText,
                            fontSize = 9.sp
                        )
                        Text(
                            text = if (isHost) "$followers" else "داعم ذهبي",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bio
                Text(
                    text = bio.ifBlank { "هذا العضو ناشط في دينا لايف ويحب التفاعلات الراقية! 🇲🇦" },
                    color = PureWhite.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إغلاق", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                }
            }
        }
    }
}

// ============== LIVE CAMERA BEAUTY FILTERS (ADDITION) =============
data class CameraFilter(
    val id: Int,
    val name: String,
    val icon: String,
    val description: String,
    val tagLine: String
)

val CameraFiltersList = listOf(
    CameraFilter(0, "عادي طبيعي ✨", "📷", "بث نقي بدون مؤثرات إضافية", "الكاميرا طبيعية"),
    CameraFilter(1, "الوجه المخملي 🧴", "✨", "تنعيم فوري لإضاءة بيضاء دافئة", "تنعيم البشرة مفعل"),
    CameraFilter(2, "الأطلس الرومانسي 🌹", "🌸", "لون وردي ناعم كزهر الأطلس", "فلتر وردي مفعل"),
    CameraFilter(3, "الذهب الملكي الشهباء 👑", "💫", "إضاءة لؤلؤية وبريق ذهبي متلألئ", "الذهب الملكي مفعل"),
    CameraFilter(4, "نيون الشات والنشاط 🏙️", "⚡", "ألوان كازابلانكا الصاخبة المشرقة", "النشاط والنيون مفعل"),
    CameraFilter(5, "زمان والسينما القديمة 🎞️", "🏺", "تأثير دافئ كلاسيكي وعتيق", "سينما كلاسيكية مفعل")
)

@Composable
fun CameraFilterOverlay(filterId: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "filter_transition")
    when (filterId) {
        1 -> { // Velvet Soft skin
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                            radius = 800f
                        )
                    )
            )
        }
        2 -> { // Moroccan Rose
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rose_pulse"
            )
            val petalOffset by transition.animateFloat(
                initialValue = -50f,
                targetValue = 600f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "petal_fall"
            )

            Box(modifier = modifier.fillMaxSize()) {
                // Pink tint layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFF1493).copy(alpha = pulseAlpha))
                )
                
                // Falling soft pink particles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        color = Color(0xFFFF69B4).copy(alpha = 0.65f),
                        radius = 11f,
                        center = Offset(w * 0.15f, petalOffset)
                    )
                    drawCircle(
                        color = Color(0xFFFFC0CB).copy(alpha = 0.55f),
                        radius = 8f,
                        center = Offset(w * 0.8f, (petalOffset + 250f) % h)
                    )
                    drawCircle(
                        color = Color(0xFFFF1493).copy(alpha = 0.45f),
                        radius = 14f,
                        center = Offset(w * 0.45f, (petalOffset + 400f) % h)
                    )
                }
            }
        }
        3 -> { // Royal Golden Glimmer
            val sparkleValue by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sparkle_rotation"
            )

            Box(modifier = modifier.fillMaxSize()) {
                // Golden vignette border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color(0xFFFFD700).copy(alpha = 0.12f)),
                                radius = 900f
                            )
                        )
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val angle = Math.toRadians(sparkleValue.toDouble())
                    val w = size.width
                    val h = size.height

                    // Drawing beautiful gold micro-particles at orbits
                    val x1 = (w * 0.25f + 110 * Math.sin(angle)).toFloat()
                    val y1 = (h * 0.3f + 70 * Math.cos(angle)).toFloat()
                    drawCircle(Color(0xFFFFD700), radius = 6f, center = Offset(x1, y1))

                    val x2 = (w * 0.75f + 130 * Math.cos(angle + 2.5)).toFloat()
                    val y2 = (h * 0.45f + 90 * Math.sin(angle + 2.5)).toFloat()
                    drawCircle(Color(0xFFFFF8DC), radius = 5f, center = Offset(x2, y2))

                    val x3 = (w * 0.5f + 160 * Math.sin(angle * 2.0)).toFloat()
                    val y3 = (h * 0.2f + 80 * Math.cos(angle * 2.0)).toFloat()
                    drawCircle(Color(0xFFFFA500), radius = 7f, center = Offset(x3, y3))
                }
            }
        }
        4 -> { // Casablanca Neon Party
            val pulseVal by transition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "neon_pulse_size"
            )

            Box(modifier = modifier.fillMaxSize()) {
                // Cyan tint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x1F00F3FF))
                )

                // Neon glowing borders
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    BrightPink.copy(alpha = pulseVal + 0.3f),
                                    NeonCyan.copy(alpha = pulseVal + 0.4f),
                                    BrightPink.copy(alpha = pulseVal + 0.3f)
                                )
                            ),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        )
                )
            }
        }
        5 -> { // Vintage Carthage
            Box(modifier = modifier.fillMaxSize()) {
                // Sepia color-cast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x3E8B5A2B))
                )
                // Vignette shadow frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                radius = 950f
                            )
                        )
                )
            }
        }
        else -> {} // No filter
    }
}

