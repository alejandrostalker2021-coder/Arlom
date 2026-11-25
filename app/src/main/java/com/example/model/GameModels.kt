package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.RarityCommon
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RarityLegendary
import com.example.ui.theme.RarityRare
import com.example.ui.theme.RaritySetUnique
import com.squareup.moshi.JsonClass

const val MAX_PLAYER_LEVEL = 30
const val HIGH_TIER_EQUIP_LEVEL_BONUS = 6

enum class Race(
    val displayName: String,
    val title: String,
    val passiveName: String,
    val passiveDesc: String,
    val activeSkillName: String,
    val activeSkillDesc: String,
    val activeCooldownSec: Float,
    val skinColorHex: Long,
    val hairColorHex: Long,
    val bonusHpPct: Float = 0f,
    val bonusArmorPct: Float = 0f,
    val bonusSpeedPct: Float = 0f,
    val bonusDodgePct: Float = 0f,
    val bonusPhysDmgPct: Float = 0f,
    val bonusXpPct: Float = 0f
) {
    HUMAN(
        displayName = "Humano",
        title = "Bastión de Valoria",
        passiveName = "Espíritu Versátil",
        passiveDesc = "+8% XP obtenida y +5% a todas las estadísticas base.",
        activeSkillName = "Voluntad Indomable",
        activeSkillDesc = "Purifica aturdimientos/ralentizaciones y otorga +18% daño por 6s.",
        activeCooldownSec = 20f,
        skinColorHex = 0xFFF1C27D,
        hairColorHex = 0xFF5C3A21,
        bonusHpPct = 0.05f,
        bonusArmorPct = 0.05f,
        bonusPhysDmgPct = 0.05f,
        bonusXpPct = 0.08f
    ),
    ELF(
        displayName = "Elfo",
        title = "Centinela del Bosque",
        passiveName = "Agilidad Élfica",
        passiveDesc = "+10% velocidad de movimiento y +8% evasión/crítico.",
        activeSkillName = "Paso Sombrío",
        activeSkillDesc = "Impulso rápido hacia adelante y +25% evasión durante 4s.",
        activeCooldownSec = 16f,
        skinColorHex = 0xFFF5D0B5,
        hairColorHex = 0xFFE6D690,
        bonusSpeedPct = 0.10f,
        bonusDodgePct = 0.08f
    ),
    ORC(
        displayName = "Orco",
        title = "Hijo de la Horda",
        passiveName = "Furia de Sangre",
        passiveDesc = "+10% daño físico y resistencia natural al aturdimiento.",
        activeSkillName = "Grito de Guerra",
        activeSkillDesc = "Desata furia: +25% daño y velocidad de ataque por 6s.",
        activeCooldownSec = 22f,
        skinColorHex = 0xFF4F8A4B,
        hairColorHex = 0xFF1F1D24,
        bonusHpPct = 0.04f,
        bonusPhysDmgPct = 0.10f
    ),
    DWARF(
        displayName = "Enano",
        title = "Forjador de Montaña",
        passiveName = "Piel de Piedra",
        passiveDesc = "+12% armadura y +10% vida máxima.",
        activeSkillName = "Forma Pétrea",
        activeSkillDesc = "Reduce 35% el daño recibido y regenera 14% HP en 5s.",
        activeCooldownSec = 24f,
        skinColorHex = 0xFFE5B495,
        hairColorHex = 0xFFB45309,
        bonusHpPct = 0.10f,
        bonusArmorPct = 0.12f
    )
}

enum class ArmorWeight(val label: String) {
    PLATE("Placas"),
    MAIL_LEATHER("Cuero"),
    CLOTH("Tela")
}

enum class WeaponCategory(val label: String) {
    HEAVY_MELEE("Espadas, Mandobles y Martillos"),
    BOW_SPEAR("Arcos, Ballestas y Lanzas"),
    STAFF_GRIMOIRE("Báculos, Cetros y Grimorios"),
    RUNIC_AXE_TOTEM("Hachas Rúnicas y Tótems"),
    TWIN_DAGGERS("Dagas y Hojas Ocultas")
}

enum class ClassGroup(val label: String) {
    PLATE_VANGUARD("Vanguardia de Placas"),
    HUNTER("Cazador"),
    MAGE("Mago y Artes Arcanas"),
    SHAMAN("Chamán"),
    STALKER("Sigilo")
}

