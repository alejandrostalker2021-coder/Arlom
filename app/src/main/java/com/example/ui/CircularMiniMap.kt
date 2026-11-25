package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.WorldRenderState
import com.example.model.*
import com.example.ui.theme.*
import com.example.world.OpenWorldMap
import com.example.world.TileKind
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

enum class MiniMapMarkerKind {
    QUEST_TURN_IN,
    QUEST_OBJECTIVE,
    QUEST_AVAILABLE,
    NPC,
    CHEST,
    ENEMY,
    BOSS,
    ALLY
}

data class MiniMapMarker(
    val id: String,
    val label: String,
    val worldX: Float,
    val worldY: Float,
    val kind: MiniMapMarkerKind,
    val normalizedX: Float,
    val normalizedY: Float,
    val isClampedToEdge: Boolean
)

fun zoneCenterCoords(zone: ZoneId): Pair<Float, Float> = when (zone) {
    ZoneId.VALORIA_VILLAGE -> 12f to 12f
    ZoneId.EMERALD_FOREST -> 34f to 17f
    ZoneId.TWILIGHT_RUINS -> 58f to 18f
    ZoneId.CURSED_SWAMP -> 22f to 52f
    ZoneId.DRAGON_SUMMIT -> 59f to 58f
}

fun projectToMiniMap(
    playerX: Float,
    playerY: Float,
    targetX: Float,
    targetY: Float,
    radiusTiles: Float,
    clampToEdge: Boolean
): Triple<Float, Float, Boolean>? {
    val dx = targetX - playerX
    val dy = targetY - playerY
    val dist = hypot(dx, dy)
    if (dist <= radiusTiles) {
        return Triple(dx / radiusTiles, dy / radiusTiles, false)
    }
    if (!clampToEdge || dist <= 0.001f) return null
    val edgeScale = 0.88f
    return Triple((dx / dist) * edgeScale, (dy / dist) * edgeScale, true)
}

fun buildMiniMapMarkers(
    profile: CharacterProfile,
    world: WorldRenderState,
    remotePlayers: Map<String, RemotePlayerState>,
    radiusTiles: Float = 15f
): List<MiniMapMarker> {
    val markers = mutableListOf<MiniMapMarker>()
    val questNpcIds = mutableSetOf<String>()

    fun addMarker(
        id: String,
        label: String,
        wx: Float,
        wy: Float,
        kind: MiniMapMarkerKind,
        clampToEdge: Boolean
    ) {
        val proj = projectToMiniMap(world.playerX, world.playerY, wx, wy, radiusTiles, clampToEdge) ?: return
        markers += MiniMapMarker(
            id = id,
            label = label,
            worldX = wx,
            worldY = wy,
            kind = kind,
            normalizedX = proj.first,
            normalizedY = proj.second,
            isClampedToEdge = proj.third
        )
    }

    // 1. Active Quests (turn-in NPCs & active objectives — clamped to rim when far so they guide the player)
    profile.quests.filter { !it.turnedIn }.forEach { qp ->
        val def = LootAndQuestData.quests.firstOrNull { it.id == qp.questId } ?: return@forEach
        if (qp.completed) {
            val npc = LootAndQuestData.npcs.firstOrNull { it.id == def.turnInNpcId }
            if (npc != null) {
                questNpcIds += npc.id
                addMarker("q_turn_${def.id}", npc.name, npc.x, npc.y, MiniMapMarkerKind.QUEST_TURN_IN, clampToEdge = true)
            }
        } else {
            when (def.objectiveType) {
                QuestObjectiveType.TALK_NPC -> {
                    val targetNpc = LootAndQuestData.npcs.firstOrNull { it.id == def.targetNpcId }
                    if (targetNpc != null) {
                        questNpcIds += targetNpc.id
                        addMarker("q_talk_${def.id}", targetNpc.name, targetNpc.x, targetNpc.y, MiniMapMarkerKind.QUEST_OBJECTIVE, clampToEdge = true)
                    }
                }
                QuestObjectiveType.KILL_ENEMY -> {
                    val nearestTarget = world.enemies
                        .filter { it.isAlive && it.kind == def.targetEnemyKind }
                        .minByOrNull { hypot(it.x - world.playerX, it.y - world.playerY) }
                    if (nearestTarget != null) {
                        addMarker("q_kill_${def.id}", nearestTarget.kind.displayName, nearestTarget.x, nearestTarget.y, MiniMapMarkerKind.QUEST_OBJECTIVE, clampToEdge = true)
                    }
                }
                QuestObjectiveType.REACH_ZONE -> {
                    def.targetZone?.let { tz ->
                        val (zx, zy) = zoneCenterCoords(tz)
                        addMarker("q_zone_${def.id}", tz.displayName, zx, zy, MiniMapMarkerKind.QUEST_OBJECTIVE, clampToEdge = true)
                    }
                }
            }
        }
    }

    // 2. Available Quests at NPCs
    LootAndQuestData.quests.forEach { q ->
        val canAccept = profile.quests.none { it.questId == q.id } &&
            (q.prerequisiteQuestId == null || profile.quests.any { it.questId == q.prerequisiteQuestId && it.turnedIn })
        if (canAccept) {
            val giver = LootAndQuestData.npcs.firstOrNull { it.id == q.giverNpcId }
            if (giver != null && giver.id !in questNpcIds) {
                questNpcIds += giver.id
                addMarker("q_avail_${q.id}", giver.name, giver.x, giver.y, MiniMapMarkerKind.QUEST_AVAILABLE, clampToEdge = true)
            }
        }
    }

    // 3. Nearby NPCs
    LootAndQuestData.npcs.forEach { npc ->
        if (npc.id !in questNpcIds) {
            addMarker("npc_${npc.id}", npc.name, npc.x, npc.y, MiniMapMarkerKind.NPC, clampToEdge = false)
        }
    }

    // 4. Nearby Unopened Chests
    world.chests.filter { !it.opened }.forEach { ch ->
        addMarker("chest_${ch.id}", "Cofre", ch.x, ch.y, MiniMapMarkerKind.CHEST, clampToEdge = false)
    }

    // 5. Nearby Alive Enemies & Zone Bosses
    world.enemies.filter { it.isAlive }.forEach { en ->
        val kind = if (en.kind.isBoss) MiniMapMarkerKind.BOSS else MiniMapMarkerKind.ENEMY
        addMarker("en_${en.id}", en.kind.displayName, en.x, en.y, kind, clampToEdge = false)
    }

    // 6. Co-op Allies (clamped to rim so teammates remain easy to locate)
    remotePlayers.values.forEach { rp ->
        addMarker("ally_${rp.playerId}", rp.name, rp.x, rp.y, MiniMapMarkerKind.ALLY, clampToEdge = true)
    }

    return markers
}

