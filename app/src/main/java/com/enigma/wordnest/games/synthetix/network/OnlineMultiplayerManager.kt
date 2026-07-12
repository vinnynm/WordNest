package com.enigma.wordnest.games.synthetix.network

import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.model.TileSetConfig
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

// ─────────────────────────────────────────────────────────────────────────────
//  Online game document stored in Firebase Realtime Database
//
//  /synthetix_games/{roomCode}/
//    meta:
//      roomCode, hostName, guestName (null until joined),
//      status: "waiting" | "playing" | "finished"
//      boardConfigJson, tileSetJson, createdAt
//    state:
//      board[][], players[], currentPlayer, bag[], consecutiveSkips
//      placedThisTurn[], isGameOver, lastPlayMessage, lastPlayScore
//    chat[]:
//      {sender, text, timestamp}
// ─────────────────────────────────────────────────────────────────────────────

// ── Wire types ────────────────────────────────────────────────────────────────

data class OnlineRoomMeta(
    val roomCode: String        = "",
    val hostName: String        = "",
    val guestName: String?      = null,
    val status: String          = "waiting",    // waiting | playing | finished
    val boardConfigJson: String = "",
    val tileSetJson: String     = "",
    val createdAt: Long         = 0L
)

data class OnlineGameState(
    val board: List<List<Map<String, Any>?>> = emptyList(),  // Tile fields
    val players: List<Map<String, Any>>      = emptyList(),
    val currentPlayer: Int                   = 0,
    val bag: List<String>                    = emptyList(),  // chars as strings
    val consecutiveSkips: Int                = 0,
    val placedThisTurn: List<Map<String, Any>> = emptyList(),
    val isGameOver: Boolean                  = false,
    val lastPlayMessage: String              = "",
    val lastPlayScore: Int                   = 0,
    val turnVersion: Long                    = 0L           // increment to trigger listener
)

data class ChatMessage(
    val sender: String    = "",
    val text: String      = "",
    val timestamp: Long   = 0L
)

sealed class OnlineState {
    object Idle : OnlineState()
    data class CreatingRoom(val roomCode: String) : OnlineState()
    data class WaitingForGuest(val roomCode: String) : OnlineState()
    data class JoiningRoom(val roomCode: String) : OnlineState()
    data class InGame(
        val roomCode: String,
        val playerIndex: Int,       // 0 = host, 1 = guest
        val meta: OnlineRoomMeta
    ) : OnlineState()
    data class Error(val message: String) : OnlineState()
}

// ─────────────────────────────────────────────────────────────────────────────
//  OnlineMultiplayerManager
//  Uses Firebase Realtime Database. Callers must add the Firebase dependency:
//
//  implementation("com.google.firebase:firebase-database-ktx:20.3.1")
//  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
//
//  and apply the google-services plugin in their app/build.gradle.
// ─────────────────────────────────────────────────────────────────────────────

class OnlineMultiplayerManager {

    private val gson  = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val gamesRef: DatabaseReference by lazy { db.getReference("synthetix_games") }

    private val _onlineState   = MutableStateFlow<OnlineState>(OnlineState.Idle)
    val onlineState: StateFlow<OnlineState> = _onlineState.asStateFlow()

    private val _remoteGameState = MutableStateFlow<OnlineGameState?>(null)
    val remoteGameState: StateFlow<OnlineGameState?> = _remoteGameState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _openRooms = MutableStateFlow<List<OnlineRoomMeta>>(emptyList())
    val openRooms: StateFlow<List<OnlineRoomMeta>> = _openRooms.asStateFlow()

    private var roomListener: ValueEventListener? = null
    private var stateListener: ValueEventListener? = null
    private var chatListener: ChildEventListener? = null
    private var currentRoomCode: String? = null

    // ── Room management ───────────────────────────────────────────────────────

    /**
     * Create a new online game room. Returns the 6-character room code.
     */
    suspend fun createRoom(
        hostName: String,
        boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): String {
        val roomCode = generateRoomCode()
        currentRoomCode = roomCode
        _onlineState.value = OnlineState.CreatingRoom(roomCode)

        val meta = OnlineRoomMeta(
            roomCode        = roomCode,
            hostName        = hostName,
            status          = "waiting",
            boardConfigJson = gson.toJson(boardConfig),
            tileSetJson     = gson.toJson(tileSet),
            createdAt       = System.currentTimeMillis()
        )

        gamesRef.child(roomCode).child("meta").setValue(meta).await()
        listenForGuest(roomCode)
        _onlineState.value = OnlineState.WaitingForGuest(roomCode)
        return roomCode
    }