enum class HeroClass(
    val displayName: String,
    val group: ClassGroup,
    val roleSummary: String,
    val armorWeight: ArmorWeight,
    val weaponCategory: WeaponCategory,
    val baseHp: Int,
    val baseMana: Int,
    val baseAttack: Int,
    val baseArmor: Int,
    val baseSpeed: Float,
    val baseDodge: Float,
    val attackRangeTiles: Float,
    val accentHex: Long,
    val isRanged: Boolean
) {
    PALADIN(
        displayName = "Paladín",
        group = ClassGroup.PLATE_VANGUARD,
        roleSummary = "Tanque, DPS medio y sanación sagrada.",
        armorWeight = ArmorWeight.PLATE,
        weaponCategory = WeaponCategory.HEAVY_MELEE,
        baseHp = 185,
        baseMana = 120,
        baseAttack = 18,
        baseArmor = 22,
        baseSpeed = 4.2f,
        baseDodge = 0.05f,
        attackRangeTiles = 1.65f,
        accentHex = 0xFFFACC15,
        isRanged = false
    ),
    WARRIOR(
        displayName = "Guerrero",
        group = ClassGroup.PLATE_VANGUARD,
        roleSummary = "Tanque principal, DPS y muy alta resistencia.",
        armorWeight = ArmorWeight.PLATE,
        weaponCategory = WeaponCategory.HEAVY_MELEE,
        baseHp = 220,
        baseMana = 90,
        baseAttack = 20,
        baseArmor = 30,
        baseSpeed = 4.25f,
        baseDodge = 0.06f,
        attackRangeTiles = 1.65f,
        accentHex = 0xFFDC2626,
        isRanged = false
    ),
    KNIGHT(
        displayName = "Caballero",
        group = ClassGroup.PLATE_VANGUARD,
        roleSummary = "DPS en placas, roba vida y resistencia superior al Paladín.",
        armorWeight = ArmorWeight.PLATE,
        weaponCategory = WeaponCategory.HEAVY_MELEE,
        baseHp = 198,
        baseMana = 105,
        baseAttack = 23,
        baseArmor = 25,
        baseSpeed = 4.3f,
        baseDodge = 0.06f,
        attackRangeTiles = 1.7f,
        accentHex = 0xFF991B1B,
        isRanged = false
    ),
    HUNTER_SURVIVAL(
        displayName = "Cazador Supervivencia",
        group = ClassGroup.HUNTER,
        roleSummary = "Alto DPS ágil con gran evasión y velocidad de movimiento.",
        armorWeight = ArmorWeight.MAIL_LEATHER,
        weaponCategory = WeaponCategory.BOW_SPEAR,
        baseHp = 145,
        baseMana = 110,
        baseAttack = 24,
        baseArmor = 14,
        baseSpeed = 5.05f,
        baseDodge = 0.22f,
        attackRangeTiles = 4.5f,
        accentHex = 0xFF22C55E,
        isRanged = true
    ),
    HUNTER_BEASTMASTER(
        displayName = "Maestro de Bestias",
        group = ClassGroup.HUNTER,
        roleSummary = "Doma bestias tanque que absorben daño y generan alta amenaza.",
        armorWeight = ArmorWeight.MAIL_LEATHER,
        weaponCategory = WeaponCategory.BOW_SPEAR,
        baseHp = 155,
        baseMana = 120,
        baseAttack = 19,
        baseArmor = 15,
        baseSpeed = 4.6f,
        baseDodge = 0.12f,
        attackRangeTiles = 4.8f,
        accentHex = 0xFFD97706,
        isRanged = true
    ),
    HUNTER_TRAPPER(
        displayName = "Experto en Trampas",
        group = ClassGroup.HUNTER,
        roleSummary = "Inmoviliza con trampas, ralentiza y envenena reduciendo curación.",
        armorWeight = ArmorWeight.MAIL_LEATHER,
        weaponCategory = WeaponCategory.BOW_SPEAR,
        baseHp = 148,
        baseMana = 115,
        baseAttack = 21,
        baseArmor = 14,
        baseSpeed = 4.65f,
        baseDodge = 0.14f,
        attackRangeTiles = 5.0f,
        accentHex = 0xFF84CC16,
        isRanged = true
    ),
    MAGE_WARLOCK(
        displayName = "Brujo",
        group = ClassGroup.MAGE,
        roleSummary = "Maldiciones variadas e invocación demoníaca que potencia su daño.",
        armorWeight = ArmorWeight.CLOTH,
        weaponCategory = WeaponCategory.STAFF_GRIMOIRE,
        baseHp = 138,
        baseMana = 155,
        baseAttack = 23,
        baseArmor = 10,
        baseSpeed = 4.3f,
        baseDodge = 0.08f,
        attackRangeTiles = 5.2f,
        accentHex = 0xFF9333EA,
        isRanged = true
    ),
    MAGE_ELEMENTAL(
        displayName = "Mago Elemental",
        group = ClassGroup.MAGE,
        roleSummary = "Domina Fuego, Escarcha y Relámpago para daño explosivo en área.",
        armorWeight = ArmorWeight.CLOTH,
        weaponCategory = WeaponCategory.STAFF_GRIMOIRE,
        baseHp = 130,
        baseMana = 165,
        baseAttack = 26,
        baseArmor = 9,
        baseSpeed = 4.35f,
        baseDodge = 0.09f,
        attackRangeTiles = 5.4f,
        accentHex = 0xFF38BDF8,
        isRanged = true
    ),
    MAGE_SUMMONER(
        displayName = "Invocador",
        group = ClassGroup.MAGE,
        roleSummary = "Ralentiza, aturde con explosiones y fuerza duelos 1v1 con su invocación.",
        armorWeight = ArmorWeight.CLOTH,
        weaponCategory = WeaponCategory.STAFF_GRIMOIRE,
        baseHp = 140,
        baseMana = 160,
        baseAttack = 21,
        baseArmor = 11,
        baseSpeed = 4.35f,
        baseDodge = 0.09f,
        attackRangeTiles = 5.2f,
        accentHex = 0xFF6366F1,
        isRanged = true
    ),
    SHAMAN(
        displayName = "Chamán",
        group = ClassGroup.SHAMAN,
        roleSummary = "Tótems espirituales, choques elementales y sanación ancestral.",
        armorWeight = ArmorWeight.MAIL_LEATHER,
        weaponCategory = WeaponCategory.RUNIC_AXE_TOTEM,
        baseHp = 165,
        baseMana = 145,
        baseAttack = 20,
        baseArmor = 17,
        baseSpeed = 4.45f,
        baseDodge = 0.10f,
        attackRangeTiles = 4.2f,
        accentHex = 0xFF0EA5E9,
        isRanged = true
    ),
    STALKER(
        displayName = "Acechador",
        group = ClassGroup.STALKER,
        roleSummary = "Asesino sigiloso (Pícaro): golpes críticos, combo y evasión letal.",
        armorWeight = ArmorWeight.MAIL_LEATHER,
        weaponCategory = WeaponCategory.TWIN_DAGGERS,
        baseHp = 145,
        baseMana = 100,
        baseAttack = 27,
        baseArmor = 13,
        baseSpeed = 5.15f,
        baseDodge = 0.24f,
        attackRangeTiles = 1.6f,
        accentHex = 0xFFE11D48,
        isRanged = false
    )
}

