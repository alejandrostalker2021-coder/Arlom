package com.example.engine

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CharacterRepository
import com.example.data.GameDatabase
import com.example.model.*
import com.example.net.CoopEnemySync
import com.example.net.CoopEnvelope
import com.example.net.CoopMode
import com.example.net.DiscoveredRoom
import com.example.net.LanCoopManager
import com.example.world.OpenWorldMap
import com.example.world.TileKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class AppScreen {
    TITLE_SELECT,
    CHARACTER_CREATOR,
    IN_GAME
}

enum class GameModal {
    NONE,
    INVENTORY,
    QUESTS,
    SKILLS,
    NPC_DIALOG,
    COOP_LOBBY,
    WORLD_MAP
}

data class ComputedStats(
    val maxHp: Int,
    val maxMana: Int,
    val attack: Int,
    val armor: Int,
    val moveSpeed: Float,
    val dodgeChance: Float,
    val critChance: Float,
    val lifestealPct: Float,
    val xpBonusPct: Float,
    val activeSetCounts: Map<String, Int>
)

data class ActiveBuffs(
    var shieldAmount: Int = 0,
    var shieldSec: Float = 0f,
    var damageReductionPct: Float = 0f,
    var dmgRedSec: Float = 0f,
    var attackBonusPct: Float = 0f,
    var atkBuffSec: Float = 0f,
    var speedBonusPct: Float = 0f,
    var speedBuffSec: Float = 0f,
    var dodgeBonusPct: Float = 0f,
    var dodgeBuffSec: Float = 0f,
    var lifestealBonusPct: Float = 0f,
    var lifestealBuffSec: Float = 0f,
    var stealthActive: Boolean = false,
    var stealthSec: Float = 0f,
    var comboPoints: Int = 0
)

enum class SkillExecutionStatus {
    READY,
    ON_COOLDOWN,
    ON_GLOBAL_COOLDOWN,
    INSUFFICIENT_MANA
}

data class SkillSlotState(
    val remainingSec: Float,
    val totalSec: Float,
    val status: SkillExecutionStatus,
    val isActiveEffect: Boolean = false
) {
    val cooldownFraction: Float
        get() = if (totalSec > 0f && remainingSec > 0f) (remainingSec / totalSec).coerceIn(0f, 1f) else 0f
}

