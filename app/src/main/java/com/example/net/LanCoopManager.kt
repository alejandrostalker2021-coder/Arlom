package com.example.net

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.HeroClass
import com.example.model.Race
import com.example.model.RemotePlayerState
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

const val COOP_TCP_PORT = 29450
const val COOP_UDP_PORT = 29451

enum class CoopMode {
    SOLO,
    HOST,
    CLIENT
}

data class DiscoveredRoom(
    val hostIp: String,
    val hostName: String,
    val playerCount: Int,
    val lastSeenMs: Long
)

@JsonClass(generateAdapter = true)
data class CoopEnemySync(
    val id: String,
    val hp: Int,
    val x: Float,
    val y: Float
)

@JsonClass(generateAdapter = true)
data class CoopEnvelope(
    val type: String, // "PLAYER_SYNC", "WORLD_SYNC", "DAMAGE_ENEMY", "PARTY_HEAL"
    val senderId: String,
    val player: RemotePlayerState? = null,
    val players: List<RemotePlayerState>? = null,
    val enemies: List<CoopEnemySync>? = null,
    val targetEnemyId: String? = null,
    val amount: Int = 0
)

@JsonClass(generateAdapter = true)
data class UdpBeacon(
    val hostIp: String,
    val hostName: String,
    val playerCount: Int
)

class LanCoopManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val envAdapter = moshi.adapter(CoopEnvelope::class.java)
    private val beaconAdapter = moshi.adapter(UdpBeacon::class.java)

    private val _mode = MutableStateFlow(CoopMode.SOLO)
    val mode: StateFlow<CoopMode> = _mode.asStateFlow()

    private val _statusMessage = MutableStateFlow("Modo solitario")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _localIp = MutableStateFlow(findLocalIpv4Address())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    private val _remotePlayers = MutableStateFlow<Map<String, RemotePlayerState>>(emptyMap())
    val remotePlayers: StateFlow<Map<String, RemotePlayerState>> = _remotePlayers.asStateFlow()

    // Callbacks into GameViewModel
    var onRemoteEnemyDamage: ((enemyId: String, damage: Int, attackerId: String) -> Unit)? = null
    var onRemotePartyHeal: ((amount: Int, healerName: String) -> Unit)? = null
    var onWorldEnemySnapshot: ((List<CoopEnemySync>) -> Unit)? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private val connectedClients = CopyOnWriteArrayList<ClientSession>()
    private var multicastLock: WifiManager.MulticastLock? = null

    private var networkJobs = mutableListOf<Job>()

    private class ClientSession(
        val socket: Socket,
        val writer: BufferedWriter,
        var playerId: String? = null
    )

    fun refreshLocalIp() {
        _localIp.value = findLocalIpv4Address()
    }

    fun startHost(hostPlayerName: String, getHostSnapshot: () -> CoopEnvelope) {
        stopAll()
        refreshLocalIp()
        _mode.value = CoopMode.HOST
        _statusMessage.value = "Sala activa en ${_localIp.value}:$COOP_TCP_PORT"
        acquireMulticastLock()

        val acceptJob = scope.launch(Dispatchers.IO) {
            try {
                val ss = ServerSocket(COOP_TCP_PORT)
                serverSocket = ss
                while (isActive && !ss.isClosed) {
                    val sock = ss.accept()
                    if (connectedClients.size >= 3) {
                        sock.close()
                        continue
                    }
                    val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
                    val session = ClientSession(sock, writer)
                    connectedClients.add(session)
                    launchClientReader(session)
                }
            } catch (e: Exception) {
                if (_mode.value == CoopMode.HOST) {
                    _statusMessage.value = "Error anfitrión: ${e.localizedMessage ?: "Red cerrada"}"
                }
            }
        }

        val broadcastJob = scope.launch(Dispatchers.IO) {
            var udpSock: DatagramSocket? = null
            try {
                udpSock = DatagramSocket()
                udpSock.broadcast = true
                while (isActive && _mode.value == CoopMode.HOST) {
                    val myIp = findLocalIpv4Address()
                    _localIp.value = myIp
                    val beacon = beaconAdapter.toJson(
                        UdpBeacon(
                            hostIp = myIp,
                            hostName = hostPlayerName,
                            playerCount = 1 + connectedClients.size
                        )
                    )
                    val bytes = beacon.toByteArray(Charsets.UTF_8)
                    val targets = listOf("255.255.255.255", deriveSubnetBroadcast(myIp))
                    for (addrStr in targets.distinct()) {
                        runCatching {
                            val addr = InetAddress.getByName(addrStr)
                            udpSock.send(DatagramPacket(bytes, bytes.size, addr, COOP_UDP_PORT))
                        }
                    }

                    // Also send world sync to all TCP guests
                    if (connectedClients.isNotEmpty()) {
                        val snapshotJson = envAdapter.toJson(getHostSnapshot())
                        broadcastLineToGuests(snapshotJson)
                    }
                    delay(120L)
                }
            } catch (_: Exception) {
            } finally {
                udpSock?.close()
            }
        }

        networkJobs.add(acceptJob)
        networkJobs.add(broadcastJob)
    }

    private fun launchClientReader(session: ClientSession) {
        val job = scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(session.socket.getInputStream()))
                while (isActive && !session.socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val env = runCatching { envAdapter.fromJson(line) }.getOrNull() ?: continue
                    when (env.type) {
                        "PLAYER_SYNC" -> {
                            env.player?.let { p ->
                                session.playerId = p.playerId
                                val next = _remotePlayers.value.toMutableMap()
                                next[p.playerId] = p
                                _remotePlayers.value = next
                            }
                        }
                        "DAMAGE_ENEMY" -> {
                            val enemyId = env.targetEnemyId
                            if (enemyId != null && env.amount > 0) {
                                onRemoteEnemyDamage?.invoke(enemyId, env.amount, env.senderId)
                            }
                        }
                        "PARTY_HEAL" -> {
                            if (env.amount > 0) {
                                onRemotePartyHeal?.invoke(env.amount, env.player?.name ?: "Aliado")
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                connectedClients.remove(session)
                session.playerId?.let { pid ->
                    val next = _remotePlayers.value.toMutableMap()
                    next.remove(pid)
                    _remotePlayers.value = next
                }
                runCatching { session.socket.close() }
            }
        }
        networkJobs.add(job)
    }

    fun startDiscoveryScan() {
        acquireMulticastLock()
        val scanJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(COOP_UDP_PORT))
                    soTimeout = 1500
                }
                val buf = ByteArray(1024)
                while (isActive && _mode.value == CoopMode.SOLO) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val beacon = runCatching { beaconAdapter.fromJson(json) }.getOrNull()
                        if (beacon != null) {
                            val now = System.currentTimeMillis()
                            val current = _discoveredRooms.value.associateBy { it.hostIp }.toMutableMap()
                            val ip = if (beacon.hostIp == "127.0.0.1" || beacon.hostIp.isBlank()) {
                                packet.address.hostAddress ?: beacon.hostIp
                            } else beacon.hostIp
                            current[ip] = DiscoveredRoom(ip, beacon.hostName, beacon.playerCount, now)
                            _discoveredRooms.value = current.values.filter { now - it.lastSeenMs < 6000 }
                        }
                    } catch (_: Exception) {
                        val now = System.currentTimeMillis()
                        _discoveredRooms.value = _discoveredRooms.value.filter { now - it.lastSeenMs < 6000 }
                    }
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
            }
        }
        networkJobs.add(scanJob)
    }

    fun connectToHost(hostIp: String, getLocalPlayer: () -> RemotePlayerState) {
        stopAll()
        val cleanIp = hostIp.trim()
        if (cleanIp.isEmpty()) {
            _statusMessage.value = "Ingresa una IP válida"
            return
        }
        _mode.value = CoopMode.CLIENT
        _statusMessage.value = "Conectando a $cleanIp..."

        val clientJob = scope.launch(Dispatchers.IO) {
            try {
                val sock = Socket()
                sock.connect(InetSocketAddress(cleanIp, COOP_TCP_PORT), 4500)
                clientSocket = sock
                clientWriter = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
                _statusMessage.value = "Conectado al grupo en $cleanIp"

                // Sender loop
                launch {
                    while (isActive && _mode.value == CoopMode.CLIENT && !sock.isClosed) {
                        val lp = getLocalPlayer()
                        sendToHost(
                            CoopEnvelope(
                                type = "PLAYER_SYNC",
                                senderId = lp.playerId,
                                player = lp
                            )
                        )
                        delay(100L)
                    }
                }

                // Reader loop
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                while (isActive && !sock.isClosed) {
                    val line = reader.readLine() ?: break
                    val env = runCatching { envAdapter.fromJson(line) }.getOrNull() ?: continue
                    if (env.type == "WORLD_SYNC") {
                        val localId = getLocalPlayer().playerId
                        val remotes = (env.players ?: emptyList())
                            .filter { it.playerId != localId }
                            .associateBy { it.playerId }
                        _remotePlayers.value = remotes
                        env.enemies?.let { onWorldEnemySnapshot?.invoke(it) }
                    } else if (env.type == "PARTY_HEAL" && env.amount > 0) {
                        onRemotePartyHeal?.invoke(env.amount, env.player?.name ?: "Aliado")
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "Desconectado: ${e.localizedMessage ?: "Sin conexión"}"
                _mode.value = CoopMode.SOLO
                _remotePlayers.value = emptyMap()
            }
        }
        networkJobs.add(clientJob)
    }

    fun sendDamageEvent(senderId: String, enemyId: String, amount: Int) {
        if (_mode.value != CoopMode.CLIENT || amount <= 0) return
        scope.launch(Dispatchers.IO) {
            sendToHost(
                CoopEnvelope(
                    type = "DAMAGE_ENEMY",
                    senderId = senderId,
                    targetEnemyId = enemyId,
                    amount = amount
                )
            )
        }
    }

    fun sendPartyHealEvent(sender: RemotePlayerState, amount: Int) {
        if (_mode.value == CoopMode.SOLO || amount <= 0) return
        val json = envAdapter.toJson(
            CoopEnvelope(
                type = "PARTY_HEAL",
                senderId = sender.playerId,
                player = sender,
                amount = amount
            )
        )
        scope.launch(Dispatchers.IO) {
            if (_mode.value == CoopMode.HOST) {
                broadcastLineToGuests(json)
            } else if (_mode.value == CoopMode.CLIENT) {
                runCatching {
                    clientWriter?.apply {
                        write(json)
                        newLine()
                        flush()
                    }
                }
            }
        }
    }

    private fun sendToHost(env: CoopEnvelope) {
        val json = envAdapter.toJson(env)
        runCatching {
            clientWriter?.apply {
                write(json)
                newLine()
                flush()
            }
        }
    }

    private fun broadcastLineToGuests(line: String) {
        for (client in connectedClients) {
            runCatching {
                client.writer.write(line)
                client.writer.newLine()
                client.writer.flush()
            }.onFailure {
                connectedClients.remove(client)
                runCatching { client.socket.close() }
            }
        }
    }

    fun stopAll() {
        networkJobs.forEach { it.cancel() }
        networkJobs.clear()
        connectedClients.forEach { runCatching { it.socket.close() } }
        connectedClients.clear()
        runCatching { serverSocket?.close() }
        runCatching { clientSocket?.close() }
        serverSocket = null
        clientSocket = null
        clientWriter = null
        _remotePlayers.value = emptyMap()
        _mode.value = CoopMode.SOLO
        _statusMessage.value = "Modo solitario"
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        runCatching {
            if (multicastLock == null) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wm?.createMulticastLock("aethelgard_lan_coop")?.apply {
                    setReferenceCounted(true)
                }
            }
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
            }
        }
    }

    private fun releaseMulticastLock() {
        runCatching {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        }
    }

    private fun findLocalIpv4Address(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "192.168.43.1"
            var fallback: String? = null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (intf.name.contains("wlan", ignoreCase = true) ||
                            intf.name.contains("ap", ignoreCase = true)
                        ) {
                            return host
                        }
                        fallback = host
                    }
                }
            }
            fallback ?: "192.168.43.1"
        }.getOrDefault("192.168.43.1")
    }

    private fun deriveSubnetBroadcast(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.255" else "255.255.255.255"
    }
}