@Composable
fun CircularMiniMap(
    profile: CharacterProfile,
    world: WorldRenderState,
    remotePlayers: Map<String, RemotePlayerState>,
    onOpenWorldMap: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 112.dp,
    radiusTiles: Float = 15f
) {
    val markers = remember(
        world.playerX,
        world.playerY,
        world.enemies,
        world.chests,
        profile.quests,
        remotePlayers
    ) {
        buildMiniMapMarkers(profile, world, remotePlayers, radiusTiles)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = ObsidianBg,
            border = BorderStroke(2.dp, MoltenGold.copy(alpha = 0.85f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(sizeDp)
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(onClick = onOpenWorldMap)
                .testTag("circular_minimap")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val mapRadiusPx = size.minDimension / 2f
                    val pxPerTile = mapRadiusPx / radiusTiles

                    // 1. Render surrounding world terrain tiles relative to player position
                    val minTx = floor(world.playerX - radiusTiles - 1f).toInt().coerceAtLeast(0)
                    val maxTx = ceil(world.playerX + radiusTiles + 1f).toInt().coerceAtMost(OpenWorldMap.WIDTH - 1)
                    val minTy = floor(world.playerY - radiusTiles - 1f).toInt().coerceAtLeast(0)
                    val maxTy = ceil(world.playerY + radiusTiles + 1f).toInt().coerceAtMost(OpenWorldMap.HEIGHT - 1)

                    for (ty in minTy..maxTy) {
                        for (tx in minTx..maxTx) {
                            val screenX = cx + (tx - world.playerX) * pxPerTile
                            val screenY = cy + (ty - world.playerY) * pxPerTile
                            if (hypot(screenX - cx, screenY - cy) > mapRadiusPx + pxPerTile) continue

                            val tileColor = when (OpenWorldMap.getTile(tx, ty)) {
                                TileKind.GRASS -> Color(0xFF284F2B)
                                TileKind.COBBLE -> Color(0xFF5A6577)
                                TileKind.DIRT_ROAD -> Color(0xFF8C623D)
                                TileKind.WOOD_BRIDGE -> Color(0xFFA47148)
                                TileKind.WATER -> Color(0xFF1D4ED8)
                                TileKind.TREE -> Color(0xFF144224)
                                TileKind.STONE_WALL -> Color(0xFF334155)
                                TileKind.RUIN_FLOOR -> Color(0xFF36314A)
                                TileKind.SWAMP_MUD -> Color(0xFF1F352E)
                                TileKind.SWAMP_BOG -> Color(0xFF12231E)
                                TileKind.VOLCANIC_ROCK -> Color(0xFF3B2226)
                                TileKind.LAVA -> Color(0xFFEA580C)
                                TileKind.HOUSE_WALL -> Color(0xFF6B4226)
                                TileKind.HEAL_FOUNTAIN -> Color(0xFF10B981)
                            }
                            drawRect(
                                color = tileColor,
                                topLeft = Offset(screenX, screenY),
                                size = Size(pxPerTile + 0.8f, pxPerTile + 0.8f)
                            )
                        }
                    }

                    // 2. Subtle radar rings and crosshair
                    drawCircle(
                        color = ParchmentText.copy(alpha = 0.14f),
                        radius = mapRadiusPx * 0.5f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = MoltenGold.copy(alpha = 0.30f),
                        radius = mapRadiusPx - 1.5f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )

                    // 3. Points of Interest (Enemies, Chests, NPCs, Allies, Quest Markers)
                    markers.forEach { marker ->
                        val mx = cx + marker.normalizedX * mapRadiusPx
                        val my = cy + marker.normalizedY * mapRadiusPx
                        when (marker.kind) {
                            MiniMapMarkerKind.ENEMY -> {
                                drawCircle(
                                    color = CrimsonHp,
                                    radius = 3.0f,
                                    center = Offset(mx, my)
                                )
                            }
                            MiniMapMarkerKind.BOSS -> {
                                val r = 5.4f
                                val bossDiamond = Path().apply {
                                    moveTo(mx, my - r)
                                    lineTo(mx + r, my)
                                    lineTo(mx, my + r)
                                    lineTo(mx - r, my)
                                    close()
                                }
                                drawPath(path = bossDiamond, color = CrimsonHp)
                                drawPath(path = bossDiamond, color = MoltenGold, style = Stroke(width = 1.6f))
                            }
                            MiniMapMarkerKind.CHEST -> {
                                drawRect(
                                    color = WarmAmber,
                                    topLeft = Offset(mx - 3f, my - 3f),
                                    size = Size(6f, 6f)
                                )
                            }
                            MiniMapMarkerKind.NPC -> {
                                drawCircle(
                                    color = EmeraldHeal,
                                    radius = 4.2f,
                                    center = Offset(mx, my)
                                )
                                drawCircle(
                                    color = ObsidianBg,
                                    radius = 4.2f,
                                    center = Offset(mx, my),
                                    style = Stroke(width = 1.2f)
                                )
                            }
                            MiniMapMarkerKind.ALLY -> {
                                drawCircle(
                                    color = Color(0xFF38BDF8),
                                    radius = 4.5f,
                                    center = Offset(mx, my)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 4.5f,
                                    center = Offset(mx, my),
                                    style = Stroke(width = 1.2f)
                                )
                            }
                            MiniMapMarkerKind.QUEST_OBJECTIVE, MiniMapMarkerKind.QUEST_AVAILABLE -> {
                                val col = if (marker.kind == MiniMapMarkerKind.QUEST_AVAILABLE) {
                                    MoltenGold
                                } else {
                                    Color(0xFF38BDF8)
                                }
                                if (marker.isClampedToEdge) {
                                    drawCircle(
                                        color = col.copy(alpha = 0.35f),
                                        radius = 7.5f,
                                        center = Offset(mx, my)
                                    )
                                }
                                drawCircle(
                                    color = col,
                                    radius = 4.8f,
                                    center = Offset(mx, my)
                                )
                                drawCircle(
                                    color = ObsidianBg,
                                    radius = 4.8f,
                                    center = Offset(mx, my),
                                    style = Stroke(width = 1.4f)
                                )
                            }
                            MiniMapMarkerKind.QUEST_TURN_IN -> {
                                drawCircle(
                                    color = MoltenGold.copy(alpha = 0.42f),
                                    radius = 8.5f,
                                    center = Offset(mx, my)
                                )
                                drawCircle(
                                    color = MoltenGold,
                                    radius = 5.4f,
                                    center = Offset(mx, my)
                                )
                                drawCircle(
                                    color = ObsidianBg,
                                    radius = 5.4f,
                                    center = Offset(mx, my),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }
                    }

                    // 4. Local Player Center Directional Arrow
                    val dir = if (world.facingRight) 1f else -1f
                    val playerPath = Path().apply {
                        moveTo(cx + 6.5f * dir, cy)
                        lineTo(cx - 4.5f * dir, cy - 4.5f)
                        lineTo(cx - 2.2f * dir, cy)
                        lineTo(cx - 4.5f * dir, cy + 4.5f)
                        close()
                    }
                    drawPath(path = playerPath, color = MoltenGold)
                    drawPath(path = playerPath, color = Color.White, style = Stroke(width = 1.4f))
                }

                // North compass badge at top rim
                Text(
                    text = "N",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MoltenGold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )
            }
        }

        // Compact world coordinates & legend pill
        Surface(
            color = ObsidianSurface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.offset(y = (-6).dp)
        ) {
            Text(
                text = "(${world.playerX.toInt()}, ${world.playerY.toInt()})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = ParchmentText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}
