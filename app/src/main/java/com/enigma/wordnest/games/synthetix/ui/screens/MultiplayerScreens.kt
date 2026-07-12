package com.enigma.wordnest.games.synthetix.ui.screens

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme
import com.enigma.wordnest.games.synthetix.network.ChatMessage
import com.enigma.wordnest.games.synthetix.network.OnlineRoomMeta
import com.enigma.wordnest.games.synthetix.network.OnlineState
import com.enigma.wordnest.games.synthetix.network.WifiConnectionState

// ─────────────────────────────────────────────────────────────────────────────
//  Multiplayer Hub Screen — entry point for online and local WiFi play
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerHubScreen(
    theme: BoardTheme,
    onOnlineSelected: () -> Unit,
    onWifiSelected: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer", fontWeight = FontWeight.Bold, color = theme.appBarContent) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.appBarContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.appBarBackground)
            )
        },
        containerColor = theme.surfaceBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(theme.emoji, fontSize = 40.sp)
            Text(
                "PLAY WITH FRIENDS",
                fontSize = 22.sp, fontWeight = FontWeight.Black,
                letterSpacing = 3.sp, color = theme.primaryAccent
            )
            Spacer(Modifier.height(8.dp))

            // Online card
            MultiplayerOptionCard(
                icon        = "🌐",
                title       = "Online Play",
                description = "Async turn-based — play from anywhere\nvia Firebase cloud sync",
                theme       = theme,
                onClick     = onOnlineSelected
            )

            // WiFi card
            MultiplayerOptionCard(
                icon        = "📶",
                title       = "Local WiFi",
                description = "Real-time play on the same network\nwith near-zero latency",
                theme       = theme,
                onClick     = onWifiSelected
            )
        }
    }
}

