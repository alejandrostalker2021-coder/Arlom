package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SkillExecutionStatus
import com.example.engine.SkillSlotState
import com.example.engine.WorldRenderState
import com.example.model.CharacterProfile
import com.example.model.HeroClass
import com.example.model.LootAndQuestData
import com.example.model.MAX_PLAYER_LEVEL
import com.example.model.SkillDef
import com.example.pixel.PixelSprites
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Status HUD card featuring dedicated animated Health, Mana, and XP bars plus active buff badges.
 */
@Composable
fun HealthManaStatusBar(
    profile: CharacterProfile,
    world: WorldRenderState,
    modifier: Modifier = Modifier
) {
    val sprite = remember(profile.race, profile.heroClass) {
        PixelSprites.getHero(profile.race, profile.heroClass)
    }
    val hpFraction by animateFloatAsState(
        targetValue = (world.hp.toFloat() / world.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f),
        label = "hpAnim"
    )
    val manaFraction by animateFloatAsState(
        targetValue = (world.mana.toFloat() / world.maxMana.coerceAtLeast(1)).coerceIn(0f, 1f),
        label = "manaAnim"
    )
    val xpNeeded = LootAndQuestData.xpToNextLevel(profile.level).coerceAtLeast(1)
    val xpFraction by animateFloatAsState(
        targetValue = if (profile.level >= MAX_PLAYER_LEVEL) 1f else (profile.xp.toFloat() / xpNeeded).coerceIn(0f, 1f),
        label = "xpAnim"
    )

    Surface(
        color = ObsidianSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, MoltenGold.copy(alpha = 0.65f)),
        shadowElevation = 6.dp,
        modifier = modifier.testTag("health_mana_status_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pixel Portrait & Level Badge
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBg)
                        .border(1.5.dp, Color(profile.heroClass.accentHex), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sprite,
                        contentDescription = profile.name,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Surface(
                    color = MoltenGold,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.offset(y = 4.dp)
                ) {
                    Text(
                        text = "Nv.${profile.level}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ObsidianBg,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.width(164.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = ParchmentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${profile.gold}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MoltenGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Health Bar (Salud)
                StatusMeterRow(
                    icon = Icons.Default.Favorite,
                    label = "Salud ${world.hp}/${world.maxHp}",
                    fraction = hpFraction,
                    barBrush = Brush.horizontalGradient(listOf(Color(0xFF991B1B), CrimsonHp, Color(0xFFF87171))),
                    iconTint = CrimsonHp,
                    tag = "health_bar"
                )

                Spacer(Modifier.height(4.dp))

                // Mana Bar (Maná)
                StatusMeterRow(
                    icon = Icons.Default.Bolt,
                    label = "Maná ${world.mana}/${world.maxMana}",
                    fraction = manaFraction,
                    barBrush = Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), SapphireMana, Color(0xFF60A5FA))),
                    iconTint = Color(0xFF60A5FA),
                    tag = "mana_bar"
                )

                Spacer(Modifier.height(4.dp))

                // XP Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ObsidianBg)
                        .testTag("xp_bar")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xpFraction)
                            .background(MoltenGold)
                    )
                }
                Text(
                    text = if (profile.level >= MAX_PLAYER_LEVEL) "XP Máximo (Nv.30)" else "XP ${profile.xp}/$xpNeeded",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MutedStone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("xp_label")
                )

                // Active Buffs / Shield / Combo indicators
                val buffBadges = buildList {
                    if (world.buffs.shieldAmount > 0) add("Escudo ${world.buffs.shieldAmount}")
                    if (world.buffs.stealthActive) add("Sigilo")
                    if (world.buffs.attackBonusPct > 0f) add("+ATQ")
                    if (world.buffs.damageReductionPct > 0f) add("Defensa")
                    if (profile.heroClass == HeroClass.STALKER && world.buffs.comboPoints > 0) {
                        add("Combo x${world.buffs.comboPoints}")
                    }
                }
                if (buffBadges.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = buffBadges.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = EmeraldHeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMeterRow(
    icon: ImageVector,
    label: String,
    fraction: Float,
    barBrush: Brush,
    iconTint: Color,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianBg)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .align(Alignment.CenterStart)
                    .background(barBrush)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Tactile 4-way + diagonal Virtual D-Pad supporting press and continuous thumb sliding.
 */
@Composable
fun VirtualDPad(
    modifier: Modifier = Modifier,
    onDirectionChanged: (Float, Float) -> Unit
) {
    var activeDir by remember { mutableStateOf(0f to 0f) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    fun updateFromPointer(posX: Float, posY: Float) {
        if (padSize.width <= 0 || padSize.height <= 0) return
        val cx = padSize.width / 2f
        val cy = padSize.height / 2f
        val rx = (posX - cx) / (padSize.width / 2f)
        val ry = (posY - cy) / (padSize.height / 2f)

        val deadZone = 0.20f
        val dx = when {
            rx > deadZone -> 1f
            rx < -deadZone -> -1f
            else -> 0f
        }
        val dy = when {
            ry > deadZone -> 1f
            ry < -deadZone -> -1f
            else -> 0f
        }

        // Normalize diagonal so speed stays consistent
        val norm = if (dx != 0f && dy != 0f) 0.7071f else 1f
        val next = (dx * norm) to (dy * norm)
        if (next != activeDir) {
            activeDir = next
            onDirectionChanged(next.first, next.second)
        }
    }

    val upPressed = activeDir.second < -0.1f
    val downPressed = activeDir.second > 0.1f
    val leftPressed = activeDir.first < -0.1f
    val rightPressed = activeDir.first > 0.1f

    Box(
        modifier = modifier
            .size(162.dp)
            .onSizeChanged { padSize = it }
            .clip(CircleShape)
            .background(ObsidianSurface.copy(alpha = 0.36f))
            .border(1.5.dp, ObsidianBorder.copy(alpha = 0.7f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    updateFromPointer(down.position.x, down.position.y)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        updateFromPointer(change.position.x, change.position.y)
                        change.consume()
                    } while (true)
                    activeDir = 0f to 0f
                    onDirectionChanged(0f, 0f)
                }
            }
            .testTag("dpad_control"),
        contentAlignment = Alignment.Center
    ) {
        // Center Pivot Cap
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianCard.copy(alpha = 0.92f))
                .border(1.dp, MoltenGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        )

        // UP Arm
        DPadArm(
            icon = Icons.Default.KeyboardArrowUp,
            description = "Mover Arriba",
            pressed = upPressed,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .testTag("dpad_up")
        )
        // DOWN Arm
        DPadArm(
            icon = Icons.Default.KeyboardArrowDown,
            description = "Mover Abajo",
            pressed = downPressed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .testTag("dpad_down")
        )
        // LEFT Arm
        DPadArm(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = "Mover Izquierda",
            pressed = leftPressed,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .testTag("dpad_left")
        )
        // RIGHT Arm
        DPadArm(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = "Mover Derecha",
            pressed = rightPressed,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .testTag("dpad_right")
        )
    }
}