enum class ItemRarity(
    val label: String,
    val color: Color,
    val statMultiplier: Float,
    val maxLevelBypass: Int
) {
    COMMON("Común", RarityCommon, 1.0f, 0),
    RARE("Raro", RarityRare, 1.35f, 2),
    EPIC("Épico", RarityEpic, 1.75f, 4),
    LEGENDARY("Legendario", RarityLegendary, 2.3f, HIGH_TIER_EQUIP_LEVEL_BONUS),
    UNIQUE_SET("Set Único", RaritySetUnique, 2.45f, HIGH_TIER_EQUIP_LEVEL_BONUS)
}

enum class EquipSlot(val label: String) {
    HELM("Casco"),
    BRACERS("Brazales"),
    ARMOR("Pecho"),
    BOOTS("Botas"),
    WEAPON("Arma"),
    TRINKET("Reliquia")
}

@JsonClass(generateAdapter = true)
data class EquipmentItem(
    val id: String,
    val name: String,
    val slot: EquipSlot,
    val rarity: ItemRarity,
    val requiredLevel: Int,
    val attackBonus: Int = 0,
    val armorBonus: Int = 0,
    val hpBonus: Int = 0,
    val manaBonus: Int = 0,
    val critPctBonus: Float = 0f,
    val dodgePctBonus: Float = 0f,
    val speedPctBonus: Float = 0f,
    val lifestealPctBonus: Float = 0f,
    val setId: String? = null,
    val setName: String? = null,
    val goldValue: Int = 15,
    val description: String = "",
    val armorWeight: ArmorWeight? = null,
    val weaponCategory: WeaponCategory? = null
) {
    fun canEquipAtLevel(playerLevel: Int): Boolean {
        return playerLevel + rarity.maxLevelBypass >= requiredLevel
    }

    fun effectiveMinLevel(): Int {
        return (requiredLevel - rarity.maxLevelBypass).coerceAtLeast(1)
    }

    fun isCompatibleWithClass(heroClass: HeroClass): Boolean = when (slot) {
        EquipSlot.WEAPON -> weaponCategory == null || weaponCategory == heroClass.weaponCategory
        EquipSlot.HELM, EquipSlot.BRACERS, EquipSlot.ARMOR, EquipSlot.BOOTS ->
            armorWeight == null || armorWeight == heroClass.armorWeight
        EquipSlot.TRINKET -> true
    }

    fun canBeEquippedBy(heroClass: HeroClass, playerLevel: Int): Boolean {
        return isCompatibleWithClass(heroClass) && canEquipAtLevel(playerLevel)
    }

    fun classCompatibilityNote(heroClass: HeroClass): String? = when {
        slot == EquipSlot.WEAPON && weaponCategory != null && weaponCategory != heroClass.weaponCategory ->
            "Solo armamento: ${weaponCategory.label}"
        slot != EquipSlot.WEAPON && slot != EquipSlot.TRINKET && armorWeight != null && armorWeight != heroClass.armorWeight ->
            "Solo armadura de ${armorWeight.label}"
        else -> null
    }
}

data class SetBonusDef(
    val setId: String,
    val setName: String,
    val armorWeight: ArmorWeight,
    val twoPieceDesc: String,
    val fourPieceDesc: String,
    val fivePieceDesc: String = "",
    val twoPieceAtk: Int = 0,
    val twoPieceHp: Int = 0,
    val twoPieceArmor: Int = 0,
    val fourPieceAtk: Int = 0,
    val fourPieceHp: Int = 0,
    val fourPieceArmor: Int = 0,
    val fourPieceCrit: Float = 0f,
    val fourPieceLifesteal: Float = 0f,
    val fivePieceAtk: Int = 0,
    val fivePieceHp: Int = 0,
    val fivePieceArmor: Int = 0,
    val fivePieceCrit: Float = 0f,
    val fivePieceDodge: Float = 0f,
    val fivePieceSpeedPct: Float = 0f,
    val fivePieceLifesteal: Float = 0f
)

data class SkillDef(
    val id: String,
    val name: String,
    val description: String,
    val manaCost: Int,
    val cooldownSec: Float,
    val colorHex: Long,
    val tag: String
)

enum class ZoneId(
    val displayName: String,
    val levelRange: String,
    val isSafeZone: Boolean,
    val ambientColorHex: Long
) {
    VALORIA_VILLAGE("Aldea de Valoria", "Zona Segura", true, 0xFF3E4A36),
    EMERALD_FOREST("Bosque Esmeralda", "Nv. 1 - 7", false, 0xFF1F4D2B),
    TWILIGHT_RUINS("Ruinas del Crepúsculo", "Nv. 8 - 15", false, 0xFF353147),
    CURSED_SWAMP("Pantano Maldito", "Nv. 16 - 23", false, 0xFF233831),
    DRAGON_SUMMIT("Cumbre del Dragón", "Nv. 24 - 30", false, 0xFF421E22)
}

