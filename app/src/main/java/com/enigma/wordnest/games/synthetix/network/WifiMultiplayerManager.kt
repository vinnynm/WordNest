package com.enigma.wordnest.games.synthetix.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.model.PlacedTile
import com.enigma.wordnest.games.synthetix.model.Player
import com.enigma.wordnest.games.synthetix.model.Tile
import com.enigma.wordnest.games.synthetix.model.TileSetConfig
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.ServerSocket
import java.net.Socket

// ─────────────────────────────────────────────────────────────────────────────
//  Wire protocol — every message is one JSON line
// ─────────────────────────────────────────────────────────────────────────────

enum class WifiMsgType {
    HELLO,          // host→guest: playerIndex, boardConfigJson, tileSetJson, boardJson
    GAME_STATE,     // both ways: full snapshot after every turn
    PLACE_TILE,     // client→host: row, col, letter, blankAs
    PLAY_WORD,      // client→host: no payload (triggers server-side playWord())
    RECALL_ALL,     // client→host
    SKIP,           // client→host
    EXCHANGE,       // client→host
    SHUFFLE,        // client→host
    CHAT,           // both ways: message string
    PING, PONG      // keep-alive
}

data class WifiMessage(
    val type: WifiMsgType,
    val payload: String = ""   // JSON-encoded payload or plain string
)

// ─────────────────────────────────────────────────────────────────────────────
//  Shared game-state snapshot sent over the wire after each turn
// ─────────────────────────────────────────────────────────────────────────────