data class WorldRenderState(
    val playerX: Float = 10f,
    val playerY: Float = 10f,
    val facingRight: Boolean = true,
    val hp: Int = 180,
    val maxHp: Int = 180,
    val mana: Int = 120,
    val maxMana: Int = 120,
    val currentZone: ZoneId = ZoneId.VALORIA_VILLAGE,
    val enemies: List<EnemyInstance> = emptyList(),
    val summons: List<SummonInstance> = emptyList(),
    val lootDrops: List<WorldLootDrop> = emptyList(),
    val chests: List<WorldChest> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val spellVisuals: List<SpellEffectVisual> = emptyList(),
    val bossTelegraphs: List<BossTelegraph> = emptyList(),
    val selectedEnemyId: String? = null,
    val nearbyNpcId: String? = null,
    val nearbyChestId: String? = null,
    val skillCooldowns: List<Float> = listOf(0f, 0f, 0f, 0f),
    val racialCooldown: Float = 0f,
    val basicAttackCooldown: Float = 0f,
    val basicAttackMaxCooldown: Float = 0.72f,
    val globalCooldownSec: Float = 0f,
    val globalCooldownTotalSec: Float = 0.45f,
    val buffs: ActiveBuffs = ActiveBuffs(),
    val combatLog: List<String> = emptyList(),
    val tickCounter: Long = 0L
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GameDatabase.getInstance(application)
    private val repo = CharacterRepository(db.characterDao(), db.inventoryDao())
    val coopManager = LanCoopManager(application, viewModelScope)

    val savedCharacters: StateFlow<List<CharacterProfile>> = repo.allCharacters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _screen = MutableStateFlow(AppScreen.TITLE_SELECT)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _activeModal = MutableStateFlow(GameModal.NONE)
    val activeModal: StateFlow<GameModal> = _activeModal.asStateFlow()

    private val _activeNpc = MutableStateFlow<NpcDef?>(null)
    val activeNpc: StateFlow<NpcDef?> = _activeNpc.asStateFlow()

    private val _profile = MutableStateFlow<CharacterProfile?>(null)
    val profile: StateFlow<CharacterProfile?> = _profile.asStateFlow()

    private val _world = MutableStateFlow(WorldRenderState())
    val world: StateFlow<WorldRenderState> = _world.asStateFlow()

    // Live Coop flows exposed
    val coopMode: StateFlow<CoopMode> = coopManager.mode
    val coopStatus: StateFlow<String> = coopManager.statusMessage
    val coopLocalIp: StateFlow<String> = coopManager.localIp
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = coopManager.discoveredRooms
    val remotePlayers: StateFlow<Map<String, RemotePlayerState>> = coopManager.remotePlayers

    private var joyX: Float = 0f
    private var joyY: Float = 0f
    private var floatingIdSeq = 1L
    private var visualIdSeq = 1L
    private var telegraphIdSeq = 1L
    private var autoSaveAccumSec = 0f
    private var lastAttackAnimSec = 0f

    init {
        setupCoopCallbacks()
        startGameLoop()
    }

    private fun setupCoopCallbacks() {
        coopManager.onRemoteEnemyDamage = { enemyId, dmg, attackerId ->
            val currentEnemies = _world.value.enemies.toMutableList()
            val idx = currentEnemies.indexOfFirst { it.id == enemyId && it.isAlive }
            if (idx >= 0) {
                val en = currentEnemies[idx]
                val nextHp = (en.hp - dmg).coerceAtLeast(0)
                val updated = en.copy(hp = nextHp, targetId = attackerId)
                currentEnemies[idx] = updated
                if (nextHp == 0) {
                    handleEnemyDefeated(updated, fromAlly = true)
                }
                _world.value = _world.value.copy(enemies = currentEnemies)
            }
        }
        coopManager.onRemotePartyHeal = { amount, healerName ->
            val w = _world.value
            val healedHp = (w.hp + amount).coerceAtMost(w.maxHp)
            val actual = healedHp - w.hp
            if (actual > 0) {
                addFloatingText(w.playerX, w.playerY - 0.6f, "+$actual ($healerName)", Color(0xFF10B981))
                _world.value = _world.value.copy(hp = healedHp)
            }
        }
        coopManager.onWorldEnemySnapshot = { synced ->
            if (coopManager.mode.value == CoopMode.CLIENT) {
                val map = synced.associateBy { it.id }
                val updated = _world.value.enemies.map { en ->
                    val s = map[en.id]
                    if (s != null) {
                        val wasAlive = en.isAlive
                        val nowAlive = s.hp > 0
                        if (wasAlive && !nowAlive) {
                            handleEnemyDefeated(en.copy(hp = 0), fromAlly = true)
                        }
                        en.copy(hp = s.hp, x = s.x, y = s.y)
                    } else en
                }
                _world.value = _world.value.copy(enemies = updated)
            }
        }
    }

    fun navigateTo(target: AppScreen) {
        if (target != AppScreen.IN_GAME) {
            saveCurrentProfile()
            coopManager.stopAll()
        }
        _activeModal.value = GameModal.NONE
        _screen.value = target
    }

    fun openModal(modal: GameModal) {
        if (modal == GameModal.COOP_LOBBY) {
            coopManager.refreshLocalIp()
            if (coopManager.mode.value == CoopMode.SOLO) {
                coopManager.startDiscoveryScan()
            }
        }
        _activeModal.value = modal
    }

    fun closeModal() {
        _activeModal.value = GameModal.NONE
        _activeNpc.value = null
    }

    fun createAndStartCharacter(name: String, race: Race, heroClass: HeroClass) {
        val trimmed = name.trim().ifEmpty { "${race.displayName} ${heroClass.displayName}" }.take(18)
        val starterWeapon = LootAndQuestData.generateEquipment(
            targetLevel = 1,
            rarity = ItemRarity.COMMON,
            heroClass = heroClass,
            forcedSlot = EquipSlot.WEAPON
        )
        val starterQuest = QuestProgress(questId = LootAndQuestData.quests.first().id)
        val newProfile = CharacterProfile(
            id = "hero_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}",
            name = trimmed,
            race = race,
            heroClass = heroClass,
            level = 1,
            xp = 0,
            gold = 60,
            potions = 5,
            posX = 11f,
            posY = 11f,
            equipped = mapOf(EquipSlot.WEAPON to starterWeapon),
            inventory = emptyList(),
            quests = listOf(starterQuest)
        )
        viewModelScope.launch {
            repo.saveProfile(newProfile)
            startPlaying(newProfile)
        }
    }

    fun deleteCharacter(id: String) {
        viewModelScope.launch {
            repo.deleteProfile(id)
        }
    }

    fun startPlaying(profile: CharacterProfile) {
        _profile.value = profile
        val stats = computeStats(profile, ActiveBuffs())
        _world.value = WorldRenderState(
            playerX = profile.posX,
            playerY = profile.posY,
            hp = stats.maxHp,
            maxHp = stats.maxHp,
            mana = stats.maxMana,
            maxMana = stats.maxMana,
            currentZone = OpenWorldMap.zoneAt(profile.posX, profile.posY),
            enemies = OpenWorldMap.initialEnemies().map { BossTactics.applyBossScaling(it, profile.level) },
            chests = OpenWorldMap.initialChests(),
            combatLog = listOf("Bienvenido a Aldea de Valoria. Habla con el Capitán Vance (!).")
        )
        _screen.value = AppScreen.IN_GAME
        _activeModal.value = GameModal.NONE
    }

    fun setJoystick(dx: Float, dy: Float) {
        val mag = hypot(dx, dy)
        if (mag > 1f) {
            joyX = dx / mag
            joyY = dy / mag
        } else {
            joyX = dx
            joyY = dy
        }
    }

    fun computeStats(p: CharacterProfile, buffs: ActiveBuffs): ComputedStats {
        val lvScale = 1f + (p.level - 1) * 0.16f
        val cls = p.heroClass
        val race = p.race

        var hp = (cls.baseHp * lvScale).toInt()
        var mana = (cls.baseMana * lvScale).toInt()
        var atk = (cls.baseAttack * lvScale).toInt()
        var armor = (cls.baseArmor * lvScale).toInt()
        var speed = cls.baseSpeed
        var dodge = cls.baseDodge + race.bonusDodgePct
        var crit = 0.08f + if (cls == HeroClass.STALKER || race == Race.ELF) 0.07f else 0f
        var lifesteal = if (cls == HeroClass.KNIGHT) 0.12f else 0f

        val setCounts = mutableMapOf<String, Int>()
        p.equipped.values.forEach { item ->
            hp += item.hpBonus
            mana += item.manaBonus
            atk += item.attackBonus
            armor += item.armorBonus
            crit += item.critPctBonus
            dodge += item.dodgePctBonus
            lifesteal += item.lifestealPctBonus
            item.setId?.let { sid -> setCounts[sid] = (setCounts[sid] ?: 0) + 1 }
        }

        // Apply Set Bonuses (2-piece and 4-piece)
        setCounts.forEach { (sid, count) ->
            val def = LootAndQuestData.setBonuses[sid] ?: return@forEach
            if (count >= 2) {
                atk += def.twoPieceAtk
                hp += def.twoPieceHp
                armor += def.twoPieceArmor
            }
            if (count >= 4) {
                atk += def.fourPieceAtk
                hp += def.fourPieceHp
                armor += def.fourPieceArmor
                crit += def.fourPieceCrit
                lifesteal += def.fourPieceLifesteal
            }
        }

        // Racial Passives
        hp = (hp * (1f + race.bonusHpPct)).toInt()
        armor = (armor * (1f + race.bonusArmorPct)).toInt()
        atk = (atk * (1f + race.bonusPhysDmgPct)).toInt()
        speed *= (1f + race.bonusSpeedPct)

        // Active Buffs
        atk = (atk * (1f + buffs.attackBonusPct)).toInt()
        speed *= (1f + buffs.speedBonusPct)
        dodge = (dodge + buffs.dodgeBonusPct).coerceAtMost(0.75f)
        lifesteal += buffs.lifestealBonusPct

        return ComputedStats(
            maxHp = hp,
            maxMana = mana,
            attack = atk,
            armor = armor,
            moveSpeed = speed,
            dodgeChance = dodge,
            critChance = crit.coerceAtMost(0.70f),
            lifestealPct = lifesteal,
            xpBonusPct = race.bonusXpPct,
            activeSetCounts = setCounts
        )
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            val dt = 0.05f // 20 Hz simulation tick
            while (isActive) {
                if (_screen.value == AppScreen.IN_GAME && _profile.value != null) {
                    stepSimulation(dt)
                }
                delay(50L)
            }
        }
    }

    private fun stepSimulation(dt: Float) {
        val p = _profile.value ?: return
        val w = _world.value
        val buffs = w.buffs.copy()

        // Tick buffs
        if (buffs.shieldSec > 0f) {
            buffs.shieldSec = (buffs.shieldSec - dt).coerceAtLeast(0f)
            if (buffs.shieldSec <= 0f) buffs.shieldAmount = 0
        }
        if (buffs.dmgRedSec > 0f) {
            buffs.dmgRedSec = (buffs.dmgRedSec - dt).coerceAtLeast(0f)
            if (buffs.dmgRedSec <= 0f) buffs.damageReductionPct = 0f
        }
        if (buffs.atkBuffSec > 0f) {
            buffs.atkBuffSec = (buffs.atkBuffSec - dt).coerceAtLeast(0f)
            if (buffs.atkBuffSec <= 0f) buffs.attackBonusPct = 0f
        }
        if (buffs.speedBuffSec > 0f) {
            buffs.speedBuffSec = (buffs.speedBuffSec - dt).coerceAtLeast(0f)
            if (buffs.speedBuffSec <= 0f) buffs.speedBonusPct = 0f
        }
        if (buffs.dodgeBuffSec > 0f) {
            buffs.dodgeBuffSec = (buffs.dodgeBuffSec - dt).coerceAtLeast(0f)
            if (buffs.dodgeBuffSec <= 0f) buffs.dodgeBonusPct = 0f
        }
        if (buffs.lifestealBuffSec > 0f) {
            buffs.lifestealBuffSec = (buffs.lifestealBuffSec - dt).coerceAtLeast(0f)
            if (buffs.lifestealBuffSec <= 0f) buffs.lifestealBonusPct = 0f
        }
        if (buffs.stealthSec > 0f) {
            buffs.stealthSec = (buffs.stealthSec - dt).coerceAtLeast(0f)
            if (buffs.stealthSec <= 0f) buffs.stealthActive = false
        }

        val stats = computeStats(p, buffs)

        // Move player smoothly
        var px = w.playerX
        var py = w.playerY
        var facingRight = w.facingRight
        if (hypot(joyX, joyY) > 0.08f) {
            val nx = px + joyX * stats.moveSpeed * dt
            val ny = py + joyY * stats.moveSpeed * dt
            if (OpenWorldMap.isWalkable(nx, py)) px = nx
            if (OpenWorldMap.isWalkable(px, ny)) py = ny
            if (joyX > 0.1f) facingRight = true else if (joyX < -0.1f) facingRight = false
        }

        val zone = OpenWorldMap.zoneAt(px, py)
        var hp = w.hp.coerceAtMost(stats.maxHp)
        var mana = w.mana.coerceAtMost(stats.maxMana)

        // Passive regen & Healing Fountain in Valoria
        val tile = OpenWorldMap.getTile(px.toInt(), py.toInt())
        val onFountain = tile == TileKind.HEAL_FOUNTAIN || zone.isSafeZone
        var fountainHealShown = 0
        if (w.tickCounter % 20L == 0L) {
            val hpRegen = if (onFountain) max(8, stats.maxHp / 12) else max(1, stats.maxHp / 60)
            val manaRegen = if (onFountain) max(10, stats.maxMana / 10) else max(2, stats.maxMana / 30)
            val prevHp = hp
            hp = (hp + hpRegen).coerceAtMost(stats.maxHp)
            if (onFountain) fountainHealShown = hp - prevHp
            mana = (mana + manaRegen).coerceAtMost(stats.maxMana)
        }

        // Cooldowns & Global Skill Execution Interval
        val nextSkillCds = w.skillCooldowns.map { (it - dt).coerceAtLeast(0f) }
        val nextRacialCd = (w.racialCooldown - dt).coerceAtLeast(0f)
        val nextBasicCd = (w.basicAttackCooldown - dt).coerceAtLeast(0f)
        val nextGcd = (w.globalCooldownSec - dt).coerceAtLeast(0f)
        if (lastAttackAnimSec > 0f) lastAttackAnimSec = (lastAttackAnimSec - dt).coerceAtLeast(0f)

        // Update floating texts with decelerating arc physics & spell visuals
        val nextFloats = w.floatingTexts.mapNotNull { ft ->
            val rem = ft.ttlSec - dt
            if (rem <= 0f) {
                null
            } else {
                val progress = ft.lifeProgress
                val damping = (1f - progress * 0.55f).coerceAtLeast(0.25f)
                ft.copy(
                    x = ft.x + ft.vx * dt,
                    y = ft.y + ft.vy * dt * damping,
                    ttlSec = rem
                )
            }
        }.toMutableList()

        if (fountainHealShown > 0) {
            nextFloats += createFloatingText(px, py - 0.6f, "+$fountainHealShown Salud", FloatingTextKind.HEAL)
        }

        val nextVisuals = w.spellVisuals.mapNotNull { sv ->
            val rem = sv.ttlSec - dt
            if (rem <= 0f) null else sv.copy(ttlSec = rem)
        }.toMutableList()

        // Update loot drops (auto-collect gold/potions within 1.15 tiles)
        val remainingDrops = mutableListOf<WorldLootDrop>()
        var lootGoldDelta = 0
        var lootPotionsDelta = 0
        val lootItemsAdded = mutableListOf<EquipmentItem>()
        for (drop in w.lootDrops) {
            val dist = hypot(drop.x - px, drop.y - py)
            if (dist < 1.15f) {
                if (drop.gold > 0) {
                    lootGoldDelta += drop.gold
                    nextFloats += createFloatingText(px, py - 0.5f, "+${drop.gold} Oro", FloatingTextKind.LOOT)
                }
                if (drop.potionCount > 0) {
                    lootPotionsDelta += drop.potionCount
                    nextFloats += createFloatingText(px, py - 0.7f, "+${drop.potionCount} Poción", FloatingTextKind.HEAL)
                }
                if (drop.item != null) {
                    if (p.inventory.size + lootItemsAdded.size < 36) {
                        lootItemsAdded += drop.item
                        nextFloats += createFloatingText(px, py - 0.9f, drop.item.name, FloatingTextKind.LOOT, drop.item.rarity.color)
                        appendLog("Botín: [${drop.item.rarity.label}] ${drop.item.name}")
                    } else {
                        remainingDrops += drop
                    }
                }
            } else {
                val rem = drop.ttlSec - dt
                if (rem > 0f) remainingDrops += drop.copy(ttlSec = rem)
            }
        }
        val updatedProfile = p.copy(
            posX = px,
            posY = py,
            gold = p.gold + lootGoldDelta,
            potions = p.potions + lootPotionsDelta,
            inventory = if (lootItemsAdded.isEmpty()) p.inventory else p.inventory + lootItemsAdded
        )

        // Update chests respawn
        val updatedChests = w.chests.map { ch ->
            if (ch.opened) {
                val r = ch.respawnSec - dt
                if (r <= 0f) ch.copy(opened = false, respawnSec = 0f) else ch.copy(respawnSec = r)
            } else ch
        }

        // Update summons (Beasts, Arcane Sentinels, Warlock Demons, Totems, Traps)
        val enemies = w.enemies.toMutableList()
        val updatedSummons = mutableListOf<SummonInstance>()
        for (sum in w.summons) {
            val rem = sum.remainingSec - dt
            if (rem <= 0f || sum.hp <= 0) continue
            val s = sum.copy(
                remainingSec = rem,
                attackCooldown = (sum.attackCooldown - dt).coerceAtLeast(0f),
                forcedDuelRemainingSec = (sum.forcedDuelRemainingSec - dt).coerceAtLeast(0f)
            )
            if (s.forcedDuelRemainingSec <= 0f) s.forcedDuelTargetId = null

            when (s.kind) {
                SummonKind.HEALING_TOTEM -> {
                    if (w.tickCounter % 20L == 0L && hypot(s.x - px, s.y - py) < 4.5f) {
                        val heal = max(12, stats.maxHp / 14)
                        val prevHp = hp
                        hp = (hp + heal).coerceAtMost(stats.maxHp)
                        val actualHeal = hp - prevHp
                        if (actualHeal > 0) {
                            nextFloats += createFloatingText(px, py - 0.55f, "+$actualHeal Salud", FloatingTextKind.HEAL)
                        }
                        coopManager.sendPartyHealEvent(toRemoteState(updatedProfile, px, py, hp, stats.maxHp, facingRight), heal)
                    }
                }
                SummonKind.STORM_TOTEM -> {
                    if (s.attackCooldown <= 0f) {
                        val targetIdx = enemies.indexOfFirst { it.isAlive && hypot(it.x - s.x, it.y - s.y) < 4.8f }
                        if (targetIdx >= 0) {
                            s.attackCooldown = 1.1f
                            val en = enemies[targetIdx]
                            val dmg = max(8, (stats.attack * 0.65f).toInt())
                            applyDamageToEnemy(enemies, targetIdx, dmg, false, nextFloats, updatedProfile.id)
                            nextVisuals += SpellEffectVisual(visualIdSeq++, en.x, en.y, 0.9f, Color(0xFF38BDF8), 0.3f)
                        }
                    }
                }
                SummonKind.SNARE_TRAP -> {
                    val targetIdx = enemies.indexOfFirst { it.isAlive && hypot(it.x - s.x, it.y - s.y) < 1.25f }
                    if (targetIdx >= 0) {
                        val en = enemies[targetIdx]
                        enemies[targetIdx] = en.copy(rootedSec = 3.5f)
                        nextFloats += FloatingText(floatingIdSeq++, en.x, en.y - 0.6f, "¡ATRAPADO!", Color(0xFF84CC16))
                        nextVisuals += SpellEffectVisual(visualIdSeq++, en.x, en.y, 1.1f, Color(0xFF84CC16), 0.4f)
                        continue // Trap consumed
                    }
                }
                SummonKind.TANK_BEAST, SummonKind.ARCANE_SENTINEL, SummonKind.WARLOCK_DEMON -> {
                    // Find target: forced duel target first, otherwise nearest enemy near player
                    val duelTargetIdx = s.forcedDuelTargetId?.let { tid ->
                        enemies.indexOfFirst { it.id == tid && it.isAlive }
                    } ?: -1
                    val targetIdx = if (duelTargetIdx >= 0) {
                        duelTargetIdx
                    } else {
                        enemies.withIndex()
                            .filter { it.value.isAlive && hypot(it.value.x - px, it.value.y - py) < 7.5f }
                            .minByOrNull { hypot(it.value.x - s.x, it.value.y - s.y) }?.index ?: -1
                    }

                    if (targetIdx >= 0) {
                        val en = enemies[targetIdx]
                        val dist = hypot(en.x - s.x, en.y - s.y)
                        val idealRange = if (s.kind == SummonKind.ARCANE_SENTINEL && duelTargetIdx < 0) 2.5f else 1.2f
                        if (dist > idealRange) {
                            val dirX = (en.x - s.x) / dist
                            val dirY = (en.y - s.y) / dist
                            s.x += dirX * 3.8f * dt
                            s.y += dirY * 3.8f * dt
                        } else if (s.attackCooldown <= 0f) {
                            s.attackCooldown = 1.0f
                            val isCrit = Random.nextFloat() < 0.15f
                            val dmg = max(6, s.attack + if (isCrit) s.attack / 2 else 0)
                            applyDamageToEnemy(enemies, targetIdx, dmg, isCrit, nextFloats, updatedProfile.id)
                            // High threat beast forces enemy to aggro the beast!
                            if (s.kind == SummonKind.TANK_BEAST && enemies[targetIdx].isAlive) {
                                enemies[targetIdx] = enemies[targetIdx].copy(targetId = s.id)
                            }
                        }
                    } else {
                        // Follow owner
                        val dOwner = hypot(px - s.x, py - s.y)
                        if (dOwner > 1.8f) {
                            s.x += ((px - s.x) / dOwner) * 3.8f * dt
                            s.y += ((py - s.y) / dOwner) * 3.8f * dt
                        }
                    }
                }
            }
            updatedSummons += s
        }

        // Update Boss Telegraphs & Enemies AI (Host authoritative, or local solo)
        val isAuthority = coopManager.mode.value != CoopMode.CLIENT
        val activeTelegraphs = w.bossTelegraphs
            .map { it.copy(remainingSec = it.remainingSec - dt) }
            .toMutableList()

        for (i in enemies.indices) {
            var en = enemies[i]
            if (!en.isAlive) {
                activeTelegraphs.removeAll { it.bossId == en.id }
                if (isAuthority) {
                    val r = en.respawnTimerSec - dt
                    if (r <= 0f) {
                        val respawnedBase = en.copy(
                            hp = en.maxHp,
                            x = en.spawnX,
                            y = en.spawnY,
                            respawnTimerSec = 0f,
                            stunnedSec = 0f,
                            rootedSec = 0f,
                            slowedSec = 0f,
                            poisonSec = 0f,
                            cursedAgonySec = 0f,
                            cursedWeakSec = 0f,
                            forcedDuelSummonId = null,
                            forcedDuelSec = 0f,
                            targetId = null,
                            aiState = EnemyAiState.IDLE,
                            recentlyDamagedSec = 0f,
                            telegraphCooldownSec = 2.6f,
                            activeTelegraphId = null
                        )
                        enemies[i] = BossTactics.applyBossScaling(respawnedBase, updatedProfile.level)
                    } else {
                        enemies[i] = en.copy(respawnTimerSec = r, activeTelegraphId = null)
                    }
                }
                continue
            }

            if (isAuthority && en.kind.isBoss && en.aiState == EnemyAiState.IDLE && en.hp == en.maxHp) {
                val targetScaled = BossTactics.scaleBossStats(en.kind, updatedProfile.level)
                if (en.effectiveLevel != targetScaled.effectiveLevel ||
                    en.maxHp != targetScaled.maxHp ||
                    en.scaledAttack != targetScaled.attack
                ) {
                    en = BossTactics.applyBossScaling(en, updatedProfile.level)
                }
            }

            // Tick status effects
            en = en.copy(
                attackCooldown = (en.attackCooldown - dt).coerceAtLeast(0f),
                stunnedSec = (en.stunnedSec - dt).coerceAtLeast(0f),
                rootedSec = (en.rootedSec - dt).coerceAtLeast(0f),
                slowedSec = (en.slowedSec - dt).coerceAtLeast(0f),
                poisonSec = (en.poisonSec - dt).coerceAtLeast(0f),
                cursedAgonySec = (en.cursedAgonySec - dt).coerceAtLeast(0f),
                cursedWeakSec = (en.cursedWeakSec - dt).coerceAtLeast(0f),
                forcedDuelSec = (en.forcedDuelSec - dt).coerceAtLeast(0f),
                recentlyDamagedSec = (en.recentlyDamagedSec - dt).coerceAtLeast(0f),
                telegraphCooldownSec = (en.telegraphCooldownSec - dt).coerceAtLeast(0f)
            )
            if (en.forcedDuelSec <= 0f) en = en.copy(forcedDuelSummonId = null)

            // DoT ticks once per second
            if (w.tickCounter % 20L == 0L && isAuthority) {
                var dotDmg = 0
                if (en.poisonSec > 0f) dotDmg += en.poisonDps
                if (en.cursedAgonySec > 0f) dotDmg += max(6, (stats.attack * 0.45f).toInt())
                if (dotDmg > 0) {
                    enemies[i] = en
                    applyDamageToEnemy(enemies, i, dotDmg, false, nextFloats, updatedProfile.id)
                    en = enemies[i]
                    if (!en.isAlive) {
                        activeTelegraphs.removeAll { it.bossId == en.id }
                        continue
                    }
                }
            }

            if (isAuthority && en.stunnedSec <= 0f) {
                // Determine aggro target: Forced 1v1 Duel Summon > High-threat Beast > Player
                val duelSum = en.forcedDuelSummonId?.let { sid -> updatedSummons.firstOrNull { it.id == sid } }
                val tankBeast = updatedSummons.firstOrNull {
                    it.kind == SummonKind.TANK_BEAST && hypot(it.x - en.x, it.y - en.y) < 6.5f
                }

                val targetX: Float
                val targetY: Float
                val targetingSummon: SummonInstance?

                when {
                    duelSum != null -> {
                        targetX = duelSum.x
                        targetY = duelSum.y
                        targetingSummon = duelSum
                    }
                    tankBeast != null && (en.targetId == tankBeast.id || hypot(tankBeast.x - en.x, tankBeast.y - en.y) < hypot(px - en.x, py - en.y) * 1.45f) -> {
                        targetX = tankBeast.x
                        targetY = tankBeast.y
                        targetingSummon = tankBeast
                        en = en.copy(targetId = tankBeast.id)
                    }
                    else -> {
                        targetX = px
                        targetY = py
                        targetingSummon = null
                    }
                }

                val distToTarget = hypot(targetX - en.x, targetY - en.y)
                val distFromSpawn = hypot(en.x - en.spawnX, en.y - en.spawnY)
                val prevAiState = en.aiState
                val nextAiState = EnemyStateMachine.evaluateNextState(
                    enemy = en,
                    distToTarget = distToTarget,
                    distFromSpawn = distFromSpawn,
                    playerInSafeZone = zone.isSafeZone,
                    playerStealthed = buffs.stealthActive,
                    isTargetingSummon = targetingSummon != null
                )
                en = en.copy(aiState = nextAiState)
                if (prevAiState != EnemyAiState.FLEE && nextAiState == EnemyAiState.FLEE) {
                    nextFloats += createFloatingText(en.x, en.y - 0.8f, "¡Huye!", FloatingTextKind.STATUS, Color(0xFFF59E0B))
                }

                // Boss Telegraphed Attack Windup (pauses movement/melee so player can clearly dodge)
                var isWindingUpTelegraph = activeTelegraphs.any { it.bossId == en.id }
                if (!isWindingUpTelegraph &&
                    en.kind.isBoss &&
                    en.kind.bossTactic != null &&
                    en.telegraphCooldownSec <= 0f &&
                    (nextAiState == EnemyAiState.CHASE || nextAiState == EnemyAiState.ATTACK) &&
                    distToTarget <= en.kind.bossTactic.rangeTiles + 0.55f &&
                    !zone.isSafeZone
                ) {
                    val created = BossTactics.createTelegraph(telegraphIdSeq++, en, targetX, targetY)
                    if (created != null) {
                        val bossScaled = BossTactics.scaleBossStats(en.kind, updatedProfile.level)
                        activeTelegraphs += created
                        isWindingUpTelegraph = true
                        en = en.copy(
                            telegraphCooldownSec = bossScaled.telegraphCooldownSec,
                            activeTelegraphId = created.id,
                            attackCooldown = max(en.attackCooldown, created.totalWindupSec + 0.4f)
                        )
                        nextFloats += createFloatingText(
                            en.x,
                            en.y - 1.05f,
                            created.tactic.warningCallout,
                            FloatingTextKind.STATUS,
                            Color(0xFFF59E0B)
                        )
                    }
                }

                if (!isWindingUpTelegraph) {
                    when (nextAiState) {
                        EnemyAiState.CHASE -> {
                            if (distToTarget > 1.25f && en.rootedSec <= 0f) {
                                val spd = en.kind.speed * if (en.slowedSec > 0f) 0.5f else 1f
                                val nx = en.x + ((targetX - en.x) / distToTarget) * spd * dt
                                val ny = en.y + ((targetY - en.y) / distToTarget) * spd * dt
                                if (OpenWorldMap.isWalkable(nx, ny)) {
                                    en = en.copy(x = nx, y = ny)
                                }
                            }
                        }
                    EnemyAiState.ATTACK -> {
                        if (en.attackCooldown <= 0f) {
                            val atkCooldown = if (en.kind.isBoss) {
                                BossTactics.scaleBossStats(en.kind, updatedProfile.level).meleeCooldownSec
                            } else {
                                1.35f
                            }
                            en = en.copy(attackCooldown = atkCooldown, recentlyDamagedSec = max(en.recentlyDamagedSec, 3.0f))
                            val enemyCrit = Random.nextFloat() < 0.10f
                            val weakMult = if (en.cursedWeakSec > 0f) 0.7f else 1f
                            val critMult = if (enemyCrit) 1.45f else 1f
                            val rawAtk = (en.scaledAttack * weakMult * critMult).toInt()

                            if (targetingSummon != null) {
                                val dmg = max(2, rawAtk - targetingSummon.armor / 3)
                                targetingSummon.hp = (targetingSummon.hp - dmg).coerceAtLeast(0)
                                nextFloats += if (enemyCrit) {
                                    createFloatingText(
                                        targetingSummon.x,
                                        targetingSummon.y - 0.55f,
                                        "¡CRÍT -$dmg!",
                                        FloatingTextKind.CRITICAL_HIT,
                                        Color(0xFFF59E0B)
                                    )
                                } else {
                                    createFloatingText(
                                        targetingSummon.x,
                                        targetingSummon.y - 0.5f,
                                        "-$dmg",
                                        FloatingTextKind.ENEMY_DAMAGE,
                                        Color(0xFFF97316)
                                    )
                                }
                            } else if (!zone.isSafeZone) {
                                // Attack local player
                                if (Random.nextFloat() < stats.dodgeChance) {
                                    nextFloats += createFloatingText(px, py - 0.7f, "¡ESQUIVA!", FloatingTextKind.STATUS)
                                } else {
                                    var dmg = max(2, rawAtk - stats.armor / 3)
                                    dmg = (dmg * (1f - buffs.damageReductionPct)).toInt().coerceAtLeast(1)
                                    var absorbed = 0
                                    if (buffs.shieldAmount > 0) {
                                        absorbed = min(buffs.shieldAmount, dmg)
                                        buffs.shieldAmount -= absorbed
                                        dmg -= absorbed
                                    }
                                    if (dmg > 0) {
                                        hp = (hp - dmg).coerceAtLeast(0)
                                        nextFloats += if (enemyCrit) {
                                            createFloatingText(
                                                px,
                                                py - 0.65f,
                                                "¡CRÍT -$dmg!",
                                                FloatingTextKind.CRITICAL_HIT,
                                                Color(0xFFF59E0B)
                                            )
                                        } else {
                                            createFloatingText(px, py - 0.6f, "-$dmg", FloatingTextKind.ENEMY_DAMAGE)
                                        }
                                    } else if (absorbed > 0) {
                                        nextFloats += createFloatingText(px, py - 0.6f, "0 (Escudo)", FloatingTextKind.STATUS)
                                    }
                                }
                            }
                        }
                    }
                    EnemyAiState.FLEE -> {
                        if (en.rootedSec <= 0f) {
                            val awayDist = max(0.05f, distToTarget)
                            val fleeDirX = (en.x - targetX) / awayDist
                            val fleeDirY = (en.y - targetY) / awayDist
                            val fleeSpeed = en.kind.speed * (if (en.slowedSec > 0f) 0.55f else 1.08f)
                            val nx = en.x + fleeDirX * fleeSpeed * dt
                            val ny = en.y + fleeDirY * fleeSpeed * dt
                            when {
                                OpenWorldMap.isWalkable(nx, ny) -> en = en.copy(x = nx, y = ny)
                                OpenWorldMap.isWalkable(nx, en.y) -> en = en.copy(x = nx)
                                OpenWorldMap.isWalkable(en.x, ny) -> en = en.copy(y = ny)
                            }
                        }
                    }
                    EnemyAiState.IDLE -> {
                        // Return gently toward spawn when out of combat
                        if (distFromSpawn > 0.6f && en.rootedSec <= 0f) {
                            val nx = en.x + ((en.spawnX - en.x) / distFromSpawn) * en.kind.speed * 0.6f * dt
                            val ny = en.y + ((en.spawnY - en.y) / distFromSpawn) * en.kind.speed * 0.6f * dt
                            if (OpenWorldMap.isWalkable(nx, ny)) {
                                en = en.copy(x = nx, y = ny)
                            }
                        }
                    }
                }
                }
            } else if (isAuthority && en.stunnedSec > 0f) {
                activeTelegraphs.removeAll { it.bossId == en.id }
                en = en.copy(aiState = EnemyAiState.IDLE, activeTelegraphId = null)
            }
            enemies[i] = en
        }

        // Resolve completed Boss Telegraphs
        val expiredTelegraphs = activeTelegraphs.filter { it.remainingSec <= 0f }
        if (expiredTelegraphs.isNotEmpty()) {
            activeTelegraphs.removeAll { it.remainingSec <= 0f }
            expiredTelegraphs.forEach { tel ->
                val bIdx = enemies.indexOfFirst { it.id == tel.bossId && it.isAlive }
                if (bIdx >= 0) {
                    var boss = enemies[bIdx].copy(activeTelegraphId = null)
                    if (isAuthority && tel.tactic == BossTactic.LINEAR_CHARGE && boss.rootedSec <= 0f) {
                        var dashX = boss.x
                        var dashY = boss.y
                        val steps = 6
                        val stepLen = (tel.radiusTiles * 0.7f) / steps
                        for (s in 1..steps) {
                            val candX = boss.x + tel.dirX * stepLen * s
                            val candY = boss.y + tel.dirY * stepLen * s
                            if (OpenWorldMap.isWalkable(candX, candY)) {
                                dashX = candX
                                dashY = candY
                            } else break
                        }
                        boss = boss.copy(x = dashX, y = dashY)
                    }
                    enemies[bIdx] = boss
                }

                val fxX = if (tel.tactic == BossTactic.METEOR_ZONE) tel.targetX else tel.originX
                val fxY = if (tel.tactic == BossTactic.METEOR_ZONE) tel.targetY else tel.originY
                nextVisuals += SpellEffectVisual(
                    id = visualIdSeq++,
                    x = fxX,
                    y = fxY,
                    radiusTiles = tel.radiusTiles,
                    color = Color(tel.colorHex),
                    ttlSec = 0.45f
                )

                if (isAuthority) {
                    if (!zone.isSafeZone && BossTactics.isPointHitByTelegraph(tel, px, py)) {
                        var dmg = max(4, tel.damage - stats.armor / 3)
                        dmg = (dmg * (1f - buffs.damageReductionPct)).toInt().coerceAtLeast(1)
                        var absorbed = 0
                        if (buffs.shieldAmount > 0) {
                            absorbed = min(buffs.shieldAmount, dmg)
                            buffs.shieldAmount -= absorbed
                            dmg -= absorbed
                        }
                        if (dmg > 0) {
                            hp = (hp - dmg).coerceAtLeast(0)
                            nextFloats += createFloatingText(
                                px,
                                py - 0.72f,
                                "¡IMPACTO -$dmg!",
                                FloatingTextKind.CRITICAL_HIT,
                                Color(0xFFEF4444)
                            )
                        } else if (absorbed > 0) {
                            nextFloats += createFloatingText(px, py - 0.65f, "0 (Escudo)", FloatingTextKind.STATUS)
                        }
                    } else if (!zone.isSafeZone && hypot(px - tel.originX, py - tel.originY) <= 7.5f) {
                        nextFloats += createFloatingText(
                            px,
                            py - 0.75f,
                            "¡ESQUIVADO!",
                            FloatingTextKind.HEAL,
                            Color(0xFF38BDF8)
                        )
                    }

                    updatedSummons.forEach { s ->
                        if ((s.kind == SummonKind.TANK_BEAST || s.kind == SummonKind.WARLOCK_DEMON || s.kind == SummonKind.ARCANE_SENTINEL) &&
                            BossTactics.isPointHitByTelegraph(tel, s.x, s.y)
                        ) {
                            val sDmg = max(3, tel.damage - s.armor / 3)
                            s.hp = (s.hp - sDmg).coerceAtLeast(0)
                            nextFloats += createFloatingText(s.x, s.y - 0.5f, "-$sDmg", FloatingTextKind.ENEMY_DAMAGE)
                        }
                    }
                }
            }
        }

        // Handle player defeat -> respawn safely at Valoria Healing Fountain (keeping all gear & XP!)
        if (hp <= 0) {
            px = 11f
            py = 14f
            hp = stats.maxHp
            mana = stats.maxMana
            nextFloats += FloatingText(floatingIdSeq++, px, py - 0.8f, "Revivido en Valoria", Color(0xFFF5B942))
            appendLog("Has caído en combate y regresado a la Fuente de Valoria.")
        }

        // Check nearby NPC or Chest
        val nearNpc = LootAndQuestData.npcs.firstOrNull { hypot(it.x - px, it.y - py) < 1.85f }?.id
        val nearChest = updatedChests.firstOrNull { !it.opened && hypot(it.x - px, it.y - py) < 1.6f }?.id

        // Merge latest profile state (preserving summon/DoT kill quest progress, XP, and level) + check REACH_ZONE quests
        val latestProfile = _profile.value ?: p
        var zoneQuestCompleted = false
        val zoneCheckedQuests = latestProfile.quests.map { qp ->
            if (qp.completed) qp
            else {
                val def = LootAndQuestData.quests.firstOrNull { it.id == qp.questId }
                if (def?.objectiveType == QuestObjectiveType.REACH_ZONE && def.targetZone == zone) {
                    zoneQuestCompleted = true
                    nextFloats += createFloatingText(px, py - 0.95f, "¡Zona explorada!", FloatingTextKind.LOOT)
                    appendLog("Objetivo de misión completado: ${def.title}")
                    qp.copy(currentCount = def.requiredCount, completed = true)
                } else qp
            }
        }
        val mergedInventory = if (lootItemsAdded.isEmpty()) {
            latestProfile.inventory
        } else {
            val spaceLeft = (36 - latestProfile.inventory.size).coerceAtLeast(0)
            latestProfile.inventory + lootItemsAdded.take(spaceLeft)
        }
        val mergedProfile = latestProfile.copy(
            posX = px,
            posY = py,
            gold = latestProfile.gold + lootGoldDelta,
            potions = latestProfile.potions + lootPotionsDelta,
            inventory = mergedInventory,
            quests = zoneCheckedQuests
        )

        // Update auto-save timer
        autoSaveAccumSec += dt
        if (autoSaveAccumSec >= 15f || zoneQuestCompleted) {
            if (autoSaveAccumSec >= 15f) autoSaveAccumSec = 0f
            viewModelScope.launch { repo.saveProfile(mergedProfile) }
        }
        _profile.value = mergedProfile

        _world.value = w.copy(
            playerX = px,
            playerY = py,
            facingRight = facingRight,
            hp = hp,
            maxHp = stats.maxHp,
            mana = mana,
            maxMana = stats.maxMana,
            currentZone = zone,
            enemies = enemies,
            summons = updatedSummons,
            lootDrops = remainingDrops,
            chests = updatedChests,
            floatingTexts = nextFloats.takeLast(36),
            spellVisuals = nextVisuals,
            bossTelegraphs = activeTelegraphs,
            nearbyNpcId = nearNpc,
            nearbyChestId = nearChest,
            skillCooldowns = nextSkillCds,
            racialCooldown = nextRacialCd,
            basicAttackCooldown = nextBasicCd,
            globalCooldownSec = nextGcd,
            buffs = buffs,
            tickCounter = w.tickCounter + 1
        )
    }

    fun evaluateSkillSlotState(index: Int, p: CharacterProfile, w: WorldRenderState): SkillSlotState {
        val skills = LootAndQuestData.skillsForClass(p.heroClass)
        val skill = skills.getOrNull(index)
            ?: return SkillSlotState(0f, 1f, SkillExecutionStatus.ON_COOLDOWN)
        val cd = w.skillCooldowns.getOrElse(index) { 0f }
        val gcd = w.globalCooldownSec

        val isActiveEffect = when (skill.id) {
            "pal_divine_shield", "el_elem_barrier" -> w.buffs.shieldSec > 0f
            "war_shield_wall", "kni_bone_armor" -> w.buffs.dmgRedSec > 0f
            "kni_blood_pact" -> w.buffs.lifestealBuffSec > 0f
            "hs_wild_instinct", "hs_evasive_leap", "st_smoke_bomb" -> w.buffs.dodgeBuffSec > 0f
            "st_stealth" -> w.buffs.stealthActive
            "hb_bestial_wrath", "sh_windfury" -> w.buffs.atkBuffSec > 0f
            "hb_call_beast" -> w.summons.any { it.kind == SummonKind.TANK_BEAST }
            "sm_call_sentinel" -> w.summons.any { it.kind == SummonKind.ARCANE_SENTINEL }
            "wl_demon_pact" -> w.summons.any { it.kind == SummonKind.WARLOCK_DEMON }
            "sh_storm_totem" -> w.summons.any { it.kind == SummonKind.STORM_TOTEM }
            "sh_heal_totem" -> w.summons.any { it.kind == SummonKind.HEALING_TOTEM }
            else -> false
        }

        return when {
            cd > 0.01f -> SkillSlotState(cd, skill.cooldownSec, SkillExecutionStatus.ON_COOLDOWN, isActiveEffect)
            gcd > 0.01f -> SkillSlotState(gcd, w.globalCooldownTotalSec, SkillExecutionStatus.ON_GLOBAL_COOLDOWN, isActiveEffect)
            w.mana < skill.manaCost -> SkillSlotState(0f, skill.cooldownSec, SkillExecutionStatus.INSUFFICIENT_MANA, isActiveEffect)
            else -> SkillSlotState(0f, skill.cooldownSec, SkillExecutionStatus.READY, isActiveEffect)
        }
    }

    fun evaluateRacialSlotState(p: CharacterProfile, w: WorldRenderState): SkillSlotState {
        val cd = w.racialCooldown
        val gcd = w.globalCooldownSec
        return when {
            cd > 0.01f -> SkillSlotState(cd, p.race.activeCooldownSec, SkillExecutionStatus.ON_COOLDOWN)
            gcd > 0.01f -> SkillSlotState(gcd, w.globalCooldownTotalSec, SkillExecutionStatus.ON_GLOBAL_COOLDOWN)
            else -> SkillSlotState(0f, p.race.activeCooldownSec, SkillExecutionStatus.READY)
        }
    }

    fun selectEnemy(enemyId: String?) {
        _world.value = _world.value.copy(selectedEnemyId = enemyId)
    }

    fun interactNearby() {
        val w = _world.value
        val npcId = w.nearbyNpcId
        if (npcId != null) {
            val npc = LootAndQuestData.npcs.firstOrNull { it.id == npcId } ?: return
            if (npc.role == NpcRole.HEALER_INNKEEPER) {
                _world.value = w.copy(hp = w.maxHp, mana = w.maxMana)
                addFloatingText(w.playerX, w.playerY - 0.7f, "¡Vida y Maná Restaurados!", FloatingTextKind.HEAL)
            }
            _activeNpc.value = npc
            _activeModal.value = GameModal.NPC_DIALOG
            return
        }

        val chestId = w.nearbyChestId
        if (chestId != null) {
            openChest(chestId)
        }
    }

    private fun openChest(chestId: String) {
        val p = _profile.value ?: return
        val w = _world.value
        val idx = w.chests.indexOfFirst { it.id == chestId && !it.opened }
        if (idx < 0) return
        val ch = w.chests[idx]
        val rarity = LootAndQuestData.rollLootRarity(minRarity = ch.minRarity, isBoss = true)
        val item = LootAndQuestData.generateEquipment(
            targetLevel = (p.level + 2).coerceAtMost(MAX_PLAYER_LEVEL),
            rarity = rarity,
            heroClass = p.heroClass
        )
        val gold = Random.nextInt(30, 85)
        val newChests = w.chests.toMutableList()
        newChests[idx] = ch.copy(opened = true, respawnSec = 45f)

        val drops = w.lootDrops + WorldLootDrop(
            id = "drop_ch_${System.nanoTime()}",
            x = ch.x,
            y = ch.y + 0.4f,
            item = item,
            gold = gold,
            potionCount = 1
        )
        _world.value = w.copy(chests = newChests, lootDrops = drops)
        addFloatingText(ch.x, ch.y - 0.5f, "¡Cofre Abierto!", rarity.color)
    }

    fun performBasicAttack() {
        val p = _profile.value ?: return
        val w = _world.value
        if (w.basicAttackCooldown > 0f) return
        val stats = computeStats(p, w.buffs)
        val targetIdx = findBestEnemyIndex(w, p.heroClass.attackRangeTiles + 0.5f)
        if (targetIdx < 0) {
            addFloatingText(w.playerX, w.playerY - 0.6f, "Sin objetivo cerca", Color(0xFFA398B8))
            return
        }

        lastAttackAnimSec = 0.25f
        val en = w.enemies[targetIdx]
        val buffs = w.buffs.copy()
        var dmgMult = 1f
        var forceCrit = false
        if (buffs.stealthActive) {
            dmgMult = 2.3f
            forceCrit = true
            buffs.stealthActive = false
            buffs.stealthSec = 0f
        }
        val isCrit = forceCrit || Random.nextFloat() < stats.critChance
        var dmg = (stats.attack * dmgMult * (if (isCrit) 1.65f else 1f)).toInt()
        dmg = max(4, dmg + Random.nextInt(-2, 4))

        val enemies = w.enemies.toMutableList()
        val floats = w.floatingTexts.toMutableList()
        applyDamageToEnemy(enemies, targetIdx, dmg, isCrit, floats, p.id)

        // Lifesteal
        var nextHp = w.hp
        if (stats.lifestealPct > 0f) {
            val heal = max(1, (dmg * stats.lifestealPct).toInt())
            val prevHp = nextHp
            nextHp = (nextHp + heal).coerceAtMost(w.maxHp)
            val actualHeal = nextHp - prevHp
            if (actualHeal > 0) {
                floats += createFloatingText(w.playerX, w.playerY - 0.65f, "+$actualHeal Salud", FloatingTextKind.HEAL)
            }
        }
        if (p.heroClass == HeroClass.STALKER) {
            buffs.comboPoints = (buffs.comboPoints + 1).coerceAtMost(5)
        }

        val swingInterval = if (p.heroClass == HeroClass.STALKER || p.heroClass == HeroClass.HUNTER_SURVIVAL) 0.55f else 0.72f
        val visuals = w.spellVisuals + SpellEffectVisual(visualIdSeq++, en.x, en.y, 0.75f, Color(p.heroClass.accentHex), 0.25f)
        _world.value = w.copy(
            enemies = enemies,
            floatingTexts = floats.takeLast(36),
            spellVisuals = visuals,
            hp = nextHp,
            buffs = buffs,
            selectedEnemyId = en.id,
            basicAttackCooldown = swingInterval,
            basicAttackMaxCooldown = swingInterval
        )
    }

    fun useSkill(index: Int) {
        val p = _profile.value ?: return
        val w = _world.value
        val skills = LootAndQuestData.skillsForClass(p.heroClass)
        val skill = skills.getOrNull(index) ?: return
        val slotState = evaluateSkillSlotState(index, p, w)
        when (slotState.status) {
            SkillExecutionStatus.ON_COOLDOWN -> {
                addFloatingText(w.playerX, w.playerY - 0.6f, "${skill.name}: ${String.format("%.1fs", slotState.remainingSec)}", Color(0xFFF5B942))
                return
            }
            SkillExecutionStatus.ON_GLOBAL_COOLDOWN -> return
            SkillExecutionStatus.INSUFFICIENT_MANA -> {
                addFloatingText(w.playerX, w.playerY - 0.6f, "Sin Maná (${skill.manaCost} PM)", Color(0xFF3B82F6))
                return
            }
            SkillExecutionStatus.READY -> {}
        }

        val stats = computeStats(p, w.buffs)
        val buffs = w.buffs.copy()
        val enemies = w.enemies.toMutableList()
        val summons = w.summons.toMutableList()
        val floats = w.floatingTexts.toMutableList()
        val visuals = w.spellVisuals.toMutableList()
        var nextHp = w.hp
        val nextMana = (w.mana - skill.manaCost).coerceAtLeast(0)
        lastAttackAnimSec = 0.35f

        val range = max(4.8f, p.heroClass.attackRangeTiles + 1.2f)
        val targetIdx = findBestEnemyIndex(w, range)

        when (skill.id) {
            // --- PALADIN ---
            "pal_holy_strike" -> {
                if (targetIdx < 0) return notifyNoTarget()
                val dmg = (stats.attack * 1.45f).toInt()
                applyDamageToEnemy(enemies, targetIdx, dmg, false, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.1f, Color(0xFFFACC15))
            }
            "pal_flash_light" -> {
                val heal = max(25, (stats.maxHp * 0.28f).toInt())
                nextHp = (nextHp + heal).coerceAtMost(w.maxHp)
                floats += createFloatingText(w.playerX, w.playerY - 0.7f, "+$heal Salud", FloatingTextKind.HEAL)
                visuals += SpellEffectVisual(visualIdSeq++, w.playerX, w.playerY, 2.2f, Color(0xFF10B981))
                coopManager.sendPartyHealEvent(toRemoteState(p, w.playerX, w.playerY, nextHp, w.maxHp, w.facingRight), heal)
            }
            "pal_divine_shield" -> {
                buffs.shieldAmount = (stats.maxHp * 0.35f).toInt()
                buffs.shieldSec = 5f
                buffs.damageReductionPct = 0.20f
                buffs.dmgRedSec = 5f
                floats += createFloatingText(w.playerX, w.playerY - 0.7f, "Escudo Divino", FloatingTextKind.STATUS)
            }
            "pal_consecration" -> {
                visuals += SpellEffectVisual(visualIdSeq++, w.playerX, w.playerY, 2.8f, Color(0xFFF59E0B), 0.6f)
                enemies.indices.filter { enemies[it].isAlive && hypot(enemies[it].x - w.playerX, enemies[it].y - w.playerY) < 3.0f }
                    .forEach { applyDamageToEnemy(enemies, it, (stats.attack * 1.15f).toInt(), false, floats, p.id) }
                val heal = max(12, (stats.maxHp * 0.12f).toInt())
                val prevHp = nextHp
                nextHp = (nextHp + heal).coerceAtMost(w.maxHp)
                val actualHeal = nextHp - prevHp
                if (actualHeal > 0) {
                    floats += createFloatingText(w.playerX, w.playerY - 0.65f, "+$actualHeal Salud", FloatingTextKind.HEAL)
                }
            }

            // --- WARRIOR ---
            "war_brutal_slam" -> {
                if (targetIdx < 0) return notifyNoTarget()
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.65f).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.0f, Color(0xFFEF4444))
            }
            "war_whirlwind" -> {
                visuals += SpellEffectVisual(visualIdSeq++, w.playerX, w.playerY, 2.4f, Color(0xFFF97316), 0.45f)
                enemies.indices.filter { enemies[it].isAlive && hypot(enemies[it].x - w.playerX, enemies[it].y - w.playerY) < 2.6f }
                    .forEach { applyDamageToEnemy(enemies, it, (stats.attack * 1.4f).toInt(), false, floats, p.id) }
            }
            "war_shield_wall" -> {
                buffs.damageReductionPct = 0.45f
                buffs.dmgRedSec = 6f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Muro -45% Daño", Color(0xFF94A3B8))
            }
            "war_taunt" -> {
                visuals += SpellEffectVisual(visualIdSeq++, w.playerX, w.playerY, 3.5f, Color(0xFFDC2626))
                enemies.indices.filter { enemies[it].isAlive && hypot(enemies[it].x - w.playerX, enemies[it].y - w.playerY) < 4.0f }
                    .forEach { enemies[it] = enemies[it].copy(targetId = p.id) }
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "¡PROVOCAR!", Color(0xFFEF4444))
            }

            // --- KNIGHT ---
            "kni_crimson_slash" -> {
                if (targetIdx < 0) return notifyNoTarget()
                val dmg = (stats.attack * 1.55f).toInt()
                applyDamageToEnemy(enemies, targetIdx, dmg, false, floats, p.id)
                val steal = max(8, (dmg * 0.40f).toInt())
                nextHp = (nextHp + steal).coerceAtMost(w.maxHp)
                floats += createFloatingText(w.playerX, w.playerY - 0.6f, "+$steal Salud", FloatingTextKind.HEAL)
            }
            "kni_blood_pact" -> {
                buffs.lifestealBonusPct = 0.18f
                buffs.lifestealBuffSec = 6f
                buffs.attackBonusPct = 0.15f
                buffs.atkBuffSec = 6f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Pacto de Sangre", Color(0xFFDC2626))
            }
            "kni_runic_strike" -> {
                if (targetIdx < 0) return notifyNoTarget()
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.85f).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.1f, Color(0xFFA855F7))
            }
            "kni_bone_armor" -> {
                // Resistance between Paladin (20%) and Warrior (45%)
                buffs.damageReductionPct = 0.30f
                buffs.dmgRedSec = 7f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Armadura Ósea", Color(0xFFCBD5E1))
            }

            // --- HUNTER SURVIVAL ---
            "hs_rapid_shot" -> {
                if (targetIdx < 0) return notifyNoTarget()
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.65f).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 0.9f, Color(0xFF22C55E))
            }
            "hs_wild_instinct" -> {
                buffs.speedBonusPct = 0.35f
                buffs.speedBuffSec = 5f
                buffs.dodgeBonusPct = 0.25f
                buffs.dodgeBuffSec = 5f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Instinto +Vel/Eva", Color(0xFF10B981))
            }
            "hs_spear_lunge" -> {
                if (targetIdx < 0) return notifyNoTarget()
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.5f).toInt(), false, floats, p.id)
            }
            "hs_evasive_leap" -> {
                buffs.dodgeBonusPct = 0.30f
                buffs.dodgeBuffSec = 3.5f
                if (targetIdx >= 0) applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.25f).toInt(), false, floats, p.id)
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Salto Evasivo", Color(0xFF38BDF8))
            }

            // --- HUNTER BEASTMASTER ---
            "hb_call_beast" -> {
                summons.removeAll { it.kind == SummonKind.TANK_BEAST }
                summons += SummonInstance(
                    id = "sum_beast_${System.nanoTime()}",
                    ownerId = p.id,
                    kind = SummonKind.TANK_BEAST,
                    x = w.playerX + 1.1f,
                    y = w.playerY,
                    hp = (stats.maxHp * 1.65f).toInt(),
                    maxHp = (stats.maxHp * 1.65f).toInt(),
                    attack = (stats.attack * 0.85f).toInt(),
                    armor = stats.armor + 35,
                    threatMultiplier = 3.2f,
                    remainingSec = 28f
                )
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.8f, "¡Bestia Tanque!", Color(0xFFD97706))
            }
            "hb_roar" -> {
                val beast = summons.firstOrNull { it.kind == SummonKind.TANK_BEAST }
                if (beast == null) {
                    addFloatingText(w.playerX, w.playerY - 0.6f, "Invoca tu Bestia primero", Color(0xFFF59E0B))
                    return
                }
                visuals += SpellEffectVisual(visualIdSeq++, beast.x, beast.y, 4.0f, Color(0xFFEF4444), 0.5f)
                enemies.indices.filter { enemies[it].isAlive && hypot(enemies[it].x - beast.x, enemies[it].y - beast.y) < 5.0f }
                    .forEach { enemies[it] = enemies[it].copy(targetId = beast.id) }
                floats += FloatingText(floatingIdSeq++, beast.x, beast.y - 0.6f, "¡RUGIDO TANQUE!", Color(0xFFEF4444))
            }
            "hb_bestial_wrath" -> {
                buffs.attackBonusPct = 0.30f
                buffs.atkBuffSec = 6f
                buffs.damageReductionPct = 0.15f
                buffs.dmgRedSec = 6f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Furia Bestial", Color(0xFFF59E0B))
            }
            "hb_kill_command" -> {
                if (targetIdx < 0) return notifyNoTarget()
                val bonus = if (summons.any { it.kind == SummonKind.TANK_BEAST }) 1.75f else 1.25f
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * bonus).toInt(), true, floats, p.id)
            }

            // --- HUNTER TRAPPER ---
            "ht_snare_trap" -> {
                summons += SummonInstance(
                    id = "sum_trap_${System.nanoTime()}",
                    ownerId = p.id,
                    kind = SummonKind.SNARE_TRAP,
                    x = w.playerX,
                    y = w.playerY,
                    hp = 50,
                    maxHp = 50,
                    attack = 0,
                    armor = 0,
                    threatMultiplier = 0f,
                    remainingSec = 18f
                )
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.6f, "Trampa Colocada", Color(0xFF84CC16))
            }
            "ht_slow_shot" -> {
                if (targetIdx < 0) return notifyNoTarget()
                enemies[targetIdx] = enemies[targetIdx].copy(slowedSec = 4.5f)
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.3f).toInt(), false, floats, p.id)
                floats += FloatingText(floatingIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y - 0.8f, "Ralentizado", Color(0xFF38BDF8))
            }
            "ht_venom_arrow" -> {
                if (targetIdx < 0) return notifyNoTarget()
                enemies[targetIdx] = enemies[targetIdx].copy(
                    poisonSec = 6.0f,
                    poisonDps = max(6, (stats.attack * 0.35f).toInt())
                )
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.1f).toInt(), false, floats, p.id)
                floats += FloatingText(floatingIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y - 0.8f, "Veneno -50% Cura", Color(0xFF10B981))
            }
            "ht_pierce_shot" -> {
                if (targetIdx < 0) return notifyNoTarget()
                val en = enemies[targetIdx]
                val mult = if (en.rootedSec > 0f || en.poisonSec > 0f) 2.0f else 1.35f
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * mult).toInt(), true, floats, p.id)
            }

            // --- MAGE WARLOCK ---
            "wl_curse_agony" -> {
                if (targetIdx < 0) return notifyNoTarget()
                enemies[targetIdx] = enemies[targetIdx].copy(cursedAgonySec = 6.5f)
                floats += FloatingText(floatingIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y - 0.7f, "Maldición Agonía", Color(0xFF9333EA))
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.2f, Color(0xFF9333EA))
            }
            "wl_curse_weak" -> {
                if (targetIdx < 0) return notifyNoTarget()
                enemies[targetIdx] = enemies[targetIdx].copy(cursedWeakSec = 6.5f)
                floats += FloatingText(floatingIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y - 0.7f, "Maldición Debilidad", Color(0xFF6B21A8))
            }
            "wl_demon_pact" -> {
                summons.removeAll { it.kind == SummonKind.WARLOCK_DEMON }
                summons += SummonInstance(
                    id = "sum_demon_${System.nanoTime()}",
                    ownerId = p.id,
                    kind = SummonKind.WARLOCK_DEMON,
                    x = w.playerX - 1f,
                    y = w.playerY,
                    hp = (stats.maxHp * 0.9f).toInt(),
                    maxHp = (stats.maxHp * 0.9f).toInt(),
                    attack = (stats.attack * 0.95f).toInt(),
                    armor = stats.armor,
                    threatMultiplier = 1f,
                    remainingSec = 24f
                )
                buffs.attackBonusPct = 0.25f
                buffs.atkBuffSec = 10f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.8f, "Demonio +25% Daño", Color(0xFFDC2626))
            }
            "wl_demon_bolt" -> {
                if (targetIdx < 0) return notifyNoTarget()
                val en = enemies[targetIdx]
                val curseBonus = if (en.cursedAgonySec > 0f || en.cursedWeakSec > 0f) 1.35f else 1f
                val demonBonus = if (summons.any { it.kind == SummonKind.WARLOCK_DEMON }) 1.25f else 1f
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.45f * curseBonus * demonBonus).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, en.x, en.y, 1.4f, Color(0xFFA855F7))
            }

            // --- MAGE ELEMENTAL ---
            "el_fireball" -> {
                if (targetIdx < 0) return notifyNoTarget()
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.75f).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.5f, Color(0xFFEF4444))
            }
            "el_frost_nova" -> {
                visuals += SpellEffectVisual(visualIdSeq++, w.playerX, w.playerY, 3.0f, Color(0xFF38BDF8), 0.55f)
                enemies.indices.filter { enemies[it].isAlive && hypot(enemies[it].x - w.playerX, enemies[it].y - w.playerY) < 3.2f }
                    .forEach {
                        enemies[it] = enemies[it].copy(slowedSec = 5.0f)
                        applyDamageToEnemy(enemies, it, (stats.attack * 1.0f).toInt(), false, floats, p.id)
                    }
            }
            "el_chain_lightning" -> {
                val hits = enemies.withIndex()
                    .filter { it.value.isAlive && hypot(it.value.x - w.playerX, it.value.y - w.playerY) < 5.8f }
                    .sortedBy { hypot(it.value.x - w.playerX, it.value.y - w.playerY) }
                    .take(3)
                if (hits.isEmpty()) return notifyNoTarget()
                hits.forEach { (idx, en) ->
                    applyDamageToEnemy(enemies, idx, (stats.attack * 1.35f).toInt(), false, floats, p.id)
                    visuals += SpellEffectVisual(visualIdSeq++, en.x, en.y, 1.0f, Color(0xFFFACC15))
                }
            }
            "el_elem_barrier" -> {
                buffs.shieldAmount = (stats.maxHp * 0.40f).toInt()
                buffs.shieldSec = 6f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Barrera Elemental", Color(0xFF818CF8))
            }

            // --- MAGE SUMMONER ---
            "sm_arcane_orb" -> {
                if (targetIdx < 0) return notifyNoTarget()
                enemies[targetIdx] = enemies[targetIdx].copy(slowedSec = 4.0f)
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.4f).toInt(), false, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.1f, Color(0xFF6366F1))
            }
            "sm_surface_blast" -> {
                val cx = if (targetIdx >= 0) enemies[targetIdx].x else w.playerX
                val cy = if (targetIdx >= 0) enemies[targetIdx].y else w.playerY
                visuals += SpellEffectVisual(visualIdSeq++, cx, cy, 2.5f, Color(0xFFF59E0B), 0.55f)
                enemies.indices.filter { enemies[it].isAlive && hypot(enemies[it].x - cx, enemies[it].y - cy) < 2.8f }
                    .forEach {
                        enemies[it] = enemies[it].copy(stunnedSec = 2.2f)
                        applyDamageToEnemy(enemies, it, (stats.attack * 1.25f).toInt(), false, floats, p.id)
                    }
                floats += createFloatingText(cx, cy - 0.7f, "¡ATURDIDO!", FloatingTextKind.STATUS, Color(0xFFFACC15))
            }
            "sm_call_sentinel" -> {
                summons.removeAll { it.kind == SummonKind.ARCANE_SENTINEL }
                summons += SummonInstance(
                    id = "sum_sent_${System.nanoTime()}",
                    ownerId = p.id,
                    kind = SummonKind.ARCANE_SENTINEL,
                    x = w.playerX + 1.0f,
                    y = w.playerY,
                    hp = (stats.maxHp * 1.25f).toInt(),
                    maxHp = (stats.maxHp * 1.25f).toInt(),
                    attack = (stats.attack * 1.05f).toInt(),
                    armor = stats.armor + 15,
                    threatMultiplier = 2.0f,
                    remainingSec = 26f
                )
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.8f, "¡Centinela Invocado!", Color(0xFF818CF8))
            }
            "sm_forced_duel" -> {
                val sent = summons.firstOrNull { it.kind == SummonKind.ARCANE_SENTINEL }
                if (sent == null) {
                    addFloatingText(w.playerX, w.playerY - 0.6f, "Invoca al Centinela primero", Color(0xFF818CF8))
                    return
                }
                if (targetIdx < 0) return notifyNoTarget()
                val en = enemies[targetIdx]
                sent.forcedDuelTargetId = en.id
                sent.forcedDuelRemainingSec = 3.0f
                enemies[targetIdx] = en.copy(forcedDuelSummonId = sent.id, forcedDuelSec = 3.0f)
                floats += FloatingText(floatingIdSeq++, en.x, en.y - 0.8f, "¡DUELO 1v1 (3s)!", Color(0xFFEC4899))
                visuals += SpellEffectVisual(visualIdSeq++, en.x, en.y, 1.5f, Color(0xFFEC4899), 0.6f)
            }

            // --- SHAMAN ---
            "sh_earth_shock" -> {
                if (targetIdx < 0) return notifyNoTarget()
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.5f).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.1f, Color(0xFF0EA5E9))
            }
            "sh_storm_totem" -> {
                summons.removeAll { it.kind == SummonKind.STORM_TOTEM }
                summons += SummonInstance(
                    id = "sum_storm_${System.nanoTime()}",
                    ownerId = p.id,
                    kind = SummonKind.STORM_TOTEM,
                    x = w.playerX + 0.8f,
                    y = w.playerY,
                    hp = 120,
                    maxHp = 120,
                    attack = stats.attack,
                    armor = 10,
                    threatMultiplier = 0.5f,
                    remainingSec = 10f
                )
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Tótem Tormenta", Color(0xFF38BDF8))
            }
            "sh_heal_totem" -> {
                summons.removeAll { it.kind == SummonKind.HEALING_TOTEM }
                summons += SummonInstance(
                    id = "sum_heal_${System.nanoTime()}",
                    ownerId = p.id,
                    kind = SummonKind.HEALING_TOTEM,
                    x = w.playerX - 0.8f,
                    y = w.playerY,
                    hp = 120,
                    maxHp = 120,
                    attack = 0,
                    armor = 10,
                    threatMultiplier = 0f,
                    remainingSec = 10f
                )
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Tótem Curativo", Color(0xFF10B981))
            }
            "sh_windfury" -> {
                buffs.attackBonusPct = 0.30f
                buffs.atkBuffSec = 6f
                buffs.speedBonusPct = 0.20f
                buffs.speedBuffSec = 6f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Furia del Viento", Color(0xFFFACC15))
            }

            // --- STALKER (ROGUE) ---
            "st_stealth" -> {
                buffs.stealthActive = true
                buffs.stealthSec = 7f
                buffs.speedBonusPct = 0.20f
                buffs.speedBuffSec = 7f
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Sigilo (2.3x Crít)", Color(0xFF94A3B8))
            }
            "st_sinister" -> {
                if (targetIdx < 0) return notifyNoTarget()
                buffs.comboPoints = (buffs.comboPoints + 1).coerceAtMost(5)
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * 1.45f).toInt(), true, floats, p.id)
            }
            "st_eviscerate" -> {
                if (targetIdx < 0) return notifyNoTarget()
                val cp = max(1, buffs.comboPoints)
                buffs.comboPoints = 0
                val mult = 1.2f + cp * 0.35f
                applyDamageToEnemy(enemies, targetIdx, (stats.attack * mult).toInt(), true, floats, p.id)
                visuals += SpellEffectVisual(visualIdSeq++, enemies[targetIdx].x, enemies[targetIdx].y, 1.3f, Color(0xFFDC2626))
            }
            "st_smoke_bomb" -> {
                buffs.dodgeBonusPct = 0.45f
                buffs.dodgeBuffSec = 4.5f
                buffs.speedBonusPct = 0.25f
                buffs.speedBuffSec = 4.5f
                visuals += SpellEffectVisual(visualIdSeq++, w.playerX, w.playerY, 2.0f, Color(0xFFA855F7))
                floats += FloatingText(floatingIdSeq++, w.playerX, w.playerY - 0.7f, "Bomba de Humo", Color(0xFFA855F7))
            }
        }

        val nextCds = w.skillCooldowns.toMutableList()
        nextCds[index] = skill.cooldownSec

        _world.value = w.copy(
            hp = nextHp,
            mana = nextMana,
            enemies = enemies,
            summons = summons,
            floatingTexts = floats.takeLast(36),
            spellVisuals = visuals,
            buffs = buffs,
            skillCooldowns = nextCds,
            globalCooldownSec = w.globalCooldownTotalSec
        )
    }

    fun useRacialSkill() {
        val p = _profile.value ?: return
        val w = _world.value
        val racialState = evaluateRacialSlotState(p, w)
        if (racialState.status != SkillExecutionStatus.READY) return
        val buffs = w.buffs.copy()
        var nextHp = w.hp
        var nextX = w.playerX
        var nextY = w.playerY

        when (p.race) {
            Race.HUMAN -> {
                buffs.attackBonusPct = max(buffs.attackBonusPct, 0.18f)
                buffs.atkBuffSec = 6f
                addFloatingText(nextX, nextY - 0.7f, "Voluntad Indomable", Color(0xFFF5B942))
            }
            Race.ELF -> {
                val dx = if (w.facingRight) 2.2f else -2.2f
                if (OpenWorldMap.isWalkable(nextX + dx, nextY)) nextX += dx
                buffs.dodgeBonusPct = max(buffs.dodgeBonusPct, 0.25f)
                buffs.dodgeBuffSec = 4f
                addFloatingText(nextX, nextY - 0.7f, "Paso Sombrío", Color(0xFF38BDF8))
            }
            Race.ORC -> {
                buffs.attackBonusPct = max(buffs.attackBonusPct, 0.25f)
                buffs.atkBuffSec = 6f
                buffs.speedBonusPct = max(buffs.speedBonusPct, 0.15f)
                buffs.speedBuffSec = 6f
                addFloatingText(nextX, nextY - 0.7f, "¡GRITO DE GUERRA!", Color(0xFFEF4444))
            }
            Race.DWARF -> {
                buffs.damageReductionPct = max(buffs.damageReductionPct, 0.35f)
                buffs.dmgRedSec = 5f
                val heal = max(15, (w.maxHp * 0.14f).toInt())
                nextHp = (nextHp + heal).coerceAtMost(w.maxHp)
                addFloatingText(nextX, nextY - 0.6f, "+$heal Salud", FloatingTextKind.HEAL)
                addFloatingText(nextX, nextY - 0.95f, "Forma Pétrea", FloatingTextKind.STATUS, Color(0xFF94A3B8))
            }
        }

        _world.value = w.copy(
            playerX = nextX,
            playerY = nextY,
            hp = nextHp,
            buffs = buffs,
            racialCooldown = p.race.activeCooldownSec
        )
    }

    fun usePotion() {
        val p = _profile.value ?: return
        val w = _world.value
        if (p.potions <= 0 || w.hp >= w.maxHp) return
        val heal = max(45, (w.maxHp * 0.42f).toInt())
        val manaRestore = max(25, (w.maxMana * 0.25f).toInt())
        val updated = p.copy(potions = p.potions - 1)
        _profile.value = updated
        _world.value = w.copy(
            hp = (w.hp + heal).coerceAtMost(w.maxHp),
            mana = (w.mana + manaRestore).coerceAtMost(w.maxMana)
        )
        addFloatingText(w.playerX, w.playerY - 0.6f, "+$heal Salud", FloatingTextKind.HEAL)
    }

    private fun notifyNoTarget() {
        val w = _world.value
        addFloatingText(w.playerX, w.playerY - 0.6f, "Acércate a un enemigo", Color(0xFFA398B8))
    }

    private fun findBestEnemyIndex(w: WorldRenderState, range: Float): Int {
        val selected = w.selectedEnemyId?.let { id ->
            w.enemies.indexOfFirst { it.id == id && it.isAlive && hypot(it.x - w.playerX, it.y - w.playerY) <= range }
        } ?: -1
        if (selected >= 0) return selected
        return w.enemies.withIndex()
            .filter { it.value.isAlive && hypot(it.value.x - w.playerX, it.value.y - w.playerY) <= range }
            .minByOrNull { hypot(it.value.x - w.playerX, it.value.y - w.playerY) }?.index ?: -1
    }

    private fun applyDamageToEnemy(
        enemies: MutableList<EnemyInstance>,
        idx: Int,
        damage: Int,
        isCrit: Boolean,
        floats: MutableList<FloatingText>,
        attackerId: String
    ) {
        val en = enemies[idx]
        if (!en.isAlive) return
        val nextHp = (en.hp - damage).coerceAtLeast(0)
        val updated = en.copy(hp = nextHp, targetId = attackerId, recentlyDamagedSec = 4.5f)
        enemies[idx] = updated
        floats += createFloatingText(
            x = en.x,
            y = en.y - 0.55f,
            text = if (isCrit) "¡CRÍT $damage!" else "$damage",
            kind = if (isCrit) FloatingTextKind.CRITICAL_HIT else FloatingTextKind.NORMAL_DAMAGE
        )
        if (coopManager.mode.value == CoopMode.CLIENT) {
            coopManager.sendDamageEvent(attackerId, en.id, damage)
        }
        if (nextHp == 0) {
            enemies[idx] = updated.copy(respawnTimerSec = if (en.kind.isBoss) 30f else 14f)
            handleEnemyDefeated(updated, fromAlly = false)
        }
    }

    private fun handleEnemyDefeated(en: EnemyInstance, fromAlly: Boolean) {
        val p = _profile.value ?: return
        val stats = computeStats(p, _world.value.buffs)
        val xpRaw = ExperienceSystem.calculateEnemyKillXp(en, xpBonusPct = stats.xpBonusPct)
        val xpResult = ExperienceSystem.applyXpGain(p.level, p.xp, xpRaw)
        val nextLevel = xpResult.newLevel
        val nextXp = xpResult.newXp

        if (en.kind.isBoss) {
            addFloatingText(
                _world.value.playerX,
                _world.value.playerY - 0.65f,
                "+$xpRaw XP (Jefe Nv.${en.effectiveLevel} x4)",
                Color(0xFFF5B942)
            )
            appendLog("Derrotaste a ${en.kind.displayName} (Nv.${en.effectiveLevel}): +$xpRaw XP (x4 Jefe).")
        } else {
            addFloatingText(
                _world.value.playerX,
                _world.value.playerY - 0.65f,
                "+$xpRaw XP (Nv.${en.effectiveLevel})",
                Color(0xFFA78BFA)
            )
            appendLog("Derrotaste a ${en.kind.displayName} (Nv.${en.effectiveLevel}): +$xpRaw XP.")
        }

        if (xpResult.levelsGained > 0) {
            addFloatingText(_world.value.playerX, _world.value.playerY - 1.15f, "¡NIVEL $nextLevel!", Color(0xFFF5B942))
            appendLog("¡Subiste al nivel $nextLevel!")
        }

        // Update quest progress
        val updatedQuests = p.quests.map { qp ->
            if (qp.completed) qp
            else {
                val def = LootAndQuestData.quests.firstOrNull { it.id == qp.questId }
                if (def?.objectiveType == QuestObjectiveType.KILL_ENEMY && def.targetEnemyKind == en.kind) {
                    val nc = (qp.currentCount + 1).coerceAtMost(def.requiredCount)
                    val isNowDone = nc >= def.requiredCount
                    val statusMsg = if (isNowDone) {
                        "¡Misión lista: ${def.title}!"
                    } else {
                        "${en.kind.displayName}: $nc/${def.requiredCount}"
                    }
                    addFloatingText(_world.value.playerX, _world.value.playerY - 1.0f, statusMsg, FloatingTextKind.LOOT)
                    appendLog(statusMsg)
                    qp.copy(currentCount = nc, completed = isNowDone)
                } else qp
            }
        }

        // Roll loot drop (respecting 40% Rare, 20% Epic, 8% Legendary/Unique Set caps!)
        val dropChance = if (en.kind.isBoss) 1.0f else 0.52f
        val newDrops = _world.value.lootDrops.toMutableList()
        if (!fromAlly || en.kind.isBoss) {
            val droppedItem = if (Random.nextFloat() <= dropChance) {
                val rarity = LootAndQuestData.rollLootRarity(isBoss = en.kind.isBoss)
                LootAndQuestData.generateEquipment(
                    targetLevel = en.effectiveLevel.coerceAtMost(MAX_PLAYER_LEVEL),
                    rarity = rarity,
                    heroClass = p.heroClass
                )
            } else null

            newDrops += WorldLootDrop(
                id = "drop_${System.nanoTime()}",
                x = en.x,
                y = en.y,
                item = droppedItem,
                gold = en.scaledGoldReward,
                potionCount = if (Random.nextFloat() < 0.28f) 1 else 0
            )
        }

        val updatedProfile = p.copy(
            level = nextLevel,
            xp = nextXp,
            quests = updatedQuests
        )
        _profile.value = updatedProfile
        val newStats = computeStats(updatedProfile, _world.value.buffs)
        _world.value = _world.value.copy(
            lootDrops = newDrops,
            maxHp = newStats.maxHp,
            maxMana = newStats.maxMana,
            hp = if (nextLevel > p.level) newStats.maxHp else _world.value.hp,
            mana = if (nextLevel > p.level) newStats.maxMana else _world.value.mana
        )
        saveProfileAsync(updatedProfile)
    }

    // --- Equipment & Inventory Actions ---
    fun equipItem(item: EquipmentItem) {
        val p = _profile.value ?: return
        if (!item.canEquipAtLevel(p.level)) {
            appendLog("Nivel insuficiente: requiere Nv.${item.effectiveMinLevel()} (Con maestría +${item.rarity.maxLevelBypass}).")
            return
        }
        val prevEquipped = p.equipped[item.slot]
        val nextInv = p.inventory.toMutableList().apply {
            remove(item)
            if (prevEquipped != null) add(prevEquipped)
        }
        val nextEq = p.equipped.toMutableMap().apply { put(item.slot, item) }
        val updated = p.copy(equipped = nextEq, inventory = nextInv)
        _profile.value = updated
        val stats = computeStats(updated, _world.value.buffs)
        _world.value = _world.value.copy(
            maxHp = stats.maxHp,
            maxMana = stats.maxMana,
            hp = _world.value.hp.coerceAtMost(stats.maxHp),
            mana = _world.value.mana.coerceAtMost(stats.maxMana)
        )
        viewModelScope.launch { repo.equipItem(p.id, item) }
    }

    fun unequipSlot(slot: EquipSlot) {
        val p = _profile.value ?: return
        val currentlyEquipped = p.equipped[slot] ?: return
        if (p.inventory.size >= 36) {
            appendLog("Mochila llena: no puedes desequipar ${currentlyEquipped.name}.")
            return
        }
        val nextEq = p.equipped.toMutableMap().apply { remove(slot) }
        val nextInv = p.inventory + currentlyEquipped
        val updated = p.copy(equipped = nextEq, inventory = nextInv)
        _profile.value = updated
        val stats = computeStats(updated, _world.value.buffs)
        _world.value = _world.value.copy(
            maxHp = stats.maxHp,
            maxMana = stats.maxMana,
            hp = _world.value.hp.coerceAtMost(stats.maxHp),
            mana = _world.value.mana.coerceAtMost(stats.maxMana)
        )
        viewModelScope.launch { repo.unequipSlot(p.id, slot) }
    }

    fun sellItem(item: EquipmentItem) {
        val p = _profile.value ?: return
        if (!p.inventory.contains(item)) return
        val updated = p.copy(
            gold = p.gold + item.goldValue,
            inventory = p.inventory - item
        )
        _profile.value = updated
        viewModelScope.launch {
            repo.deleteItem(item.id)
            repo.saveProfile(updated)
        }
    }

    fun buyPotion() {
        val p = _profile.value ?: return
        val cost = 20
        if (p.gold < cost) return
        val updated = p.copy(gold = p.gold - cost, potions = p.potions + 1)
        _profile.value = updated
        saveProfileAsync(updated)
    }

    fun buyBlacksmithChest() {
        val p = _profile.value ?: return
        val cost = 120
        if (p.gold < cost || p.inventory.size >= 36) return
        val rarity = LootAndQuestData.rollLootRarity(minRarity = ItemRarity.RARE)
        val gear = LootAndQuestData.generateEquipment(
            targetLevel = (p.level + 3).coerceAtMost(MAX_PLAYER_LEVEL),
            rarity = rarity,
            heroClass = p.heroClass
        )
        val updated = p.copy(gold = p.gold - cost, inventory = p.inventory + gear)
        _profile.value = updated
        appendLog("Forjado: [${gear.rarity.label}] ${gear.name}")
        saveProfileAsync(updated)
    }

    // --- Quest Actions ---
    fun acceptQuest(quest: QuestDef) {
        val p = _profile.value ?: return
        if (p.quests.any { it.questId == quest.id }) return
        val updated = p.copy(quests = p.quests + QuestProgress(questId = quest.id))
        _profile.value = updated
        appendLog("Misión aceptada: ${quest.title}")
        saveProfileAsync(updated)
    }

    fun turnInQuest(quest: QuestDef) {
        val p = _profile.value ?: return
        val qp = p.quests.firstOrNull { it.questId == quest.id && it.completed && !it.turnedIn } ?: return
        var nextLevel = p.level
        var nextXp = p.xp + quest.rewardXp
        while (nextLevel < MAX_PLAYER_LEVEL && nextXp >= LootAndQuestData.xpToNextLevel(nextLevel)) {
            nextXp -= LootAndQuestData.xpToNextLevel(nextLevel)
            nextLevel++
        }
        val nextInv = if (quest.rewardItem != null && p.inventory.size < 36) {
            p.inventory + quest.rewardItem
        } else p.inventory

        val nextQuests = p.quests.map {
            if (it.questId == quest.id) qp.copy(turnedIn = true) else it
        }

        val updated = p.copy(
            level = nextLevel,
            xp = if (nextLevel >= MAX_PLAYER_LEVEL) 0 else nextXp,
            gold = p.gold + quest.rewardGold,
            inventory = nextInv,
            quests = nextQuests
        )
        _profile.value = updated
        if (nextLevel > p.level) {
            val stats = computeStats(updated, _world.value.buffs)
            _world.value = _world.value.copy(
                hp = stats.maxHp,
                maxHp = stats.maxHp,
                mana = stats.maxMana,
                maxMana = stats.maxMana
            )
        }
        appendLog("¡Misión completada! +${quest.rewardXp} XP, +${quest.rewardGold} Oro")
        saveProfileAsync(updated)
    }

    fun executeDialogueOption(npc: NpcDef, option: DialogueOption): String? {
        val p = _profile.value ?: return null
        val w = _world.value
        val outcome = DialogueEngine.applyOption(p, npc.id, option)

        if (outcome.updatedProfile != p) {
            _profile.value = outcome.updatedProfile
            saveProfileAsync(outcome.updatedProfile)
        }

        if (outcome.restoreVitals || outcome.leveledUp) {
            val stats = computeStats(outcome.updatedProfile, w.buffs)
            _world.value = _world.value.copy(
                hp = stats.maxHp,
                maxHp = stats.maxHp,
                mana = stats.maxMana,
                maxMana = stats.maxMana
            )
        }

        outcome.logMessage?.let { appendLog(it) }
        outcome.floatingText?.let { text ->
            addFloatingText(w.playerX, w.playerY - 0.9f, text, outcome.floatingKind)
        }
        if (outcome.shouldClose) {
            closeModal()
        }
        return outcome.feedbackMessage
    }

    // --- Co-op Actions ---
    fun hostWifiCoop() {
        val p = _profile.value ?: return
        coopManager.startHost(p.name) {
            val w = _world.value
            val cur = _profile.value ?: p
            val localRemote = toRemoteState(cur, w.playerX, w.playerY, w.hp, w.maxHp, w.facingRight)
            val allPlayers = listOf(localRemote) + coopManager.remotePlayers.value.values
            val enemySync = w.enemies.map { CoopEnemySync(it.id, it.hp, it.x, it.y) }
            CoopEnvelope(
                type = "WORLD_SYNC",
                senderId = cur.id,
                players = allPlayers,
                enemies = enemySync
            )
        }
    }

    fun joinWifiCoop(hostIp: String) {
        coopManager.connectToHost(hostIp) {
            val w = _world.value
            val cur = _profile.value!!
            toRemoteState(cur, w.playerX, w.playerY, w.hp, w.maxHp, w.facingRight)
        }
    }

    fun leaveWifiCoop() {
        coopManager.stopAll()
    }

    private fun toRemoteState(
        p: CharacterProfile,
        x: Float,
        y: Float,
        hp: Int,
        maxHp: Int,
        facingRight: Boolean
    ): RemotePlayerState = RemotePlayerState(
        playerId = p.id,
        name = p.name,
        race = p.race,
        heroClass = p.heroClass,
        level = p.level,
        x = x,
        y = y,
        hp = hp,
        maxHp = maxHp,
        isAttacking = lastAttackAnimSec > 0f,
        facingRight = facingRight
    )

    private fun createFloatingText(
        x: Float,
        y: Float,
        text: String,
        kind: FloatingTextKind,
        colorOverride: Color? = null
    ): FloatingText {
        val jitterX = (Random.nextFloat() - 0.5f) * 0.44f
        val jitterY = (Random.nextFloat() - 0.5f) * 0.16f
        val vx = when (kind) {
            FloatingTextKind.CRITICAL_HIT -> (Random.nextFloat() - 0.5f) * 0.45f
            FloatingTextKind.NORMAL_DAMAGE, FloatingTextKind.ENEMY_DAMAGE -> (Random.nextFloat() - 0.5f) * 0.30f
            else -> 0f
        }
        val vy = when (kind) {
            FloatingTextKind.CRITICAL_HIT -> -1.05f
            FloatingTextKind.HEAL -> -0.88f
            FloatingTextKind.NORMAL_DAMAGE, FloatingTextKind.ENEMY_DAMAGE -> -0.82f
            FloatingTextKind.STATUS, FloatingTextKind.LOOT -> -0.62f
        }
        val duration = when (kind) {
            FloatingTextKind.CRITICAL_HIT -> 1.35f
            FloatingTextKind.HEAL -> 1.20f
            FloatingTextKind.NORMAL_DAMAGE, FloatingTextKind.ENEMY_DAMAGE -> 1.05f
            FloatingTextKind.STATUS, FloatingTextKind.LOOT -> 1.15f
        }
        return FloatingText(
            id = floatingIdSeq++,
            x = x + jitterX,
            y = y + jitterY,
            text = text,
            color = colorOverride ?: kind.defaultColor,
            kind = kind,
            vx = vx,
            vy = vy,
            maxTtlSec = duration,
            ttlSec = duration
        )
    }

    private fun addFloatingText(
        x: Float,
        y: Float,
        text: String,
        kind: FloatingTextKind,
        colorOverride: Color? = null
    ) {
        val w = _world.value
        _world.value = w.copy(
            floatingTexts = (w.floatingTexts + createFloatingText(x, y, text, kind, colorOverride)).takeLast(36)
        )
    }

    private fun addFloatingText(
        x: Float,
        y: Float,
        text: String,
        color: Color
    ) {
        addFloatingText(x, y, text, FloatingTextKind.STATUS, color)
    }

    private fun appendLog(msg: String) {
        val w = _world.value
        _world.value = w.copy(combatLog = (w.combatLog + msg).takeLast(4))
    }

    fun saveCurrentProfile() {
        val p = _profile.value ?: return
        saveProfileAsync(p)
    }

    private fun saveProfileAsync(p: CharacterProfile) {
        viewModelScope.launch { repo.saveProfile(p) }
    }

    override fun onCleared() {
        super.onCleared()
        coopManager.stopAll()
    }
}