enum class BossTactic(
    val displayName: String,
    val dodgeHint: String,
    val warningCallout: String,
    val windupSec: Float,
    val damageMult: Float,
    val rangeTiles: Float
) {
    RADIAL_SLAM(
        displayName = "Onda Sísmica",
        dodgeHint = "Aléjate del círculo rojo",
        warningCallout = "¡GOLPE SÍSMICO!",
        windupSec = 1.25f,
        damageMult = 1.55f,
        rangeTiles = 2.65f
    ),
    LINEAR_CHARGE(
        displayName = "Embestida Brutal",
        dodgeHint = "Muévete a un lado de la línea",
        warningCallout = "¡EMBESTIDA!",
        windupSec = 1.15f,
        damageMult = 1.65f,
        rangeTiles = 4.6f
    ),
    METEOR_ZONE(
        displayName = "Impacto Arcano",
        dodgeHint = "Sal del círculo marcado",
        warningCallout = "¡IMPACTO!",
        windupSec = 1.30f,
        damageMult = 1.60f,
        rangeTiles = 1.85f
    ),
    SHOCKWAVE_CONE(
        displayName = "Aliento / Tajo Frontal",
        dodgeHint = "Rodéalo por detrás o un lado",
        warningCallout = "¡TAJO FRONTAL!",
        windupSec = 1.20f,
        damageMult = 1.60f,
        rangeTiles = 3.35f
    )
}

enum class EnemyRace(val displayName: String) {
    WOLF("Licántropos y Lobos"),
    BOAR("Jabalíes Acorazados"),
    FOREST_ENT("Espíritus del Espino"),
    OUTLAW("Bandidos del Sendero"),
    FOREST_SHADOW("Sombras Silvanas"),
    UNDEAD_BONE("Legión Ósea"),
    RUNE_CONSTRUCT("Constructos Rúnicos"),
    GOBLIN("Clan Goblin"),
    VAMPIRE("Estirpe Carmesí"),
    FALLEN_ORDER("Orden Caída"),
    WITCH_COVEN("Aquelarre del Pantano"),
    MIRE_COLOSSUS("Colosos del Lodo"),
    HYDRA("Estirpe de Hidra"),
    SPECTER("Espectros Sombríos"),
    TROLL("Tribu Troll"),
    DRAKONID("Legión Dracónida"),
    MAGMA_GOLEM("Gólems Volcánicos"),
    WYVERN("Guivernos de Ceniza"),
    HELLFIRE_CULT("Culto del Averno"),
    DRAGONKIN("Guardia del Wyrm")
}

