package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*

data class ActiveQuestEntry(
    val quest: QuestDef,
    val progress: QuestProgress,
    val currentCount: Int,
    val requiredCount: Int,
    val isReadyToTurnIn: Boolean,
    val objectiveSummary: String,
    val turnInNpcName: String
) {
    val progressFraction: Float
        get() = if (isReadyToTurnIn) 1f else (currentCount.toFloat() / requiredCount.coerceAtLeast(1)).coerceIn(0f, 1f)
}

fun buildActiveQuestEntries(profile: CharacterProfile): List<ActiveQuestEntry> {
    return profile.quests
        .filter { !it.turnedIn }
        .mapNotNull { qp ->
            val def = LootAndQuestData.quests.firstOrNull { it.id == qp.questId } ?: return@mapNotNull null
            val turnInNpc = LootAndQuestData.npcs.firstOrNull { it.id == def.turnInNpcId }?.name ?: "Aldea"
            val req = def.requiredCount.coerceAtLeast(1)
            val cur = if (qp.completed) req else qp.currentCount.coerceIn(0, req)
            val objective = if (qp.completed) {
                "Entregar a $turnInNpc"
            } else {
                when (def.objectiveType) {
                    QuestObjectiveType.TALK_NPC -> {
                        val targetName = LootAndQuestData.npcs.firstOrNull { it.id == def.targetNpcId }?.name ?: "PNJ"
                        "Hablar con $targetName"
                    }
                    QuestObjectiveType.KILL_ENEMY -> {
                        val enemyName = def.targetEnemyKind?.displayName ?: "Enemigos"
                        "$enemyName: $cur/$req"
                    }
                    QuestObjectiveType.REACH_ZONE -> {
                        val zoneName = def.targetZone?.displayName ?: "Zona"
                        "Explorar $zoneName"
                    }
                }
            }
            ActiveQuestEntry(
                quest = def,
                progress = qp,
                currentCount = cur,
                requiredCount = req,
                isReadyToTurnIn = qp.completed && !qp.turnedIn,
                objectiveSummary = objective,
                turnInNpcName = turnInNpc
            )
        }
        .sortedWith(compareByDescending<ActiveQuestEntry> { it.isReadyToTurnIn }.thenBy { it.quest.requiredLevel })
}

fun findNextAvailableQuestHint(profile: CharacterProfile): Pair<QuestDef, String>? {
    val nextQuest = LootAndQuestData.quests.firstOrNull { q ->
        profile.quests.none { it.questId == q.id } &&
            (q.prerequisiteQuestId == null || profile.quests.any { it.questId == q.prerequisiteQuestId && it.turnedIn })
    } ?: return null
    val giverName = LootAndQuestData.npcs.firstOrNull { it.id == nextQuest.giverNpcId }?.name ?: "Aldea"
    return nextQuest to giverName
}

@Composable
fun DynamicQuestTracker(
    profile: CharacterProfile,
    onOpenQuestLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val activeEntries = remember(profile.quests) { buildActiveQuestEntries(profile) }
    val nextHint = remember(profile.quests) {
        if (activeEntries.isEmpty()) findNextAvailableQuestHint(profile) else null
    }
    val hasReadyQuest = activeEntries.any { it.isReadyToTurnIn }
    val borderColor = if (hasReadyQuest) MoltenGold.copy(alpha = 0.85f) else SapphireMana.copy(alpha = 0.65f)

    Surface(
        color = ObsidianSurface.copy(alpha = 0.90f),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .widthIn(min = 204.dp, max = 252.dp)
            .testTag("dynamic_quest_tracker")
    ) {
        Column(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianCard.copy(alpha = 0.92f),
                        ObsidianSurface.copy(alpha = 0.88f)
                    )
                )
            )
        ) {
            // Header: Toggle expand/collapse + Open full journal button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable { expanded = !expanded }
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .testTag("quest_tracker_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (hasReadyQuest) Icons.Default.AssignmentTurnedIn else Icons.Default.AutoStories,
                        contentDescription = "Rastreador de misiones",
                        tint = if (hasReadyQuest) MoltenGold else SapphireMana,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (activeEntries.isNotEmpty()) {
                            "MISIONES (${activeEntries.size})"
                        } else {
                            "MISIONES"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasReadyQuest) MoltenGold else ParchmentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenQuestLog,
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("quest_tracker_open_log")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Abrir diario de misiones",
                            tint = MoltenGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer misiones" else "Expandir misiones",
                        tint = MutedStone,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))

                    if (activeEntries.isEmpty()) {
                        Surface(
                            color = ObsidianCard.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenQuestLog() }
                                .testTag("quest_tracker_empty_state")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (nextHint != null) {
                                    Text(
                                        text = "¡Nueva misión disponible!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MoltenGold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Habla con ${nextHint.second} (${nextHint.first.title})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ParchmentText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Todas las misiones completadas",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldHeal
                                    )
                                }
                            }
                        }
                    } else {
                        activeEntries.take(3).forEach { entry ->
                            QuestTrackerEntryRow(
                                entry = entry,
                                onClick = onOpenQuestLog
                            )
                        }
                        if (activeEntries.size > 3) {
                            Text(
                                text = "+${activeEntries.size - 3} más en el Diario",
                                style = MaterialTheme.typography.labelSmall,
                                color = MoltenGold,
                                modifier = Modifier
                                    .clickable { onOpenQuestLog() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestTrackerEntryRow(
    entry: ActiveQuestEntry,
    onClick: () -> Unit
) {
    val accent = if (entry.isReadyToTurnIn) MoltenGold else EmeraldHeal
    val animatedProgress by animateFloatAsState(
        targetValue = entry.progressFraction,
        animationSpec = spring(),
        label = "quest_progress_${entry.quest.id}"
    )

    Surface(
        color = ObsidianCard.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (entry.isReadyToTurnIn) MoltenGold.copy(alpha = 0.8f) else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("quest_tracker_item_${entry.quest.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (entry.isReadyToTurnIn) MoltenGold else SapphireMana),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (entry.isReadyToTurnIn) "?" else "!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianBg
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = entry.quest.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isReadyToTurnIn) MoltenGold else ParchmentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (entry.isReadyToTurnIn) "LISTA" else "${entry.currentCount}/${entry.requiredCount}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text = entry.objectiveSummary,
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.isReadyToTurnIn) ParchmentText else MutedStone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = accent,
                trackColor = ObsidianBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}
