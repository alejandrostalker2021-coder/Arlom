package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.ComputedStats
import com.example.model.*
import com.example.net.CoopMode
import com.example.net.DiscoveredRoom
import com.example.pixel.PixelSprites
import com.example.ui.theme.*
import com.example.world.OpenWorldMap
import com.example.world.TileKind

@Composable
fun GameModalContainer(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .widthIn(max = 680.dp),
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, MoltenGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianCard)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = MoltenGold)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = ParchmentText)
                    }
                }
                HorizontalDivider(color = ObsidianBorder)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun QuestLogModal(
    profile: CharacterProfile,
    onDismiss: () -> Unit
) {
    GameModalContainer(title = "DIARIO DE MISIONES", onDismiss = onDismiss) {
        val allQuests = LootAndQuestData.quests
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(allQuests, key = { it.id }) { q ->
                val prog = profile.quests.firstOrNull { it.questId == q.id }
                val statusText = when {
                    prog?.turnedIn == true -> "COMPLETADA"
                    prog?.completed == true -> "LISTA PARA ENTREGAR"
                    prog != null -> "EN CURSO (${prog.currentCount}/${q.requiredCount})"
                    else -> "BLOQUEADA / DISPONIBLE EN PNJ"
                }
                val statusColor = when {
                    prog?.turnedIn == true -> MutedStone
                    prog?.completed == true -> MoltenGold
                    prog != null -> EmeraldHeal
                    else -> SapphireMana
                }
                val giver = LootAndQuestData.npcs.firstOrNull { it.id == q.giverNpcId }?.name ?: "Aldea"
                val turnIn = LootAndQuestData.npcs.firstOrNull { it.id == q.turnInNpcId }?.name ?: "Aldea"

                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(q.title, style = MaterialTheme.typography.titleSmall, color = ParchmentText)
                            Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(q.description, style = MaterialTheme.typography.bodyMedium, color = MutedStone)
                        if (prog != null && !prog.turnedIn) {
                            Spacer(Modifier.height(6.dp))
                            val pct = if (prog.completed) 1f else (prog.currentCount.toFloat() / q.requiredCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { pct },
                                color = if (prog.completed) MoltenGold else EmeraldHeal,
                                trackColor = ObsidianBg,
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "PNJ: $giver → Entregar a: $turnIn | Recompensa: ${q.rewardXp} XP, ${q.rewardGold} Oro" +
                                (q.rewardItem?.let { " + [${it.rarity.label}] ${it.name}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MoltenGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NpcDialogModal(
    npc: NpcDef,
    profile: CharacterProfile,
    onDialogueOption: (NpcDef, DialogueOption) -> String?,
    onBuyPotion: () -> Unit,
    onBuyChest: () -> Unit,
    onDismiss: () -> Unit
) {
    val tree = remember(npc.id) { DialogueCatalog.treeForNpc(npc) }
    var currentNodeId by remember(npc.id) { mutableStateOf(tree.rootSelector(profile)) }
    var feedbackMessage by remember(npc.id) { mutableStateOf<String?>(null) }
    var showForge by remember(npc.id) { mutableStateOf(false) }

    val currentNode = tree.nodes[currentNodeId] ?: tree.nodes.values.first()
    val visibleOptions = remember(currentNode.id, profile.quests, profile.gold, profile.level) {
        currentNode.options.filter { it.isAvailable(profile) }
    }

    GameModalContainer(title = npc.name.uppercase(), onDismiss = onDismiss) {
        // NPC Header with Pixel Portrait & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ObsidianCard)
                    .border(1.5.dp, Color(npc.tunicColorHex), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = PixelSprites.getNpc(npc.tunicColorHex),
                    contentDescription = "Retrato de ${npc.name}",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(currentNode.speakerTitle, style = MaterialTheme.typography.labelMedium, color = SapphireMana)
                Text(
                    "Oro: ${profile.gold}g  •  Pociones: ${profile.potions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MoltenGold
                )
            }
            if (npc.role == NpcRole.MERCHANT_BLACKSMITH) {
                OutlinedButton(
                    onClick = { showForge = !showForge },
                    border = BorderStroke(1.dp, MoltenGold),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_forge_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MoltenGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (showForge) "Diálogo" else "Forja",
                        color = MoltenGold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // NPC Speech Bubble
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.2.dp, MoltenGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialogue_npc_speech")
        ) {
            Text(
                "\"${currentNode.text}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = ParchmentText,
                modifier = Modifier.padding(14.dp)
            )
        }

        // Dynamic Consequence Feedback Banner
        if (feedbackMessage != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldHeal.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, EmeraldHeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialogue_feedback_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldHeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        feedbackMessage ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = ParchmentText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (showForge && npc.role == NpcRole.MERCHANT_BLACKSMITH) {
            Text(
                "FORJA Y SUMINISTROS (Tu Oro: ${profile.gold})",
                style = MaterialTheme.typography.labelMedium,
                color = MoltenGold
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onBuyPotion,
                    enabled = profile.gold >= 20,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_potion_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldHeal, contentColor = ObsidianBg)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Poción (20g)", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onBuyChest,
                    enabled = profile.gold >= 120,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_chest_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MoltenGold, contentColor = ObsidianBg)
                ) {
                    Text("Forjar Cofre (120g)", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            "ELIGE TU RESPUESTA",
            style = MaterialTheme.typography.labelSmall,
            color = MutedStone
        )
        Spacer(Modifier.height(6.dp))

        // Branching Response Options Tree
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(visibleOptions, key = { it.id }) { option ->
                val tagColor = when (option.tag) {
                    DialogueTagKind.TURN_IN -> MoltenGold
                    DialogueTagKind.QUEST -> SapphireMana
                    DialogueTagKind.OBJECTIVE -> EmeraldHeal
                    DialogueTagKind.PERSUASION -> Color(0xFFA855F7)
                    DialogueTagKind.FORGE -> WarmAmber
                    DialogueTagKind.BLESSING -> EmeraldHeal
                    DialogueTagKind.LORE -> MutedStone
                    DialogueTagKind.FAREWELL -> CrimsonHp.copy(alpha = 0.85f)
                }
                Surface(
                    onClick = {
                        val resultMsg = onDialogueOption(npc, option)
                        if (resultMsg != null) {
                            feedbackMessage = resultMsg
                        }
                        if (option.effect.type == DialogueEffectType.OPEN_FORGE) {
                            showForge = true
                        }
                        option.nextNodeId?.let { nextId ->
                            if (tree.nodes.containsKey(nextId)) {
                                currentNodeId = nextId
                            }
                        }
                    },
                    color = ObsidianCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, tagColor.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("dialogue_option_${option.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = tagColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, tagColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = option.tag.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = option.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParchmentText,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoopLobbyModal(
    coopMode: CoopMode,
    coopStatus: String,
    localIp: String,
    discoveredRooms: List<DiscoveredRoom>,
    remotePlayers: Map<String, RemotePlayerState>,
    onHostRoom: () -> Unit,
    onJoinIp: (String) -> Unit,
    onLeaveCoop: () -> Unit,
    onDismiss: () -> Unit
) {
    var manualIp by remember { mutableStateOf("192.168.43.1") }

    GameModalContainer(title = "COOP LOCAL WI-FI / HOTSPOT", onDismiss = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, if (coopMode == CoopMode.SOLO) ObsidianBorder else EmeraldHeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = if (coopMode == CoopMode.SOLO) MutedStone else EmeraldHeal)
                    Spacer(Modifier.width(8.dp))
                    Text(coopStatus, style = MaterialTheme.typography.titleSmall, color = ParchmentText)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tu IP local: $localIp (Comparte esta IP si usas Punto de Acceso)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MoltenGold
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (coopMode == CoopMode.SOLO) {
                Button(
                    onClick = onHostRoom,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("host_wifi_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MoltenGold, contentColor = ObsidianBg)
                ) {
                    Text("CREAR SALA (ANFITRIÓN)", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onLeaveCoop,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonHp, contentColor = Color.White)
                ) {
                    Text("DESCONECTAR SALA", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (coopMode == CoopMode.SOLO) {
            Spacer(Modifier.height(14.dp))
            Text("UNIRSE POR IP DIRECTA (WI-FI O PUNTO DE ACCESO):", style = MaterialTheme.typography.labelMedium, color = MoltenGold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("IP del Anfitrión") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onJoinIp(manualIp) },
                    modifier = Modifier.height(52.dp).testTag("join_ip_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireMana, contentColor = Color.White)
                ) {
                    Text("Unirse")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("SALAS DETECTADAS EN LA RED (${discoveredRooms.size}):", style = MaterialTheme.typography.labelMedium, color = MutedStone)
            Spacer(Modifier.height(6.dp))
            if (discoveredRooms.isEmpty()) {
                Text(
                    "Buscando anfitriones en Wi-Fi local... Si estás en Punto de Acceso, usa 192.168.43.1 arriba.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedStone
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(discoveredRooms, key = { it.hostIp }) { room ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, SapphireMana),
                            modifier = Modifier.fillMaxWidth().clickable { onJoinIp(room.hostIp) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Sala de ${room.hostName}", style = MaterialTheme.typography.titleSmall, color = ParchmentText)
                                    Text("${room.hostIp} · ${room.playerCount}/4 jugadores", style = MaterialTheme.typography.labelSmall, color = MoltenGold)
                                }
                                Text("UNIRSE", style = MaterialTheme.typography.labelMedium, color = EmeraldHeal)
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(14.dp))
            Text("ALIADOS CONECTADOS (${remotePlayers.size}):", style = MaterialTheme.typography.labelMedium, color = MoltenGold)
            Spacer(Modifier.height(6.dp))
            if (remotePlayers.isEmpty()) {
                Text("Esperando a que otros jugadores se conecten a $localIp...", color = MutedStone)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(remotePlayers.values.toList(), key = { it.playerId }) { rp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, EmeraldHeal)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${rp.name} (Nv.${rp.level} ${rp.heroClass.displayName})", color = ParchmentText)
                                Text("${rp.hp}/${rp.maxHp} HP", color = EmeraldHeal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldMapModal(
    playerX: Float,
    playerY: Float,
    remotePlayers: Map<String, RemotePlayerState>,
    onDismiss: () -> Unit
) {
    GameModalContainer(title = "MAPA DEL MUNDO ABIERTO", onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(ObsidianBg, RoundedCornerShape(10.dp))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / OpenWorldMap.WIDTH
                val cellH = size.height / OpenWorldMap.HEIGHT
                for (ty in 0 until OpenWorldMap.HEIGHT) {
                    for (tx in 0 until OpenWorldMap.WIDTH) {
                        val t = OpenWorldMap.getTile(tx, ty)
                        val col = when (t) {
                            TileKind.COBBLE -> Color(0xFF64748B)
                            TileKind.DIRT_ROAD, TileKind.WOOD_BRIDGE -> Color(0xFF926944)
                            TileKind.WATER -> Color(0xFF2563EB)
                            TileKind.TREE -> Color(0xFF14532D)
                            TileKind.GRASS -> Color(0xFF2E5C31)
                            TileKind.RUIN_FLOOR -> Color(0xFF3B3652)
                            TileKind.SWAMP_MUD, TileKind.SWAMP_BOG -> Color(0xFF233831)
                            TileKind.VOLCANIC_ROCK -> Color(0xFF3F2326)
                            TileKind.LAVA -> Color(0xFFEA580C)
                            TileKind.HEAL_FOUNTAIN -> Color(0xFF10B981)
                            else -> Color(0xFF1E293B)
                        }
                        drawRect(
                            color = col,
                            topLeft = Offset(tx * cellW, ty * cellH),
                            size = Size(cellW + 0.5f, cellH + 0.5f)
                        )
                    }
                }
                // Dragon Boss marker
                drawCircle(
                    color = CrimsonHp,
                    radius = cellW * 2.1f,
                    center = Offset(60f * cellW, 60f * cellH)
                )
                // Allies
                remotePlayers.values.forEach { rp ->
                    drawCircle(
                        color = SapphireMana,
                        radius = cellW * 1.8f,
                        center = Offset(rp.x * cellW, rp.y * cellH)
                    )
                }
                // Local Player
                drawCircle(
                    color = MoltenGold,
                    radius = cellW * 2.2f,
                    center = Offset(playerX * cellW, playerY * cellH)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Dorado: Tú  |  Azul: Aliados Coop  |  Rojo: Jefe Wyrm Nv.30\nRegiones: Noroeste Valoria -> Noreste Bosque/Ruinas -> Sur Pantano -> Sureste Cumbre del Dragón",
            style = MaterialTheme.typography.labelSmall,
            color = MutedStone
        )
    }
}