    /** Scan for waiting rooms — call periodically or on demand. */
    fun listenForOpenRooms() {
        gamesRef.orderByChild("meta/status").equalTo("waiting")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rooms = snapshot.children.mapNotNull { child ->
                        child.child("meta").getValue(OnlineRoomMeta::class.java)
                    }
                    _openRooms.value = rooms
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /**
     * Join a waiting room as the guest.
     */
    suspend fun joinRoom(guestName: String, roomCode: String) {
        _onlineState.value = OnlineState.JoiningRoom(roomCode)
        currentRoomCode = roomCode

        val metaRef = gamesRef.child(roomCode).child("meta")
        val snapshot = metaRef.get().await()
        val meta = snapshot.getValue(OnlineRoomMeta::class.java)
            ?: run {
                _onlineState.value = OnlineState.Error("Room $roomCode not found")
                return
            }

        if (meta.status != "waiting") {
            _onlineState.value = OnlineState.Error("Room $roomCode is not waiting for players")
            return
        }

        metaRef.child("guestName").setValue(guestName).await()
        metaRef.child("status").setValue("playing").await()

        _onlineState.value = OnlineState.InGame(
            roomCode    = roomCode,
            playerIndex = 1,
            meta        = meta.copy(guestName = guestName, status = "playing")
        )
        attachRoomListeners(roomCode)
    }

    // ── State synchronisation ─────────────────────────────────────────────────

    /**
     * Host pushes the authoritative game state after each turn.
     * The guest's listener fires and updates [remoteGameState].
     */
    suspend fun pushGameState(state: OnlineGameState) {
        val code = currentRoomCode ?: return
        gamesRef.child(code).child("state")
            .setValue(state.copy(turnVersion = System.currentTimeMillis()))
            .await()
    }

    /** Guest pushes a move request; host reads it and applies it. */
    suspend fun pushMoveRequest(payload: MoveRequestPayload) {
        val code = currentRoomCode ?: return
        gamesRef.child(code).child("pendingMove").setValue(payload).await()
    }

    /** Host listens for pending move requests from the guest. */
    fun listenForMoveRequests(onRequest: (MoveRequestPayload) -> Unit) {
        val code = currentRoomCode ?: return
        gamesRef.child(code).child("pendingMove")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val move = snapshot.getValue(MoveRequestPayload::class.java) ?: return
                    if (move.type.isNotEmpty()) onRequest(move)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /** Clear the pending move after the host has applied it. */
    suspend fun clearPendingMove() {
        val code = currentRoomCode ?: return
        gamesRef.child(code).child("pendingMove").removeValue().await()
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun sendChatMessage(sender: String, text: String) {
        val code = currentRoomCode ?: return
        val msg = ChatMessage(sender = sender, text = text, timestamp = System.currentTimeMillis())
        gamesRef.child(code).child("chat").push().setValue(msg)
    }

    // ── Internal listeners ────────────────────────────────────────────────────

    private fun listenForGuest(roomCode: String) {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val meta = snapshot.child("meta").getValue(OnlineRoomMeta::class.java) ?: return
                if (meta.status == "playing" && meta.guestName != null) {
                    _onlineState.value = OnlineState.InGame(
                        roomCode    = roomCode,
                        playerIndex = 0,
                        meta        = meta
                    )
                    attachRoomListeners(roomCode)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.child(roomCode).addValueEventListener(roomListener!!)
    }

    private fun attachRoomListeners(roomCode: String) {
        // Game state changes
        stateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(OnlineGameState::class.java)
                _remoteGameState.value = state
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.child(roomCode).child("state").addValueEventListener(stateListener!!)

        // Chat
        chatListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(ChatMessage::class.java) ?: return
                _chatMessages.update { it + msg }
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {}
        }
        gamesRef.child(roomCode).child("chat").addChildEventListener(chatListener!!)
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun leaveRoom() {
        val code = currentRoomCode ?: return
        roomListener?.let { gamesRef.child(code).removeEventListener(it) }
        stateListener?.let { gamesRef.child(code).child("state").removeEventListener(it) }
        chatListener?.let  { gamesRef.child(code).child("chat").removeEventListener(it)  }
        currentRoomCode = null
        _onlineState.value = OnlineState.Idle
        scope.cancel()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Move request payload — guest → host
// ─────────────────────────────────────────────────────────────────────────────

data class MoveRequestPayload(
    val type: String       = "",   // PLACE_TILE | PLAY_WORD | RECALL_ALL | SKIP | EXCHANGE | SHUFFLE
    val row: Int           = -1,
    val col: Int           = -1,
    val letter: String     = "",
    val blankAs: String    = "",
    val timestamp: Long    = 0L
)