data class WifiGameSnapshot(
    val board: List<List<Tile?>>,
    val players: List<Player>,
    val currentPlayer: Int,
    val bag: List<Char>,
    val consecutiveSkips: Int,
    val placedThisTurn: List<PlacedTile>,
    val isGameOver: Boolean,
    val lastPlayMessage: String = "",
    val lastPlayScore: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
//  Connection state
// ─────────────────────────────────────────────────────────────────────────────

sealed class WifiConnectionState {
    object Idle : WifiConnectionState()
    object Advertising : WifiConnectionState()
    object Scanning : WifiConnectionState()
    data class Connecting(val host: String) : WifiConnectionState()
    data class Connected(val peerName: String, val isHost: Boolean) : WifiConnectionState()
    data class Error(val message: String) : WifiConnectionState()
}

// ─────────────────────────────────────────────────────────────────────────────
//  WifiMultiplayerManager
//  - Host: opens a ServerSocket, advertises via NSD, accepts one connection
//  - Guest: discovers via NSD, connects to the host
//  Both sides exchange WifiMessage lines over a persistent socket.
// ─────────────────────────────────────────────────────────────────────────────

class WifiMultiplayerManager(private val context: Context) {

    companion object {
        private const val TAG          = "WifiMultiplayer"
        private const val SERVICE_TYPE = "_synthetix._tcp."
        private const val SERVICE_NAME = "SynthetixGame"
        private const val PORT         = 47291
    }

    private val gson  = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    // Public state flows
    private val _connectionState = MutableStateFlow<WifiConnectionState>(WifiConnectionState.Idle)
    val connectionState: StateFlow<WifiConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableStateFlow<WifiMessage?>(null)
    val incomingMessages: StateFlow<WifiMessage?> = _incomingMessages.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredHosts: StateFlow<List<NsdServiceInfo>> = _discoveredHosts.asStateFlow()

    // Socket infrastructure
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    var isHost: Boolean = false
        private set

    // ── HOST side ─────────────────────────────────────────────────────────────

    /**
     * Start hosting: bind a server socket, register the NSD service so nearby
     * devices can discover us, then wait for the first connection.
     */
    fun startHosting(playerName: String) {
        isHost = true
        _connectionState.value = WifiConnectionState.Advertising
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                registerNsdService(playerName)

                Log.d(TAG, "Waiting for guest on port $PORT…")
                val socket = serverSocket!!.accept()   // blocks until guest connects
                setupSocket(socket)
                Log.d(TAG, "Guest connected: ${socket.inetAddress.hostAddress}")

                // Connection state updated once HELLO handshake is done
                startReading()
            } catch (e: Exception) {
                Log.e(TAG, "Host error", e)
                _connectionState.value = WifiConnectionState.Error(e.localizedMessage ?: "Host error")
            }
        }
    }

    private fun registerNsdService(playerName: String) {
        val info = NsdServiceInfo().apply {
            serviceName = "$SERVICE_NAME-$playerName"
            serviceType = SERVICE_TYPE
            port        = PORT
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "NSD registered: ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {
                Log.e(TAG, "NSD registration failed: $code")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
        }
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    // ── GUEST side ────────────────────────────────────────────────────────────

    /** Scan for advertised Synthetix games on the local network. */
    fun startDiscovery() {
        _connectionState.value = WifiConnectionState.Scanning
        _discoveredHosts.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, code: Int) {
                Log.e(TAG, "Discovery start failed: $code")
            }
            override fun onStopDiscoveryFailed(serviceType: String, code: Int) {}
            override fun onDiscoveryStarted(serviceType: String) { Log.d(TAG, "Discovery started") }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (info.serviceType.contains(SERVICE_TYPE.trimEnd('.'))) {
                    // Resolve to get the host IP
                    nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, code: Int) {
                            Log.e(TAG, "Resolve failed: $code")
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            Log.d(TAG, "Resolved: ${info.host?.hostAddress}:${info.port}")
                            val current = _discoveredHosts.value.toMutableList()
                            if (current.none { it.serviceName == info.serviceName }) {
                                current += info
                                _discoveredHosts.value = current
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                _discoveredHosts.value = _discoveredHosts.value.filter { it.serviceName != info.serviceName }
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    /** Connect to a specific host discovered via [startDiscovery]. */
    fun connectToHost(info: NsdServiceInfo) {
        isHost = false
        val host = info.host?.hostAddress ?: return
        _connectionState.value = WifiConnectionState.Connecting(host)

        scope.launch {
            try {
                val socket = Socket(host, info.port)
                setupSocket(socket)
                startReading()
                Log.d(TAG, "Connected to host $host:${info.port}")
            } catch (e: Exception) {
                Log.e(TAG, "Connect error", e)
                _connectionState.value = WifiConnectionState.Error(e.localizedMessage ?: "Connect error")
            }
        }
    }

    // ── Shared socket I/O ─────────────────────────────────────────────────────

    private fun setupSocket(socket: Socket) {
        clientSocket = socket
        writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    }

    /** Continuously read lines from the peer, dispatch as [WifiMessage]s. */
    private fun startReading() {
        scope.launch {
            try {
                while (true) {
                    val line = reader?.readLine() ?: break
                    val msg  = gson.fromJson(line, WifiMessage::class.java)
                    handleIncoming(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Read error", e)
                _connectionState.value = WifiConnectionState.Error("Connection lost")
            }
        }

        // Keep-alive ping every 5 s
        scope.launch {
            while (true) {
                delay(5_000)
                send(WifiMessage(WifiMsgType.PING))
            }
        }
    }

    private fun handleIncoming(msg: WifiMessage) {
        when (msg.type) {
            WifiMsgType.PONG -> { /* ignore */ }
            WifiMsgType.PING -> send(WifiMessage(WifiMsgType.PONG))
            WifiMsgType.HELLO -> {
                val hello = gson.fromJson(msg.payload, HelloPayload::class.java)
                _connectionState.value = WifiConnectionState.Connected(
                    peerName = hello.peerName,
                    isHost   = false
                )
                _incomingMessages.value = msg
            }
            else -> _incomingMessages.value = msg
        }
    }

    /** Send a message to the connected peer. Thread-safe. */
    fun send(msg: WifiMessage) {
        scope.launch {
            try {
                writer?.println(gson.toJson(msg))
            } catch (e: Exception) {
                Log.e(TAG, "Send error", e)
            }
        }
    }

    // ── Convenience senders ───────────────────────────────────────────────────

    /** Host → guest: initial game setup after connection. */
    fun sendHello(peerName: String, playerIndex: Int, boardConfig: BoardConfig, tileSet: TileSetConfig) {
        val payload = HelloPayload(
            peerName        = peerName,
            playerIndex     = playerIndex,
            boardConfigJson = gson.toJson(boardConfig),
            tileSetJson     = gson.toJson(tileSet)
        )
        send(WifiMessage(WifiMsgType.HELLO, gson.toJson(payload)))
        _connectionState.value = WifiConnectionState.Connected(peerName, isHost = true)
    }

    /** Broadcast the authoritative game state to the guest. */
    fun sendGameState(snapshot: WifiGameSnapshot) {
        send(WifiMessage(WifiMsgType.GAME_STATE, gson.toJson(snapshot)))
    }

    /** Guest → host: place a tile. */
    fun sendPlaceTile(row: Int, col: Int, letter: Char, blankAs: Char?) {
        send(WifiMessage(WifiMsgType.PLACE_TILE, gson.toJson(PlaceTilePayload(row, col, letter, blankAs))))
    }

    fun sendPlayWord()   = send(WifiMessage(WifiMsgType.PLAY_WORD))
    fun sendRecallAll()  = send(WifiMessage(WifiMsgType.RECALL_ALL))
    fun sendSkip()       = send(WifiMessage(WifiMsgType.SKIP))
    fun sendExchange()   = send(WifiMessage(WifiMsgType.EXCHANGE))
    fun sendShuffle()    = send(WifiMessage(WifiMsgType.SHUFFLE))
    fun sendChat(text: String) = send(WifiMessage(WifiMsgType.CHAT, text))

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun stopDiscovery() {
        runCatching { discoveryListener?.let { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
    }

    fun disconnect() {
        stopDiscovery()
        runCatching { registrationListener?.let { nsdManager.unregisterService(it) } }
        runCatching { clientSocket?.close() }
        runCatching { serverSocket?.close() }
        scope.cancel()
        _connectionState.value = WifiConnectionState.Idle
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Payload data classes
// ─────────────────────────────────────────────────────────────────────────────

data class HelloPayload(
    val peerName: String,
    val playerIndex: Int,
    val boardConfigJson: String,
    val tileSetJson: String
)

data class PlaceTilePayload(
    val row: Int,
    val col: Int,
    val letter: Char,
    val blankAs: Char?
)