enum class EnemyKind(
    val displayName: String,
    val zone: ZoneId,
    val race: EnemyRace,
    val baseLevel: Int,
    val baseHp: Int,
    val baseAttack: Int,
    val speed: Float,
    val xpReward: Int,
    val goldReward: Int,
    val isBoss: Boolean = false,
    val bodyColorHex: Long,
    val accentHex: Long,
    val bossTactic: BossTactic? = null,
    val bossDifficultyOffset: Int = 0,
    val bossHpMult: Float = 1.0f,
    val bossAtkMult: Float = 1.0f,
    val isMiniboss: Boolean = false
) {
    // --- Regular Zone Mobs (Grouped by Race) ---
    WOLF("Lobo Salvaje", ZoneId.EMERALD_FOREST, EnemyRace.WOLF, 2, 70, 9, 2.8f, 38, 8, false, 0xFF78716C, 0xFFD6D3D1),
    BOAR("Jabalí Furioso", ZoneId.EMERALD_FOREST, EnemyRace.BOAR, 3, 95, 11, 2.4f, 52, 12, false, 0xFF7C4A2D, 0xFFFDE68A),
    THORN_SPRITE("Brote Espinoso", ZoneId.EMERALD_FOREST, EnemyRace.FOREST_ENT, 4, 115, 12, 2.2f, 62, 16, false, 0xFF3F6212, 0xFF86EFAC),
    BANDIT("Bandido del Bosque", ZoneId.EMERALD_FOREST, EnemyRace.OUTLAW, 5, 130, 14, 2.7f, 75, 22, false, 0xFF475569, 0xFFEF4444),
    FOREST_PROWLER("Acechador Silvano", ZoneId.EMERALD_FOREST, EnemyRace.FOREST_SHADOW, 6, 135, 15, 3.0f, 88, 25, false, 0xFF065F46, 0xFFA3E635),

    SKELETON("Esqueleto Guerrero", ZoneId.TWILIGHT_RUINS, EnemyRace.UNDEAD_BONE, 9, 220, 23, 2.5f, 125, 32, false, 0xFFE2E8F0, 0xFF64748B),
    RUNE_CONSTRUCT("Autómata Rúnico", ZoneId.TWILIGHT_RUINS, EnemyRace.RUNE_CONSTRUCT, 10, 260, 24, 2.1f, 140, 36, false, 0xFF475569, 0xFF38BDF8),
    MARAUDER("Goblin Saqueador", ZoneId.TWILIGHT_RUINS, EnemyRace.GOBLIN, 11, 240, 26, 3.0f, 155, 40, false, 0xFF65A30D, 0xFFFACC15),
    BLOOD_ACOLYTE("Acólito Carmesí", ZoneId.TWILIGHT_RUINS, EnemyRace.VAMPIRE, 13, 275, 28, 2.9f, 180, 48, false, 0xFF581C87, 0xFFE11D48),
    FALLEN_SQUIRE("Escudero Caído", ZoneId.TWILIGHT_RUINS, EnemyRace.FALLEN_ORDER, 14, 310, 29, 2.5f, 200, 54, false, 0xFF312E81, 0xFFA855F7),

    SWAMP_WITCH("Bruja del Pantano", ZoneId.CURSED_SWAMP, EnemyRace.WITCH_COVEN, 17, 410, 44, 2.5f, 240, 62, false, 0xFF4C1D95, 0xFF10B981),
    MUD_BRUTE("Bruto del Fango", ZoneId.CURSED_SWAMP, EnemyRace.MIRE_COLOSSUS, 18, 520, 46, 2.0f, 275, 70, false, 0xFF365314, 0xFF84CC16),
    SWAMP_SERPENT("Sierpe de Ciénaga", ZoneId.CURSED_SWAMP, EnemyRace.HYDRA, 20, 490, 50, 2.8f, 305, 78, false, 0xFF0F766E, 0xFFFACC15),
    SPECTER("Espectro Sombrío", ZoneId.CURSED_SWAMP, EnemyRace.SPECTER, 21, 530, 54, 3.1f, 340, 88, false, 0xFF1E293B, 0xFF2DD4BF),
    TROLL("Troll de Ciénaga", ZoneId.CURSED_SWAMP, EnemyRace.TROLL, 22, 620, 52, 2.1f, 360, 95, false, 0xFF15803D, 0xFF84CC16),

    DRAKONID("Guardia Dracónido", ZoneId.DRAGON_SUMMIT, EnemyRace.DRAKONID, 25, 820, 68, 2.7f, 480, 130, false, 0xFF991B1B, 0xFFF59E0B),
    OBSIDIAN_GOLEM("Gólem de Obsidiana", ZoneId.DRAGON_SUMMIT, EnemyRace.MAGMA_GOLEM, 26, 1020, 72, 1.9f, 520, 145, false, 0xFF27272A, 0xFFEA580C),
    ASH_DRAKE("Cría de Guiverno", ZoneId.DRAGON_SUMMIT, EnemyRace.WYVERN, 27, 940, 75, 2.9f, 560, 155, false, 0xFF57534E, 0xFFEF4444),
    ASH_WARLOCK("Cultista del Averno", ZoneId.DRAGON_SUMMIT, EnemyRace.HELLFIRE_CULT, 28, 980, 78, 2.5f, 600, 170, false, 0xFF18181B, 0xFFA855F7),
    WYRMGUARD("Centinela del Wyrm", ZoneId.DRAGON_SUMMIT, EnemyRace.DRAGONKIN, 29, 1080, 80, 2.5f, 650, 185, false, 0xFF7F1D1D, 0xFFFBBF24),

    // --- 20 Enclave Minibosses (1 per Enclave, same race as Boss & guards, below weakest Boss in zone) ---
    MINIBOSS_DIRE_WOLF("Minijefe Huargo Alfa", ZoneId.EMERALD_FOREST, EnemyRace.WOLF, 2, 168, 15, 2.9f, 95, 28, false, 0xFF94A3B8, 0xFF38BDF8, isMiniboss = true),
    MINIBOSS_IRON_TUSK("Minijefe Jabalí Blindado", ZoneId.EMERALD_FOREST, EnemyRace.BOAR, 3, 182, 16, 2.45f, 108, 32, false, 0xFF92400E, 0xFFFBBF24, isMiniboss = true),
    MINIBOSS_THORN_GUARDIAN("Minijefe Guardián Espino", ZoneId.EMERALD_FOREST, EnemyRace.FOREST_ENT, 4, 195, 16, 2.1f, 120, 36, false, 0xFF365314, 0xFF4ADE80, isMiniboss = true),
    MINIBOSS_BANDIT_CHAMPION("Minijefe Lugarteniente", ZoneId.EMERALD_FOREST, EnemyRace.OUTLAW, 6, 205, 17, 2.75f, 132, 42, false, 0xFF334155, 0xFFF87171, isMiniboss = true),
    MINIBOSS_SHADOW_HUNTER("Minijefe Cazador Sombrío", ZoneId.EMERALD_FOREST, EnemyRace.FOREST_SHADOW, 7, 212, 18, 3.05f, 145, 46, false, 0xFF064E3B, 0xFFBEF264, isMiniboss = true),

    MINIBOSS_BONE_SENTINEL("Minijefe Guardia Óseo", ZoneId.TWILIGHT_RUINS, EnemyRace.UNDEAD_BONE, 9, 410, 31, 2.55f, 210, 62, false, 0xFFCBD5E1, 0xFFF59E0B, isMiniboss = true),
    MINIBOSS_RUNE_WARDEN("Minijefe Vigía Rúnico", ZoneId.TWILIGHT_RUINS, EnemyRace.RUNE_CONSTRUCT, 11, 460, 32, 2.1f, 235, 70, false, 0xFF64748B, 0xFF38BDF8, isMiniboss = true),
    MINIBOSS_GOBLIN_SHAMAN("Minijefe Chamán Goblin", ZoneId.TWILIGHT_RUINS, EnemyRace.GOBLIN, 12, 440, 34, 2.95f, 255, 76, false, 0xFF4D7C0F, 0xFFF97316, isMiniboss = true),
    MINIBOSS_BLOOD_KNIGHT("Minijefe Vampiro Duelista", ZoneId.TWILIGHT_RUINS, EnemyRace.VAMPIRE, 14, 480, 35, 3.1f, 285, 86, false, 0xFF6B21A8, 0xFFE11D48, isMiniboss = true),
    MINIBOSS_DEATH_CAPTAIN("Minijefe Capitán Caído", ZoneId.TWILIGHT_RUINS, EnemyRace.FALLEN_ORDER, 15, 520, 36, 2.6f, 310, 94, false, 0xFF3730A3, 0xFFC084FC, isMiniboss = true),

    MINIBOSS_COVEN_MATRON("Minijefe Suma Bruja", ZoneId.CURSED_SWAMP, EnemyRace.WITCH_COVEN, 17, 790, 58, 2.5f, 410, 120, false, 0xFF581C87, 0xFF4ADE80, isMiniboss = true),
    MINIBOSS_MIRE_CRUSHER("Minijefe Titán del Lodo", ZoneId.CURSED_SWAMP, EnemyRace.MIRE_COLOSSUS, 19, 890, 60, 2.0f, 450, 134, false, 0xFF3F6212, 0xFFA3E635, isMiniboss = true),
    MINIBOSS_HYDRA_MATRIARCH("Minijefe Matriarca Hidra", ZoneId.CURSED_SWAMP, EnemyRace.HYDRA, 21, 930, 62, 2.65f, 495, 148, false, 0xFF115E59, 0xFFFDE047, isMiniboss = true),
    MINIBOSS_WRAITH_LORD("Minijefe Señor Espectral", ZoneId.CURSED_SWAMP, EnemyRace.SPECTER, 22, 960, 64, 3.05f, 530, 160, false, 0xFF0F172A, 0xFF2DD4BF, isMiniboss = true),
    MINIBOSS_TROLL_WARLORD("Minijefe Caudillo Troll", ZoneId.CURSED_SWAMP, EnemyRace.TROLL, 24, 1040, 65, 2.25f, 580, 175, false, 0xFF14532D, 0xFFF59E0B, isMiniboss = true),

    MINIBOSS_DRAKONID_CHAMPION("Minijefe Campeón Dracónido", ZoneId.DRAGON_SUMMIT, EnemyRace.DRAKONID, 25, 1420, 82, 2.7f, 720, 220, false, 0xFF991B1B, 0xFFFBBF24, isMiniboss = true),
    MINIBOSS_MOLTEN_GOLEM("Minijefe Gólem Fundido", ZoneId.DRAGON_SUMMIT, EnemyRace.MAGMA_GOLEM, 27, 1620, 84, 2.0f, 790, 245, false, 0xFF3F3F46, 0xFFF97316, isMiniboss = true),
    MINIBOSS_ELDER_WYVERN("Minijefe Guiverno Alfa", ZoneId.DRAGON_SUMMIT, EnemyRace.WYVERN, 28, 1680, 86, 2.9f, 840, 265, false, 0xFF44403C, 0xFFEF4444, isMiniboss = true),
    MINIBOSS_LICH_INVOKER("Minijefe Invocador Exánime", ZoneId.DRAGON_SUMMIT, EnemyRace.HELLFIRE_CULT, 29, 1720, 88, 2.55f, 900, 290, false, 0xFF27272A, 0xFFA855F7, isMiniboss = true),
    MINIBOSS_DRAGON_HERALD("Minijefe Heraldo del Wyrm", ZoneId.DRAGON_SUMMIT, EnemyRace.DRAGONKIN, 30, 1850, 90, 2.55f, 980, 320, false, 0xFFB91C1C, 0xFFFDE047, isMiniboss = true),

    // --- 20 Varied Zone Bosses (5 per Combat Zone, centered in Racial Enclaves, scaling Levels 1..30) ---
    BOSS_ALPHA_WOLF("Colmillo de Plata", ZoneId.EMERALD_FOREST, EnemyRace.WOLF, 2, 240, 19, 2.85f, 160, 48, true, 0xFFCBD5E1, 0xFF38BDF8, BossTactic.LINEAR_CHARGE, 0, 0.94f, 1.00f),
    BOSS_IRON_BOAR("Jabalí Colmilloferro", ZoneId.EMERALD_FOREST, EnemyRace.BOAR, 3, 305, 21, 2.45f, 180, 55, true, 0xFF92400E, 0xFFF59E0B, BossTactic.LINEAR_CHARGE, 0, 1.02f, 0.96f),
    BOSS_THORN_ENT("Anciano Espino", ZoneId.EMERALD_FOREST, EnemyRace.FOREST_ENT, 4, 380, 24, 2.0f, 210, 64, true, 0xFF3F6212, 0xFF4ADE80, BossTactic.RADIAL_SLAM, 1, 1.12f, 0.95f),
    BOSS_BANDIT_CAPTAIN("Varek el Saqueador", ZoneId.EMERALD_FOREST, EnemyRace.OUTLAW, 6, 440, 28, 2.75f, 235, 74, true, 0xFF334155, 0xFFEF4444, BossTactic.SHOCKWAVE_CONE, 1, 1.00f, 1.06f),
    BOSS_FOREST_STALKER("Sombra del Roble", ZoneId.EMERALD_FOREST, EnemyRace.FOREST_SHADOW, 7, 480, 32, 3.05f, 260, 82, true, 0xFF065F46, 0xFFA3E635, BossTactic.METEOR_ZONE, 2, 0.98f, 1.12f),

    BOSS_BONE_WARLORD("Señor Óseo Kael", ZoneId.TWILIGHT_RUINS, EnemyRace.UNDEAD_BONE, 9, 640, 38, 2.55f, 310, 92, true, 0xFFF1F5F9, 0xFFF59E0B, BossTactic.SHOCKWAVE_CONE, 0, 1.00f, 1.02f),
    BOSS_RUNE_GOLEM("Centinela Rúnico", ZoneId.TWILIGHT_RUINS, EnemyRace.RUNE_CONSTRUCT, 11, 860, 42, 2.05f, 355, 105, true, 0xFF475569, 0xFF38BDF8, BossTactic.RADIAL_SLAM, 1, 1.15f, 0.96f),
    BOSS_GOBLIN_PYROMANCER("Zik el Piroclástico", ZoneId.TWILIGHT_RUINS, EnemyRace.GOBLIN, 12, 820, 48, 2.9f, 380, 115, true, 0xFF4D7C0F, 0xFFF97316, BossTactic.METEOR_ZONE, 1, 0.95f, 1.14f),
    BOSS_CRYPT_HUNTER("Vampira Carmesí", ZoneId.TWILIGHT_RUINS, EnemyRace.VAMPIRE, 14, 960, 53, 3.1f, 420, 126, true, 0xFF581C87, 0xFFE11D48, BossTactic.LINEAR_CHARGE, 2, 0.98f, 1.15f),
    FALLEN_KNIGHT("Caballero Caído", ZoneId.TWILIGHT_RUINS, EnemyRace.FALLEN_ORDER, 15, 1160, 56, 2.6f, 460, 140, true, 0xFF312E81, 0xFFA855F7, BossTactic.RADIAL_SLAM, 2, 1.10f, 1.08f),

    BOSS_PLAGUE_MATRON("Bruja Madre Morwen", ZoneId.CURSED_SWAMP, EnemyRace.WITCH_COVEN, 17, 1320, 68, 2.5f, 580, 168, true, 0xFF6B21A8, 0xFF4ADE80, BossTactic.METEOR_ZONE, 0, 0.96f, 1.10f),
    BOSS_MIRE_BEHEMOTH("Coloso del Lodo", ZoneId.CURSED_SWAMP, EnemyRace.MIRE_COLOSSUS, 19, 1720, 72, 2.0f, 650, 188, true, 0xFF365314, 0xFF84CC16, BossTactic.RADIAL_SLAM, 1, 1.18f, 0.98f),
    BOSS_HYDRA_SPAWN("Hidra de Ciénaga", ZoneId.CURSED_SWAMP, EnemyRace.HYDRA, 21, 1840, 79, 2.55f, 730, 212, true, 0xFF0F766E, 0xFFFACC15, BossTactic.SHOCKWAVE_CONE, 1, 1.08f, 1.08f),
    BOSS_DREAD_REAPER("Segador del Fango", ZoneId.CURSED_SWAMP, EnemyRace.SPECTER, 22, 1880, 85, 3.05f, 780, 230, true, 0xFF1E293B, 0xFF2DD4BF, BossTactic.LINEAR_CHARGE, 2, 1.00f, 1.16f),
    BOSS_BOG_KING("Rey Troll Grimgor", ZoneId.CURSED_SWAMP, EnemyRace.TROLL, 24, 2280, 90, 2.25f, 880, 260, true, 0xFF166534, 0xFFF59E0B, BossTactic.RADIAL_SLAM, 2, 1.16f, 1.10f),

    BOSS_PYRO_DRAKONID("Comandante Ignarok", ZoneId.DRAGON_SUMMIT, EnemyRace.DRAKONID, 25, 2420, 94, 2.7f, 1020, 310, true, 0xFF7F1D1D, 0xFFFBBF24, BossTactic.SHOCKWAVE_CONE, 0, 1.04f, 1.06f),
    BOSS_MAGMA_COLOSSUS("Titán de Magma", ZoneId.DRAGON_SUMMIT, EnemyRace.MAGMA_GOLEM, 27, 2940, 99, 2.0f, 1140, 345, true, 0xFF27272A, 0xFFF97316, BossTactic.RADIAL_SLAM, 1, 1.20f, 1.00f),
    BOSS_ASH_WYVERN("Guiverno Ceniza", ZoneId.DRAGON_SUMMIT, EnemyRace.WYVERN, 28, 2860, 106, 2.9f, 1240, 380, true, 0xFF57534E, 0xFFEF4444, BossTactic.LINEAR_CHARGE, 1, 1.06f, 1.12f),
    BOSS_HELLFIRE_LICH("Archimago de Ceniza", ZoneId.DRAGON_SUMMIT, EnemyRace.HELLFIRE_CULT, 29, 2920, 112, 2.55f, 1340, 420, true, 0xFF18181B, 0xFFA855F7, BossTactic.METEOR_ZONE, 2, 1.02f, 1.16f),
    DRAGON_BOSS("Ignis, Wyrm Ancestral", ZoneId.DRAGON_SUMMIT, EnemyRace.DRAGONKIN, 30, 3680, 122, 2.5f, 1750, 580, true, 0xFFB91C1C, 0xFFFBBF24, BossTactic.SHOCKWAVE_CONE, 2, 1.18f, 1.18f)
}