@Composable
private fun DPadArm(
    icon: ImageVector,
    description: String,
    pressed: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "dpadScale")
    Box(
        modifier = modifier
            .size(50.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) MoltenGold else ObsidianCard.copy(alpha = 0.88f))
            .border(
                width = 1.5.dp,
                color = if (pressed) ParchmentText else ObsidianBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (pressed) ObsidianBg else ParchmentText,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * Touch-optimized cluster of Quick-Action Skill Buttons, Racial Skill, Potion, and Primary Attack.
 */
@Composable
fun QuickActionSkillCluster(
    profile: CharacterProfile,
    world: WorldRenderState,
    classSkills: List<SkillDef>,
    skillStates: List<SkillSlotState>,
    racialState: SkillSlotState,
    onBasicAttack: () -> Unit,
    onUseSkill: (Int) -> Unit,
    onUseRacial: () -> Unit,
    onUsePotion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val basicCdFraction = if (world.basicAttackMaxCooldown > 0f && world.basicAttackCooldown > 0f) {
        (world.basicAttackCooldown / world.basicAttackMaxCooldown).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier.testTag("quick_action_skills"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Quick-Action Row: Racial + Potion + Skill 3 & 4
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickSkillButton(
                title = profile.race.activeSkillName.take(6),
                subLabel = "Racial",
                slotState = racialState,
                accent = MoltenGold,
                icon = Icons.Default.Star,
                tag = "racial_skill_button",
                onClick = onUseRacial
            )
            QuickSkillButton(
                title = "Poción",
                subLabel = "x${profile.potions}",
                slotState = SkillSlotState(
                    remainingSec = 0f,
                    totalSec = 1f,
                    status = if (profile.potions > 0 && world.hp < world.maxHp) {
                        SkillExecutionStatus.READY
                    } else {
                        SkillExecutionStatus.INSUFFICIENT_MANA
                    }
                ),
                accent = EmeraldHeal,
                icon = Icons.Default.LocalDrink,
                tag = "potion_button",
                onClick = onUsePotion
            )
            if (classSkills.size >= 4 && skillStates.size >= 4) {
                QuickSkillButton(
                    title = classSkills[2].name.take(7),
                    subLabel = "${classSkills[2].manaCost} PM",
                    slotState = skillStates[2],
                    accent = Color(classSkills[2].colorHex),
                    icon = Icons.Default.Shield,
                    tag = "skill_2_button",
                    onClick = { onUseSkill(2) }
                )
                QuickSkillButton(
                    title = classSkills[3].name.take(7),
                    subLabel = "${classSkills[3].manaCost} PM",
                    slotState = skillStates[3],
                    accent = Color(classSkills[3].colorHex),
                    icon = Icons.Default.Bolt,
                    tag = "skill_3_button",
                    onClick = { onUseSkill(3) }
                )
            }
        }

        // Bottom Quick-Action Row: Skill 1, Skill 2, and Large Primary Attack Button with Cooldown Arc
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (classSkills.size >= 2 && skillStates.size >= 2) {
                QuickSkillButton(
                    title = classSkills[0].name.take(7),
                    subLabel = "${classSkills[0].manaCost} PM",
                    slotState = skillStates[0],
                    accent = Color(classSkills[0].colorHex),
                    icon = Icons.Default.Bolt,
                    tag = "skill_0_button",
                    onClick = { onUseSkill(0) }
                )
                QuickSkillButton(
                    title = classSkills[1].name.take(7),
                    subLabel = "${classSkills[1].manaCost} PM",
                    slotState = skillStates[1],
                    accent = Color(classSkills[1].colorHex),
                    icon = Icons.Default.Star,
                    tag = "skill_1_button",
                    onClick = { onUseSkill(1) }
                )
            }

            // Large Primary Attack Button with radial swing cooldown overlay
            Surface(
                onClick = onBasicAttack,
                shape = CircleShape,
                color = Color(profile.heroClass.accentHex),
                border = BorderStroke(3.dp, ParchmentText),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(70.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("basic_attack_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (basicCdFraction > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.Black.copy(alpha = 0.58f),
                                startAngle = -90f,
                                sweepAngle = 360f * basicCdFraction,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = size
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ATQ",
                            style = MaterialTheme.typography.titleSmall,
                            color = ObsidianBg,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (profile.heroClass.isRanged) "DIST" else "C/C",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = ObsidianBg.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSkillButton(
    title: String,
    subLabel: String,
    slotState: SkillSlotState,
    accent: Color,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val isReady = slotState.status == SkillExecutionStatus.READY
    val onCd = slotState.status == SkillExecutionStatus.ON_COOLDOWN ||
        slotState.status == SkillExecutionStatus.ON_GLOBAL_COOLDOWN
    val noMana = slotState.status == SkillExecutionStatus.INSUFFICIENT_MANA

    val cdFraction = slotState.cooldownFraction
    val borderColor = when {
        slotState.isActiveEffect -> EmeraldHeal
        isReady -> accent
        onCd -> MoltenGold.copy(alpha = 0.6f)
        else -> ObsidianBorder
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isReady) ObsidianCard.copy(alpha = 0.94f) else ObsidianBg.copy(alpha = 0.85f),
        border = BorderStroke(if (slotState.isActiveEffect) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (isReady) 5.dp else 1.dp,
        modifier = Modifier
            .size(56.dp)
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            // Radial Clock-Sweep + Progress Arc Cooldown Overlay
            if (cdFraction > 0f) {
                Canvas(modifier = Modifier.fillMaxSize().testTag("${tag}_cooldown_overlay")) {
                    val pad = 2.dp.toPx()
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    // Translucent radial sweep sector
                    drawArc(
                        color = Color.Black.copy(alpha = 0.68f),
                        startAngle = -90f,
                        sweepAngle = 360f * cdFraction,
                        useCenter = true,
                        topLeft = Offset(-size.width * 0.25f, -size.height * 0.25f),
                        size = Size(size.width * 1.5f, size.height * 1.5f)
                    )
                    // Outer glowing cooldown ring
                    drawArc(
                        color = if (slotState.status == SkillExecutionStatus.ON_GLOBAL_COOLDOWN) {
                            SapphireMana
                        } else {
                            MoltenGold
                        },
                        startAngle = -90f,
                        sweepAngle = 360f * cdFraction,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            } else if (noMana) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SapphireMana.copy(alpha = 0.14f))
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {
                if (slotState.status == SkillExecutionStatus.ON_COOLDOWN && slotState.remainingSec > 0.05f) {
                    val text = if (slotState.remainingSec < 3f) {
                        String.format("%.1fs", slotState.remainingSec)
                    } else {
                        "${ceil(slotState.remainingSec).toInt()}s"
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        color = MoltenGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = title.take(5),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MutedStone,
                        maxLines = 1
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = when {
                            isReady -> accent
                            slotState.status == SkillExecutionStatus.ON_GLOBAL_COOLDOWN -> SapphireMana
                            else -> MutedStone
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isReady) ParchmentText else MutedStone,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = if (noMana && subLabel.endsWith("PM")) "Sin Maná" else subLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (noMana) Color(0xFF60A5FA) else if (isReady) MoltenGold else MutedStone,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