@Composable
private fun MultiplayerOptionCard(
    icon: String,
    title: String,
    description: String,
    theme: BoardTheme,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, theme.gridLineColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = theme.surfaceCard
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(icon, fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = theme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 12.sp, color = theme.onSurfaceMuted, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = theme.onSurfaceMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Online Lobby Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineLobbyScreen(
    theme: BoardTheme,
    onlineState: OnlineState,
    openRooms: List<OnlineRoomMeta>,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    onRefreshRooms: () -> Unit,
    onBack: () -> Unit
) {
    var playerName  by remember { mutableStateOf("") }
    var joinCode    by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Online Lobby", fontWeight = FontWeight.Bold, color = theme.appBarContent) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.appBarContent)
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshRooms) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = theme.appBarContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.appBarBackground)
            )
        },
        containerColor = theme.surfaceBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status banner
            OnlineStatusBanner(onlineState, theme)

            // Player name
            OutlinedTextField(
                value         = playerName,
                onValueChange = { playerName = it.take(20) },
                label         = { Text("Your Name", color = theme.onSurfaceMuted) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = { if (playerName.isNotBlank()) onCreateRoom(playerName) },
                    modifier = Modifier.weight(1f),
                    enabled  = playerName.isNotBlank() && onlineState is OnlineState.Idle
                ) { Text("CREATE ROOM") }

                OutlinedButton(
                    onClick  = { showJoinDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled  = playerName.isNotBlank() && onlineState is OnlineState.Idle
                ) { Text("JOIN BY CODE") }
            }

            // Waiting banner
            if (onlineState is OnlineState.WaitingForGuest) {
                WaitingRoomCard(onlineState.roomCode, theme)
            }

            // Open rooms list
            if (openRooms.isNotEmpty()) {
                Text(
                    "OPEN GAMES",
                    fontSize = 11.sp, letterSpacing = 2.sp,
                    color = theme.onSurfaceMuted, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(openRooms) { room ->
                        OpenRoomRow(
                            room    = room,
                            theme   = theme,
                            onClick = {
                                if (playerName.isNotBlank()) onJoinRoom(playerName, room.roomCode)
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No open games found.\nCreate one and share the code!",
                        textAlign = TextAlign.Center, color = theme.onSurfaceMuted, fontSize = 13.sp)
                }
            }
        }
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            containerColor   = theme.surfaceCard,
            title = { Text("Join by Room Code", color = theme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value         = joinCode,
                    onValueChange = { joinCode = it.uppercase().take(6) },
                    label         = { Text("6-character code") },
                    singleLine    = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (joinCode.length == 6 && playerName.isNotBlank()) {
                        onJoinRoom(playerName, joinCode)
                        showJoinDialog = false
                    }
                }, enabled = joinCode.length == 6) { Text("JOIN") }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WaitingRoomCard(roomCode: String, theme: BoardTheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = theme.primaryAccent.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = theme.primaryAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text("Waiting for opponent…", color = theme.primaryAccent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Share this code:", color = theme.onSurfaceMuted, fontSize = 12.sp)
            Text(
                roomCode,
                fontSize = 32.sp, fontWeight = FontWeight.Black,
                letterSpacing = 8.sp, color = theme.primaryAccent
            )
        }
    }
}

@Composable
private fun OpenRoomRow(room: OnlineRoomMeta, theme: BoardTheme, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = theme.surfaceCard
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(room.hostName, fontWeight = FontWeight.Bold, color = theme.onSurface, fontSize = 14.sp)
                Text("Room: ${room.roomCode}", color = theme.onSurfaceMuted, fontSize = 11.sp)
            }
            Button(onClick = onClick, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("JOIN", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun OnlineStatusBanner(state: OnlineState, theme: BoardTheme) {
    val (text, color) = when (state) {
        is OnlineState.Idle           -> null to Color.Transparent
        is OnlineState.CreatingRoom   -> "Creating room…" to theme.primaryAccent
        is OnlineState.WaitingForGuest -> null to Color.Transparent
        is OnlineState.JoiningRoom    -> "Joining ${state.roomCode}…" to theme.primaryAccent
        is OnlineState.InGame         -> "In game — Room ${state.roomCode}" to theme.primaryAccent
        is OnlineState.Error          -> "Error: ${state.message}" to MaterialTheme.colorScheme.error
    }
    if (text != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text, color = color, fontSize = 12.sp,
                modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WiFi Lobby Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiLobbyScreen(
    theme: BoardTheme,
    connectionState: WifiConnectionState,
    discoveredHosts: List<NsdServiceInfo>,
    onHost: (String) -> Unit,
    onScan: () -> Unit,
    onConnect: (NsdServiceInfo) -> Unit,
    onBack: () -> Unit
) {
    var playerName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local WiFi", fontWeight = FontWeight.Bold, color = theme.appBarContent) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.appBarContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.appBarBackground)
            )
        },
        containerColor = theme.surfaceBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WifiStatusBanner(connectionState, theme)

            OutlinedTextField(
                value         = playerName,
                onValueChange = { playerName = it.take(20) },
                label         = { Text("Your Name", color = theme.onSurfaceMuted) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick  = { if (playerName.isNotBlank()) onHost(playerName) },
                    modifier = Modifier.weight(1f),
                    enabled  = playerName.isNotBlank() && connectionState is WifiConnectionState.Idle
                ) { Text("HOST GAME") }

                OutlinedButton(
                    onClick  = onScan,
                    modifier = Modifier.weight(1f),
                    enabled  = connectionState is WifiConnectionState.Idle ||
                               connectionState is WifiConnectionState.Scanning
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SCAN")
                }
            }

            // Advertising indicator
            if (connectionState is WifiConnectionState.Advertising) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = theme.primaryAccent.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = theme.primaryAccent, modifier = Modifier.size(20.dp))
                        Text("Advertising game on local network…", color = theme.primaryAccent, fontSize = 13.sp)
                    }
                }
            }

            // Discovered games
            if (discoveredHosts.isNotEmpty()) {
                Text(
                    "NEARBY GAMES",
                    fontSize = 11.sp, letterSpacing = 2.sp,
                    color = theme.onSurfaceMuted, fontWeight = FontWeight.Bold
                )
                LazyColumn {
                    items(discoveredHosts) { info ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onConnect(info) },
                            shape = RoundedCornerShape(10.dp),
                            color = theme.surfaceCard
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📶", fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(info.serviceName.removePrefix("SynthetixGame-"),
                                        fontWeight = FontWeight.Bold, color = theme.onSurface, fontSize = 14.sp)
                                    Text("${info.host?.hostAddress}:${info.port}",
                                        color = theme.onSurfaceMuted, fontSize = 11.sp)
                                }
                                Button(onClick = { onConnect(info) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)) {
                                    Text("CONNECT", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (connectionState is WifiConnectionState.Scanning && discoveredHosts.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = theme.primaryAccent)
                        Spacer(Modifier.height(12.dp))
                        Text("Scanning for nearby games…", color = theme.onSurfaceMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiStatusBanner(state: WifiConnectionState, theme: BoardTheme) {
    val text = when (state) {
        is WifiConnectionState.Idle        -> null
        is WifiConnectionState.Advertising -> null  // shown as separate card
        is WifiConnectionState.Scanning    -> "Scanning…"
        is WifiConnectionState.Connecting  -> "Connecting to ${state.host}…"
        is WifiConnectionState.Connected   -> "Connected to ${state.peerName} ${if (state.isHost) "(hosting)" else "(guest)"}"
        is WifiConnectionState.Error       -> "Error: ${state.message}"
    }
    if (text != null) {
        val color = if (state is WifiConnectionState.Error) MaterialTheme.colorScheme.error else theme.primaryAccent
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
            Text(text, color = color, fontSize = 12.sp, modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  In-game Chat Panel (shared for online + WiFi)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    myName: String,
    theme: BoardTheme,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { msg ->
                val isMine = msg.sender == myName
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 12.dp, topEnd = 12.dp,
                            bottomStart = if (isMine) 12.dp else 2.dp,
                            bottomEnd   = if (isMine) 2.dp else 12.dp
                        ),
                        color = if (isMine) theme.primaryAccent.copy(alpha = 0.25f) else theme.surfaceCard,
                        modifier = Modifier.widthIn(max = 240.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (!isMine) {
                                Text(msg.sender, fontSize = 10.sp, color = theme.primaryAccent,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            Text(msg.text, color = theme.onSurface, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                placeholder   = { Text("Message…", fontSize = 13.sp) },
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotBlank()) { onSend(text); text = ""; keyboard?.hide() }
                })
            )
            IconButton(
                onClick  = { if (text.isNotBlank()) { onSend(text); text = "" } },
                enabled  = text.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = theme.primaryAccent)
            }
        }
    }
}