data class BossTelegraph(
    val id: Long,
    val bossId: String,
    val bossName: String,
    val tactic: BossTactic,
    val originX: Float,
    val originY: Float,
    val targetX: Float,
    val targetY: Float,
    val dirX: Float,
    val dirY: Float,
    val radiusTiles: Float,
    val totalWindupSec: Float,
    val remainingSec: Float,
    val damage: Int,
    val colorHex: Long
) {
    val progress: Float
        get() = if (totalWindupSec > 0f) (1f - remainingSec / totalWindupSec).coerceIn(0f, 1f) else 1f
}

enum class SummonKind(val displayName: String, val colorHex: Long) {
    TANK_BEAST("Bestia Guardiana", 0xFFD97706),
    ARCANE_SENTINEL("Centinela Arcano", 0xFF6366F1),
    WARLOCK_DEMON("Demonio del Pacto", 0xFF9333EA),
    HEALING_TOTEM("Tótem Curativo", 0xFF10B981),
    STORM_TOTEM("Tótem de Tormenta", 0xFF38BDF8),
    SNARE_TRAP("Trampa de Cepo", 0xFF84CC16)
}

data class SummonInstance(
    val id: String,
    val ownerId: String,
    val kind: SummonKind,
    var x: Float,
    var y: Float,
    var hp: Int,
    val maxHp: Int,
    val attack: Int,
    val armor: Int,
    val threatMultiplier: Float,
    var remainingSec: Float,
    var attackCooldown: Float = 0f,
    var forcedDuelTargetId: String? = null,
    var forcedDuelRemainingSec: Float = 0f
)

