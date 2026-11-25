package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AppScreen
import com.example.engine.BossTactics
import com.example.engine.GameModal
import com.example.engine.GameViewModel
import com.example.engine.WorldRenderState
import com.example.model.*
import com.example.net.CoopMode
import com.example.pixel.PixelSprites
import com.example.ui.theme.*
import com.example.world.OpenWorldMap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GameWorldScreen(
    viewModel: GameViewModel,
    profile: CharacterProfile,
    world: WorldRenderState,
    activeModal: GameModal,
    activeNpc: NpcDef?,
    coopMode: CoopMode,
    remotePlayers: Map<String, RemotePlayerState>
) {
    BackHandler {
        if (activeModal != GameModal.NONE) {
            viewModel.closeModal()
        } else {
            viewModel.navigateTo(AppScreen.TITLE_SELECT)
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val classSkills = remember(profile.heroClass) { LootAndQuestData.skillsForClass(profile.heroClass) }
    val stats = remember(profile, world.buffs) { viewModel.computeStats(profile, world.buffs) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Open World Pixel-Art Canvas
        OpenWorldCanvas(
            profile = profile,
            world = world,
            remotePlayers = remotePlayers,
            textMeasurer = textMeasurer,
            onTapEnemy = { viewModel.selectEnemy(it) }
        )

        // 2. Top HUD Overlay (safe area aware)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Dedicated Health & Mana Status Bar HUD
                HealthManaStatusBar(profile = profile, world = world)

                // Top-Right Action Bar (Bag, Quests, Map, Coop, Exit)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudSquareButton(
                        icon = Icons.Default.Backpack,
                        label = "${profile.inventory.size}",
                        tint = MoltenGold,
                        tag = "open_bag_button",
                        onClick = { viewModel.openModal(GameModal.INVENTORY) }
                    )
                    HudSquareButton(
                        icon = Icons.Default.AutoStories,
                        label = "Mis.",
                        tint = SapphireMana,
                        tag = "open_quests_button",
                        onClick = { viewModel.openModal(GameModal.QUESTS) }
                    )
                    HudSquareButton(
                        icon = Icons.Default.Map,
                        label = "Mapa",
                        tint = EmeraldHeal,
                        tag = "open_map_button",
                        onClick = { viewModel.openModal(GameModal.WORLD_MAP) }
                    )
                    HudSquareButton(
                        icon = Icons.Default.Wifi,
                        label = if (coopMode == CoopMode.SOLO) "Coop" else "${remotePlayers.size + 1}/4",
                        tint = if (coopMode == CoopMode.SOLO) MutedStone else EmeraldHeal,
                        tag = "open_coop_button",
                        onClick = { viewModel.openModal(GameModal.COOP_LOBBY) }
                    )
                    HudSquareButton(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Salir",
                        tint = CrimsonHp,
                        tag = "exit_world_button",
                        onClick = { viewModel.navigateTo(AppScreen.TITLE_SELECT) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Zone Badge + Active Quest Tracker + Selected Enemy Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = ObsidianSurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = "${world.currentZone.displayName} (${world.currentZone.levelRange})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MoltenGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Dynamic Active Quest Tracker
                    DynamicQuestTracker(
                        profile = profile,
                        onOpenQuestLog = { viewModel.openModal(GameModal.QUESTS) }
                    )

                    // Co-op Party Mini-Frames
                    remotePlayers.values.take(3).forEach { rp ->
                        Surface(
                            color = ObsidianSurface.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmeraldHeal.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${rp.name} Nv.${rp.level}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ParchmentText
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${rp.hp}/${rp.maxHp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldHeal
                                )
                            }
                        }
                    }
                }

                // Right HUD Column: Circular Mini-Map + Selected Enemy Target Frame
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CircularMiniMap(
                        profile = profile,
                        world = world,
                        remotePlayers = remotePlayers,
                        onOpenWorldMap = { viewModel.openModal(GameModal.WORLD_MAP) }
                    )

                    val targetEn = world.selectedEnemyId?.let { id ->
                        world.enemies.firstOrNull { it.id == id && it.isAlive }
                    }
                    if (targetEn != null) {
                        Surface(
                            color = ObsidianSurface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CrimsonHp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).width(164.dp)) {
                                val stateColor = when (targetEn.aiState) {
                                    EnemyAiState.IDLE -> MutedStone
                                    EnemyAiState.CHASE -> WarmAmber
                                    EnemyAiState.ATTACK -> CrimsonHp
                                    EnemyAiState.FLEE -> Color(0xFF38BDF8)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${targetEn.kind.displayName} (Nv.${targetEn.effectiveLevel})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ParchmentText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        color = stateColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, stateColor.copy(alpha = 0.7f)),
                                        modifier = Modifier.testTag("target_enemy_ai_state")
                                    ) {
                                        Text(
                                            text = targetEn.aiState.displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = stateColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (targetEn.hp.toFloat() / targetEn.maxHp).coerceIn(0f, 1f) },
                                    color = if (targetEn.kind.isBoss) MoltenGold else CrimsonHp,
                                    trackColor = ObsidianBg,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (targetEn.kind.isBoss) {
                                        "Recompensa: +${targetEn.scaledXpReward} XP (x4 Jefe)"
                                    } else {
                                        "Recompensa: +${targetEn.scaledXpReward} XP"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (targetEn.kind.isBoss) MoltenGold else MutedStone,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (targetEn.kind.isBoss && targetEn.kind.bossTactic != null) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "Jefe · ${targetEn.kind.bossTactic.displayName}: ${targetEn.kind.bossTactic.dodgeHint}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarmAmber,
                                        maxLines = 2
                                    )
                                }
                                val tags = buildList {
                                    if (targetEn.stunnedSec > 0f) add("ATURDIDO")
                                    if (targetEn.rootedSec > 0f) add("ATRAPADO")
                                    if (targetEn.slowedSec > 0f) add("LENTO")
                                    if (targetEn.poisonSec > 0f) add("VENENO")
                                    if (targetEn.forcedDuelSec > 0f) add("DUELO 1v1")
                                }
                                if (tags.isNotEmpty()) {
                                    Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MoltenGold)
                                }
                            }
                        }
                    }

                    val activeWarning = world.bossTelegraphs.firstOrNull {
                        hypot(it.originX - world.playerX, it.originY - world.playerY) < 9f
                    }
                    if (activeWarning != null) {
                        Surface(
                            color = CrimsonHp.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.2.dp, WarmAmber),
                            modifier = Modifier.testTag("boss_telegraph_warning")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).width(164.dp)) {
                                Text(
                                    text = "${activeWarning.tactic.warningCallout} (${activeWarning.bossName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmAmber,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeWarning.tactic.dodgeHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ParchmentText,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Touch Controls (Virtual D-Pad + Quick-Action Skill Cluster)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Left: Tactile Virtual D-Pad
            VirtualDPad(
                modifier = Modifier.align(Alignment.BottomStart),
                onDirectionChanged = { dx, dy -> viewModel.setJoystick(dx, dy) }
            )

            // Center-bottom compact combat log / nearby interact prompt
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (world.nearbyNpcId != null || world.nearbyChestId != null) {
                    val promptText = if (world.nearbyNpcId != null) {
                        val npcName = LootAndQuestData.npcs.firstOrNull { it.id == world.nearbyNpcId }?.name ?: "PNJ"
                        "Hablar con $npcName"
                    } else {
                        "Abrir Cofre"
                    }
                    Button(
                        onClick = { viewModel.interactNearby() },
                        colors = ButtonDefaults.buttonColors(containerColor = MoltenGold, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(48.dp).testTag("interact_button")
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(promptText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                } else if (world.combatLog.isNotEmpty()) {
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = world.combatLog.last(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ParchmentText.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right: Touch-Optimized Quick-Action Skill Cluster
            val skillStates = classSkills.indices.map { idx ->
                viewModel.evaluateSkillSlotState(idx, profile, world)
            }
            val racialState = viewModel.evaluateRacialSlotState(profile, world)
            QuickActionSkillCluster(
                profile = profile,
                world = world,
                classSkills = classSkills,
                skillStates = skillStates,
                racialState = racialState,
                onBasicAttack = { viewModel.performBasicAttack() },
                onUseSkill = { idx -> viewModel.useSkill(idx) },
                onUseRacial = { viewModel.useRacialSkill() },
                onUsePotion = { viewModel.usePotion() },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // 4. Modals
        when (activeModal) {
            GameModal.INVENTORY -> PlayerInventoryModal(
                profile = profile,
                stats = stats,
                onEquip = { viewModel.equipItem(it) },
                onUnequipSlot = { viewModel.unequipSlot(it) },
                onSell = { viewModel.sellItem(it) },
                onDismiss = { viewModel.closeModal() }
            )
            GameModal.QUESTS -> QuestLogModal(
                profile = profile,
                onDismiss = { viewModel.closeModal() }
            )
            GameModal.NPC_DIALOG -> if (activeNpc != null) {
                NpcDialogModal(
                    npc = activeNpc,
                    profile = profile,
                    onDialogueOption = { n, opt -> viewModel.executeDialogueOption(n, opt) },
                    onBuyPotion = { viewModel.buyPotion() },
                    onBuyChest = { viewModel.buyBlacksmithChest() },
                    onDismiss = { viewModel.closeModal() }
                )
            }
            GameModal.COOP_LOBBY -> CoopLobbyModal(
                coopMode = coopMode,
                coopStatus = viewModel.coopStatus.collectAsState().value,
                localIp = viewModel.coopLocalIp.collectAsState().value,
                discoveredRooms = viewModel.discoveredRooms.collectAsState().value,
                remotePlayers = remotePlayers,
                onHostRoom = { viewModel.hostWifiCoop() },
                onJoinIp = { viewModel.joinWifiCoop(it) },
                onLeaveCoop = { viewModel.leaveWifiCoop() },
                onDismiss = { viewModel.closeModal() }
            )
            GameModal.WORLD_MAP -> WorldMapModal(
                playerX = world.playerX,
                playerY = world.playerY,
                remotePlayers = remotePlayers,
                onDismiss = { viewModel.closeModal() }
            )
            else -> {}
        }
    }
}

@Composable
private fun HudSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = ObsidianSurface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = ParchmentText, maxLines = 1)
        }
    }
}

fun computeClampedCameraOffset(
    targetTileX: Float,
    targetTileY: Float,
    facingRight: Boolean,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    tilePx: Float = 68f,
    lookAheadTiles: Float = 0.55f,
    mapWidthTiles: Int = OpenWorldMap.WIDTH,
    mapHeightTiles: Int = OpenWorldMap.HEIGHT
): Offset {
    val leadX = if (facingRight) lookAheadTiles else -lookAheadTiles
    val worldW = mapWidthTiles * tilePx
    val worldH = mapHeightTiles * tilePx

    val desiredX = (targetTileX + leadX) * tilePx - viewportWidthPx / 2f
    val desiredY = targetTileY * tilePx - viewportHeightPx / 2f

    val clampedX = if (worldW <= viewportWidthPx) {
        (worldW - viewportWidthPx) / 2f
    } else {
        desiredX.coerceIn(0f, worldW - viewportWidthPx)
    }
    val clampedY = if (worldH <= viewportHeightPx) {
        (worldH - viewportHeightPx) / 2f
    } else {
        desiredY.coerceIn(0f, worldH - viewportHeightPx)
    }
    return Offset(clampedX, clampedY)
}

fun stepSmoothCamera(
    current: Offset,
    target: Offset,
    dtSec: Float,
    smoothSpeed: Float = 9.5f,
    snapDistancePx: Float = 68f * 8f
): Offset {
    val dist = hypot(target.x - current.x, target.y - current.y)
    if (dist >= snapDistancePx || dist < 0.25f) {
        return target
    }
    val alpha = (1f - exp(-smoothSpeed * dtSec.coerceIn(0f, 0.1f))).coerceIn(0f, 1f)
    return Offset(
        x = current.x + (target.x - current.x) * alpha,
        y = current.y + (target.y - current.y) * alpha
    )
}

@Composable
private fun OpenWorldCanvas(
    profile: CharacterProfile,
    world: WorldRenderState,
    remotePlayers: Map<String, RemotePlayerState>,
    textMeasurer: TextMeasurer,
    onTapEnemy: (String?) -> Unit
) {
    val tilePx = 68f
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var cameraOffset by remember(profile.id) { mutableStateOf<Offset?>(null) }

    val latestWorld by rememberUpdatedState(world)
    val latestCanvasSize by rememberUpdatedState(canvasSize)
    val latestOnTapEnemy by rememberUpdatedState(onTapEnemy)

    LaunchedEffect(profile.id) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNanos = now
            val sz = latestCanvasSize
            if (sz.width > 0f && sz.height > 0f) {
                val w = latestWorld
                val target = computeClampedCameraOffset(
                    targetTileX = w.playerX,
                    targetTileY = w.playerY,
                    facingRight = w.facingRight,
                    viewportWidthPx = sz.width,
                    viewportHeightPx = sz.height,
                    tilePx = tilePx
                )
                val cur = cameraOffset
                cameraOffset = if (cur == null) target else stepSmoothCamera(cur, target, dt)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                canvasSize = Size(sz.width.toFloat(), sz.height.toFloat())
            }
            .pointerInput(profile.id) {
                detectTapGestures { tap ->
                    val w = latestWorld
                    val cam = cameraOffset ?: computeClampedCameraOffset(
                        targetTileX = w.playerX,
                        targetTileY = w.playerY,
                        facingRight = w.facingRight,
                        viewportWidthPx = canvasSize.width,
                        viewportHeightPx = canvasSize.height,
                        tilePx = tilePx
                    )
                    val worldTapX = (tap.x + cam.x) / tilePx
                    val worldTapY = (tap.y + cam.y) / tilePx
                    val clickedEnemy = w.enemies
                        .filter { it.isAlive }
                        .minByOrNull { hypot(it.x - worldTapX, it.y - worldTapY) }
                        ?.takeIf { hypot(it.x - worldTapX, it.y - worldTapY) < 1.2f }
                    latestOnTapEnemy(clickedEnemy?.id)
                }
            }
    ) {
        val cam = cameraOffset ?: computeClampedCameraOffset(
            targetTileX = world.playerX,
            targetTileY = world.playerY,
            facingRight = world.facingRight,
            viewportWidthPx = size.width,
            viewportHeightPx = size.height,
            tilePx = tilePx
        )
        val camX = cam.x
        val camY = cam.y

        val startTx = floor(camX / tilePx).toInt().coerceAtLeast(0)
        val endTx = ceil((camX + size.width) / tilePx).toInt().coerceAtMost(OpenWorldMap.WIDTH - 1)
        val startTy = floor(camY / tilePx).toInt().coerceAtLeast(0)
        val endTy = ceil((camY + size.height) / tilePx).toInt().coerceAtMost(OpenWorldMap.HEIGHT - 1)

        // 1. Render Crisp Pixel Tiles
        for (ty in startTy..endTy) {
            for (tx in startTx..endTx) {
                val tile = OpenWorldMap.getTile(tx, ty)
                val sx = (tx * tilePx - camX).roundToInt()
                val sy = (ty * tilePx - camY).roundToInt()
                val sz = tilePx.roundToInt() + 1
                drawImage(
                    image = PixelSprites.getTile(tile),
                    dstOffset = IntOffset(sx, sy),
                    dstSize = IntSize(sz, sz),
                    filterQuality = FilterQuality.None
                )
            }
        }

        // 2. Render Chests
        world.chests.forEach { ch ->
            val sx = (ch.x * tilePx - camX - tilePx * 0.42f).roundToInt()
            val sy = (ch.y * tilePx - camY - tilePx * 0.42f).roundToInt()
            val sz = (tilePx * 0.84f).roundToInt()
            drawImage(
                image = PixelSprites.getChest(ch.opened),
                dstOffset = IntOffset(sx, sy),
                dstSize = IntSize(sz, sz),
                filterQuality = FilterQuality.None
            )
        }

        // 3. Render Loot Drops
        world.lootDrops.forEach { drop ->
            val sx = drop.x * tilePx - camX
            val sy = drop.y * tilePx - camY
            val col = drop.item?.rarity?.color ?: MoltenGold
            drawCircle(color = col.copy(alpha = 0.35f), radius = tilePx * 0.32f, center = Offset(sx, sy))
            drawCircle(color = col, radius = tilePx * 0.18f, center = Offset(sx, sy))
        }

        // 3.5. Render Dodgeable Boss Attack Telegraphs on ground
        world.bossTelegraphs.forEach { tel ->
            val prog = tel.progress
            val dangerFill = CrimsonHp.copy(alpha = 0.18f + prog * 0.18f)
            val dangerStroke = WarmAmber.copy(alpha = 0.85f)
            val innerFill = CrimsonHp.copy(alpha = 0.34f)
            when (tel.tactic) {
                BossTactic.RADIAL_SLAM -> {
                    val cx = tel.originX * tilePx - camX
                    val cy = tel.originY * tilePx - camY
                    val r = tel.radiusTiles * tilePx
                    drawCircle(color = dangerFill, radius = r, center = Offset(cx, cy))
                    drawCircle(color = innerFill, radius = r * prog, center = Offset(cx, cy))
                    drawCircle(color = dangerStroke, radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
                }
                BossTactic.METEOR_ZONE -> {
                    val cx = tel.targetX * tilePx - camX
                    val cy = tel.targetY * tilePx - camY
                    val r = tel.radiusTiles * tilePx
                    drawCircle(color = dangerFill, radius = r, center = Offset(cx, cy))
                    drawCircle(color = innerFill, radius = r * prog, center = Offset(cx, cy))
                    drawCircle(color = dangerStroke, radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
                }
                BossTactic.LINEAR_CHARGE -> {
                    val ox = tel.originX * tilePx - camX
                    val oy = tel.originY * tilePx - camY
                    val halfW = BossTactics.LINEAR_CHARGE_HALF_WIDTH * tilePx
                    val len = tel.radiusTiles * tilePx
                    val perpX = -tel.dirY
                    val perpY = tel.dirX
                    val lanePath = Path().apply {
                        moveTo(ox + perpX * halfW, oy + perpY * halfW)
                        lineTo(ox + tel.dirX * len + perpX * halfW, oy + tel.dirY * len + perpY * halfW)
                        lineTo(ox + tel.dirX * len - perpX * halfW, oy + tel.dirY * len - perpY * halfW)
                        lineTo(ox - perpX * halfW, oy - perpY * halfW)
                        close()
                    }
                    val fillLen = len * prog
                    val fillPath = Path().apply {
                        moveTo(ox + perpX * halfW, oy + perpY * halfW)
                        lineTo(ox + tel.dirX * fillLen + perpX * halfW, oy + tel.dirY * fillLen + perpY * halfW)
                        lineTo(ox + tel.dirX * fillLen - perpX * halfW, oy + tel.dirY * fillLen - perpY * halfW)
                        lineTo(ox - perpX * halfW, oy - perpY * halfW)
                        close()
                    }
                    drawPath(path = lanePath, color = dangerFill)
                    drawPath(path = fillPath, color = innerFill)
                    drawPath(path = lanePath, color = dangerStroke, style = Stroke(width = 3f))
                }
                BossTactic.SHOCKWAVE_CONE -> {
                    val ox = tel.originX * tilePx - camX
                    val oy = tel.originY * tilePx - camY
                    val r = tel.radiusTiles * tilePx
                    val centerAngleDeg = (atan2(tel.dirY.toDouble(), tel.dirX.toDouble()) * 180.0 / PI).toFloat()
                    val sweepDeg = 76f
                    val startAngle = centerAngleDeg - sweepDeg / 2f
                    drawArc(
                        color = dangerFill,
                        startAngle = startAngle,
                        sweepAngle = sweepDeg,
                        useCenter = true,
                        topLeft = Offset(ox - r, oy - r),
                        size = Size(r * 2f, r * 2f)
                    )
                    val innerR = r * prog
                    drawArc(
                        color = innerFill,
                        startAngle = startAngle,
                        sweepAngle = sweepDeg,
                        useCenter = true,
                        topLeft = Offset(ox - innerR, oy - innerR),
                        size = Size(innerR * 2f, innerR * 2f)
                    )
                    drawArc(
                        color = dangerStroke,
                        startAngle = startAngle,
                        sweepAngle = sweepDeg,
                        useCenter = true,
                        topLeft = Offset(ox - r, oy - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }

        // 4. Render NPCs with ! / ? badges
        LootAndQuestData.npcs.forEach { npc ->
            val sx = npc.x * tilePx - camX
            val sy = npc.y * tilePx - camY
            val sz = (tilePx * 0.92f).roundToInt()
            drawImage(
                image = PixelSprites.getNpc(npc.tunicColorHex),
                dstOffset = IntOffset((sx - sz / 2f).roundToInt(), (sy - sz / 2f).roundToInt()),
                dstSize = IntSize(sz, sz),
                filterQuality = FilterQuality.None
            )
            // Quest marker check
            val hasTurnIn = LootAndQuestData.quests.any { q ->
                q.turnInNpcId == npc.id && profile.quests.any { it.questId == q.id && it.completed && !it.turnedIn }
            }
            val hasAvail = LootAndQuestData.quests.any { q ->
                q.giverNpcId == npc.id && profile.quests.none { it.questId == q.id } &&
                    (q.prerequisiteQuestId == null || profile.quests.any { it.questId == q.prerequisiteQuestId && it.turnedIn })
            }
            if (hasTurnIn || hasAvail) {
                val badge = if (hasTurnIn) "?" else "!"
                drawText(
                    textMeasurer = textMeasurer,
                    text = badge,
                    topLeft = Offset(sx - 6f, sy - tilePx * 0.85f),
                    style = TextStyle(color = MoltenGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                )
            }
            drawText(
                textMeasurer = textMeasurer,
                text = npc.name,
                topLeft = Offset(sx - 38f, sy + tilePx * 0.45f),
                style = TextStyle(color = ParchmentText, fontSize = 10.sp)
            )
        }

        // 5. Render Summons / Beasts / Totems / Traps
        world.summons.forEach { sum ->
            val sx = sum.x * tilePx - camX
            val sy = sum.y * tilePx - camY
            val sz = (tilePx * 0.82f).roundToInt()
            drawImage(
                image = PixelSprites.getSummon(sum.kind),
                dstOffset = IntOffset((sx - sz / 2f).roundToInt(), (sy - sz / 2f).roundToInt()),
                dstSize = IntSize(sz, sz),
                filterQuality = FilterQuality.None
            )
            if (sum.forcedDuelRemainingSec > 0f) {
                drawCircle(
                    color = Color(0xFFEC4899),
                    radius = tilePx * 0.55f,
                    center = Offset(sx, sy),
                    style = Stroke(width = 3f)
                )
            }
        }

        // 6. Render Enemies
        world.enemies.filter { it.isAlive }.forEach { en ->
            val sx = en.x * tilePx - camX
            val sy = en.y * tilePx - camY
            val scale = if (en.kind.isBoss) 1.35f else 0.92f
            val sz = (tilePx * scale).roundToInt()
            if (en.id == world.selectedEnemyId) {
                drawCircle(
                    color = CrimsonHp,
                    radius = sz * 0.62f,
                    center = Offset(sx, sy),
                    style = Stroke(width = 3f)
                )
            }
            drawImage(
                image = PixelSprites.getEnemy(en.kind),
                dstOffset = IntOffset((sx - sz / 2f).roundToInt(), (sy - sz / 2f).roundToInt()),
                dstSize = IntSize(sz, sz),
                filterQuality = FilterQuality.None
            )
            // Enemy HP Bar
            val barW = sz * 0.9f
            val barLeft = sx - barW / 2f
            val barTop = sy - sz * 0.6f
            if (en.kind.isBoss) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "JEFE Nv.${en.effectiveLevel} · ${en.kind.displayName}",
                    topLeft = Offset(barLeft - 14f, barTop - 20f),
                    style = TextStyle(color = MoltenGold, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                )
            }
            drawRect(color = ObsidianBg, topLeft = Offset(barLeft, barTop), size = Size(barW, 7f))
            drawRect(
                color = if (en.kind.isBoss) MoltenGold else CrimsonHp,
                topLeft = Offset(barLeft, barTop),
                size = Size(barW * (en.hp.toFloat() / en.maxHp).coerceIn(0f, 1f), 7f)
            )
            if (en.aiState == EnemyAiState.FLEE || en.aiState == EnemyAiState.CHASE) {
                val badge = if (en.aiState == EnemyAiState.FLEE) "HUYE" else "!"
                val badgeColor = if (en.aiState == EnemyAiState.FLEE) Color(0xFF38BDF8) else WarmAmber
                val badgeOffsetY = if (en.kind.isBoss) barTop - 36f else barTop - 22f
                drawText(
                    textMeasurer = textMeasurer,
                    text = badge,
                    topLeft = Offset(sx - 10f, badgeOffsetY),
                    style = TextStyle(color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                )
            }
        }

        // 7. Render Remote Wi-Fi Co-op Allies
        remotePlayers.values.forEach { rp ->
            val sx = rp.x * tilePx - camX
            val sy = rp.y * tilePx - camY
            val sz = (tilePx * 0.95f).roundToInt()
            drawImage(
                image = PixelSprites.getHero(rp.race, rp.heroClass),
                dstOffset = IntOffset((sx - sz / 2f).roundToInt(), (sy - sz / 2f).roundToInt()),
                dstSize = IntSize(sz, sz),
                filterQuality = FilterQuality.None
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "${rp.name} (${rp.level})",
                topLeft = Offset(sx - 36f, sy - sz * 0.72f),
                style = TextStyle(color = EmeraldHeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }

        // 8. Render Local Player
        val px = world.playerX * tilePx - camX
        val py = world.playerY * tilePx - camY
        val pSz = (tilePx * 0.98f).roundToInt()
        if (world.buffs.shieldAmount > 0 || world.buffs.damageReductionPct > 0f) {
            drawCircle(
                color = SapphireMana.copy(alpha = 0.45f),
                radius = pSz * 0.65f,
                center = Offset(px, py),
                style = Stroke(width = 4f)
            )
        }
        drawImage(
            image = PixelSprites.getHero(profile.race, profile.heroClass),
            dstOffset = IntOffset((px - pSz / 2f).roundToInt(), (py - pSz / 2f).roundToInt()),
            dstSize = IntSize(pSz, pSz),
            alpha = if (world.buffs.stealthActive) 0.45f else 1f,
            filterQuality = FilterQuality.None
        )

        // 9. Spell Visual Rings
        world.spellVisuals.forEach { sv ->
            val sx = sv.x * tilePx - camX
            val sy = sv.y * tilePx - camY
            drawCircle(
                color = sv.color.copy(alpha = (sv.ttlSec * 1.6f).coerceIn(0f, 0.75f)),
                radius = sv.radiusTiles * tilePx * (1.1f - sv.ttlSec * 0.5f),
                center = Offset(sx, sy),
                style = Stroke(width = 5f)
            )
        }

        // 10. Floating Combat Numbers (normal, crits, enemy damage, healing, status)
        world.floatingTexts.forEach { ft ->
            val progress = ft.lifeProgress
            val alpha = if (progress < 0.65f) 1f else ((1f - progress) / 0.35f).coerceIn(0f, 1f)
            val popScale = when (ft.kind) {
                FloatingTextKind.CRITICAL_HIT -> if (progress < 0.16f) {
                    1.48f - (progress / 0.16f) * 0.34f
                } else {
                    1.14f
                }
                FloatingTextKind.HEAL, FloatingTextKind.ENEMY_DAMAGE -> if (progress < 0.14f) {
                    1.24f - (progress / 0.14f) * 0.24f
                } else {
                    1.0f
                }
                FloatingTextKind.NORMAL_DAMAGE -> if (progress < 0.12f) {
                    1.16f - (progress / 0.12f) * 0.16f
                } else {
                    1.0f
                }
                FloatingTextKind.STATUS, FloatingTextKind.LOOT -> 1.0f
            }
            val fontSize = when (ft.kind) {
                FloatingTextKind.CRITICAL_HIT -> 16.sp
                FloatingTextKind.HEAL, FloatingTextKind.ENEMY_DAMAGE -> 14.sp
                FloatingTextKind.NORMAL_DAMAGE -> 13.sp
                FloatingTextKind.STATUS, FloatingTextKind.LOOT -> 12.sp
            }
            val style = TextStyle(
                color = ft.color.copy(alpha = alpha),
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold
            )
            val measured = textMeasurer.measure(ft.text, style = style)
            val centerX = ft.x * tilePx - camX
            val centerY = ft.y * tilePx - camY
            val topLeft = Offset(
                x = centerX - measured.size.width / 2f,
                y = centerY - measured.size.height / 2f
            )
            val shadowColor = Color(0xFF0A0710).copy(alpha = alpha * 0.92f)
            val shadowStyle = style.copy(color = shadowColor)

            withTransform({
                scale(scaleX = popScale, scaleY = popScale, pivot = Offset(centerX, centerY))
            }) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = ft.text,
                    topLeft = topLeft + Offset(2f, 2f),
                    style = shadowStyle
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = ft.text,
                    topLeft = topLeft + Offset(-1.5f, -1.5f),
                    style = shadowStyle
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = ft.text,
                    topLeft = topLeft,
                    style = style
                )
            }
        }
    }
}
