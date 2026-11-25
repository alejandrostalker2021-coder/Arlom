package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ComputedStats
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun PlayerInventoryModal(
    profile: CharacterProfile,
    stats: ComputedStats,
    onEquip: (EquipmentItem) -> Unit,
    onUnequipSlot: (EquipSlot) -> Unit,
    onSell: (EquipmentItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRarity by remember { mutableStateOf<ItemRarity?>(null) }
    var selectedSlotFilter by remember { mutableStateOf<EquipSlot?>(null) }

    val filteredItems = remember(profile.inventory, selectedRarity, selectedSlotFilter) {
        profile.inventory
            .filter { item ->
                (selectedRarity == null || item.rarity == selectedRarity) &&
                    (selectedSlotFilter == null || item.slot == selectedSlotFilter)
            }
            .sortedWith(compareByDescending<EquipmentItem> { it.rarity.ordinal }.thenByDescending { it.requiredLevel })
    }

    GameModalContainer(title = "GESTIÓN DE INVENTARIO", onDismiss = onDismiss) {
        // 1. Hero Stats & Rarity Drop Caps Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${profile.name} · Nv.${profile.level}/$MAX_PLAYER_LEVEL",
                        style = MaterialTheme.typography.titleSmall,
                        color = MoltenGold
                    )
                    Text(
                        "${profile.gold} Oro · ${profile.inventory.size}/36",
                        style = MaterialTheme.typography.labelMedium,
                        color = ParchmentText
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "ATQ ${stats.attack}  |  ARM ${stats.armor}  |  HP ${stats.maxHp}  |  CRÍT ${(stats.critChance * 100).toInt()}%  |  ROBO ${(stats.lifestealPct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParchmentText
                )
            }
        }

        // 2. Active Set Bonuses
        if (stats.activeSetCounts.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            stats.activeSetCounts.forEach { (setId, count) ->
                val def = LootAndQuestData.setBonuses[setId] ?: return@forEach
                Card(
                    colors = CardDefaults.cardColors(containerColor = RaritySetUnique.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, RaritySetUnique),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Set ${def.setName} ($count/4)", style = MaterialTheme.typography.labelMedium, color = RaritySetUnique)
                        Text(def.twoPieceDesc, style = MaterialTheme.typography.labelSmall, color = if (count >= 2) ParchmentText else MutedStone)
                        Text(def.fourPieceDesc, style = MaterialTheme.typography.labelSmall, color = if (count >= 4) ParchmentText else MutedStone)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 3. Equipment Slots (Weapons, Armor, Helm, Trinket) with tap-to-unequip
        Text("RANURAS DE EQUIPO (TOCA PARA DESEQUIPAR):", style = MaterialTheme.typography.labelMedium, color = MoltenGold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().testTag("equipment_slots_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EquipSlot.entries.forEach { slot ->
                val equippedItem = profile.equipped[slot]
                EquipmentSlotCard(
                    slot = slot,
                    item = equippedItem,
                    onUnequip = { onUnequipSlot(slot) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4. Rarity Level Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("rarity_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedRarity == null,
                onClick = { selectedRarity = null },
                label = { Text("Todas (${profile.inventory.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MoltenGold,
                    selectedLabelColor = ObsidianBg,
                    containerColor = ObsidianCard,
                    labelColor = ParchmentText
                )
            )
            ItemRarity.entries.forEach { rarity ->
                val count = profile.inventory.count { it.rarity == rarity }
                FilterChip(
                    selected = selectedRarity == rarity,
                    onClick = { selectedRarity = if (selectedRarity == rarity) null else rarity },
                    label = { Text("${rarity.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = rarity.color,
                        selectedLabelColor = ObsidianBg,
                        containerColor = ObsidianCard,
                        labelColor = rarity.color
                    ),
                    modifier = Modifier.testTag("filter_rarity_${rarity.name}")
                )
            }
        }

        // Slot Filter Row (Weapon / Armor / Helm / Trinket)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSlotFilter == null,
                onClick = { selectedSlotFilter = null },
                label = { Text("Todo Equipo") }
            )
            EquipSlot.entries.forEach { slot ->
                FilterChip(
                    selected = selectedSlotFilter == slot,
                    onClick = { selectedSlotFilter = if (selectedSlotFilter == slot) null else slot },
                    label = { Text(slot.label) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 5. Bag Items List with Stat Deltas vs Currently Equipped Slot
        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (profile.inventory.isEmpty()) {
                        "Tu mochila está vacía. Derrota enemigos o abre cofres."
                    } else {
                        "No hay objetos con el filtro seleccionado."
                    },
                    color = MutedStone
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().testTag("inventory_items_list")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val currentInSlot = profile.equipped[item.slot]
                    InventoryBagItemCard(
                        item = item,
                        currentInSlot = currentInSlot,
                        playerLevel = profile.level,
                        onEquip = { onEquip(item) },
                        onSell = { onSell(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentSlotCard(
    slot: EquipSlot,
    item: EquipmentItem?,
    onUnequip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = item?.rarity?.color ?: ObsidianBorder
    Surface(
        onClick = { if (item != null) onUnequip() },
        enabled = item != null,
        shape = RoundedCornerShape(10.dp),
        color = if (item != null) item.rarity.color.copy(alpha = 0.08f) else ObsidianCard,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .heightIn(min = 86.dp)
            .minimumInteractiveComponentSize()
            .testTag("equip_slot_${slot.name}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = slotIcon(slot),
                    contentDescription = slot.label,
                    tint = item?.rarity?.color ?: MutedStone,
                    modifier = Modifier.size(16.dp)
                )
                if (item != null) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Desequipar ${slot.label}",
                        tint = MutedStone,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = slot.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MutedStone
            )
            Text(
                text = item?.name ?: "Vacío",
                style = MaterialTheme.typography.labelSmall,
                color = item?.rarity?.color ?: MutedStone,
                fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item != null) {
                Text(
                    text = "${item.rarity.label} · Nv.${item.requiredLevel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = ParchmentText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InventoryBagItemCard(
    item: EquipmentItem,
    currentInSlot: EquipmentItem?,
    playerLevel: Int,
    onEquip: () -> Unit,
    onSell: () -> Unit
) {
    val canEquip = item.canEquipAtLevel(playerLevel)
    val atkDelta = item.attackBonus - (currentInSlot?.attackBonus ?: 0)
    val armDelta = item.armorBonus - (currentInSlot?.armorBonus ?: 0)
    val hpDelta = item.hpBonus - (currentInSlot?.hpBonus ?: 0)

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.2.dp, item.rarity.color.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(item.rarity.color.copy(alpha = 0.14f))
                    .border(1.dp, item.rarity.color, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slotIcon(item.slot),
                    contentDescription = item.slot.label,
                    tint = item.rarity.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "[${item.rarity.label}] ${item.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = item.rarity.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.slot.label} · Req. Nv.${item.requiredLevel} (Usable desde Nv.${item.effectiveMinLevel()})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canEquip) ParchmentText else CrimsonHp
                )
                Text(
                    buildString {
                        if (item.attackBonus > 0) append("+${item.attackBonus} ATQ ")
                        if (item.armorBonus > 0) append("+${item.armorBonus} ARM ")
                        if (item.hpBonus > 0) append("+${item.hpBonus} HP ")
                        if (item.critPctBonus > 0f) append("+${(item.critPctBonus * 100).toInt()}% Crít ")
                        if (item.lifestealPctBonus > 0f) append("+${(item.lifestealPctBonus * 100).toInt()}% Robo")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldHeal
                )
                val deltaText = buildList {
                    if (atkDelta != 0) add("ATQ ${if (atkDelta > 0) "+$atkDelta" else "$atkDelta"}")
                    if (armDelta != 0) add("ARM ${if (armDelta > 0) "+$armDelta" else "$armDelta"}")
                    if (hpDelta != 0) add("HP ${if (hpDelta > 0) "+$hpDelta" else "$hpDelta"}")
                }.joinToString(" · ")
                if (deltaText.isNotEmpty()) {
                    Text(
                        text = "Comparativa: $deltaText",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (atkDelta >= 0 && armDelta >= 0) MoltenGold else MutedStone
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onEquip,
                    enabled = canEquip,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 40.dp).testTag("equip_item_${item.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = item.rarity.color,
                        contentColor = ObsidianBg
                    )
                ) {
                    Text("Equipar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onSell,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Text("Vender +${item.goldValue}g", style = MaterialTheme.typography.labelSmall, color = MoltenGold)
                }
            }
        }
    }
}

private fun slotIcon(slot: EquipSlot): ImageVector = when (slot) {
    EquipSlot.WEAPON -> Icons.Default.Hardware
    EquipSlot.ARMOR -> Icons.Default.Shield
    EquipSlot.HELM -> Icons.Default.AutoAwesome
    EquipSlot.TRINKET -> Icons.Default.Diamond
}