enum class EnemyAiState(val displayLabel: String) {
    IDLE("Patrulla"),
    CHASE("Persigue"),
    ATTACK("Ataca"),
    FLEE("Huye")
}

data class EnemyInstance(
    val id: String,
    val kind: EnemyKind,
    val spawnX: Float,
    val spawnY: Float,
    var x: Float,
    var y: Float,
    var hp: Int,
    val maxHp: Int,
    var attackCooldown: Float = 0f,
    var stunnedSec: Float = 0f,
    var rootedSec: Float = 0f,
    var slowedSec: Float = 0f,
    var poisonSec: Float = 0f,
    var poisonDps: Int = 0,
    var cursedAgonySec: Float = 0f,
    var cursedWeakSec: Float = 0f,
    var forcedDuelSummonId: String? = null,
    var forcedDuelSec: Float = 0f,
    var respawnTimerSec: Float = 0f,
    var targetId: String? = null,
    val aiState: EnemyAiState = EnemyAiState.IDLE,
    val recentlyDamagedSec: Float = 0f,
    val telegraphCooldownSec: Float = 2.6f,
    val activeTelegraphId: Long? = null,
    val effectiveLevel: Int = kind.baseLevel,
    val scaledAttack: Int = kind.baseAttack,
    val scaledXpReward: Int = kind.xpReward,
    val scaledGoldReward: Int = kind.goldReward,
    val enclaveId: String? = null
) {
    val isAlive: Boolean get() = hp > 0
}

data class WorldLootDrop(
    val id: String,
    var x: Float,
    var y: Float,
    val item: EquipmentItem?,
    val gold: Int = 0,
    val potionCount: Int = 0,
    var ttlSec: Float = 60f
)

data class WorldChest(
    val id: String,
    val x: Float,
    val y: Float,
    val zone: ZoneId,
    val minRarity: ItemRarity,
    var opened: Boolean = false,
    var respawnSec: Float = 0f
)

enum class FloatingTextKind(val defaultColor: Color) {
    NORMAL_DAMAGE(Color.White),
    CRITICAL_HIT(Color(0xFFFACC15)),
    ENEMY_DAMAGE(Color(0xFFEF4444)),
    HEAL(Color(0xFF10B981)),
    STATUS(Color(0xFF38BDF8)),
    LOOT(Color(0xFFF5B942))
}

data class FloatingText(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val kind: FloatingTextKind = FloatingTextKind.STATUS,
    val vx: Float = 0f,
    val vy: Float = -0.65f,
    val maxTtlSec: Float = 1.15f,
    var ttlSec: Float = maxTtlSec
) {
    val lifeProgress: Float
        get() = if (maxTtlSec > 0f) (1f - (ttlSec / maxTtlSec)).coerceIn(0f, 1f) else 1f
}

data class SpellEffectVisual(
    val id: Long,
    val x: Float,
    val y: Float,
    val radiusTiles: Float,
    val color: Color,
    var ttlSec: Float = 0.45f
)

enum class QuestObjectiveType {
    TALK_NPC,
    KILL_ENEMY,
    REACH_ZONE,
    CLEAR_ENCLAVES
}

data class QuestDef(
    val id: String,
    val title: String,
    val description: String,
    val giverNpcId: String,
    val turnInNpcId: String,
    val requiredLevel: Int,
    val prerequisiteQuestId: String?,
    val objectiveType: QuestObjectiveType,
    val targetEnemyKind: EnemyKind? = null,
    val targetZone: ZoneId? = null,
    val targetNpcId: String? = null,
    val requiredCount: Int = 1,
    val rewardXp: Int,
    val rewardGold: Int,
    val rewardItem: EquipmentItem? = null,
    val rewardSetTier: Int? = null,
    val rewardSetSlots: List<EquipSlot> = emptyList()
) {
    fun resolveRewardItems(heroClass: HeroClass): List<EquipmentItem> {
        if (rewardSetTier != null && rewardSetSlots.isNotEmpty()) {
            return LootAndQuestData.buildClassSetQuestRewards(
                questId = id,
                tier = rewardSetTier,
                slots = rewardSetSlots,
                heroClass = heroClass,
                baseLevel = requiredLevel
            )
        }
        return listOfNotNull(rewardItem?.let { LootAndQuestData.adaptQuestItemForClass(it, heroClass) })
    }
}

@JsonClass(generateAdapter = true)
data class QuestProgress(
    val questId: String,
    val currentCount: Int = 0,
    val completed: Boolean = false,
    val turnedIn: Boolean = false
)

enum class NpcRole {
    QUEST_GIVER,
    MERCHANT_BLACKSMITH,
    HEALER_INNKEEPER
}

data class NpcDef(
    val id: String,
    val name: String,
    val title: String,
    val role: NpcRole,
    val x: Float,
    val y: Float,
    val tunicColorHex: Long,
    val greeting: String
)

@JsonClass(generateAdapter = true)
data class RemotePlayerState(
    val playerId: String,
    val name: String,
    val race: Race,
    val heroClass: HeroClass,
    val level: Int,
    val x: Float,
    val y: Float,
    val hp: Int,
    val maxHp: Int,
    val isAttacking: Boolean = false,
    val facingRight: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CharacterProfile(
    val id: String,
    val name: String,
    val race: Race,
    val heroClass: HeroClass,
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = 50,
    val potions: Int = 5,
    val posX: Float = 10f,
    val posY: Float = 10f,
    val equipped: Map<EquipSlot, EquipmentItem> = emptyMap(),
    val inventory: List<EquipmentItem> = emptyList(),
    val quests: List<QuestProgress> = emptyList(),
    val clearedEnclaves: Set<String> = emptySet(),
    val unlockedZoneTier: Int = 1,
    val updatedAtMs: Long = System.currentTimeMillis()
)
