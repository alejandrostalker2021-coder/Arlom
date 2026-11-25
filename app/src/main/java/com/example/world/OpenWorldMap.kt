package com.example.world

import com.example.model.EnemyInstance
import com.example.model.EnemyKind
import com.example.model.ItemRarity
import com.example.model.WorldChest
import com.example.model.ZoneId
import kotlin.math.abs

enum class TileKind(val walkable: Boolean) {
    GRASS(true),
    COBBLE(true),
    DIRT_ROAD(true),
    WOOD_BRIDGE(true),
    WATER(false),
    TREE(false),
    STONE_WALL(false),
    RUIN_FLOOR(true),
    SWAMP_MUD(true),
    SWAMP_BOG(false),
    VOLCANIC_ROCK(true),
    LAVA(false),
    HOUSE_WALL(false),
    HEAL_FOUNTAIN(true)
}

object OpenWorldMap {
    const val WIDTH = 72
    const val HEIGHT = 72

    private val grid: Array<Array<TileKind>> = Array(HEIGHT) { Array(WIDTH) { TileKind.GRASS } }

    private val zoneBossSpawns: List<Triple<EnemyKind, Float, Float>> = listOf(
        // Bosque Esmeralda (5 bosses)
        Triple(EnemyKind.BOSS_ALPHA_WOLF, 27f, 6f),
        Triple(EnemyKind.BOSS_IRON_BOAR, 42f, 8f),
        Triple(EnemyKind.BOSS_THORN_ENT, 30f, 19f),
        Triple(EnemyKind.BOSS_BANDIT_CAPTAIN, 43f, 22f),
        Triple(EnemyKind.BOSS_FOREST_STALKER, 28f, 30f),
        // Ruinas del Crepúsculo (5 bosses)
        Triple(EnemyKind.BOSS_BONE_WARLORD, 53f, 6f),
        Triple(EnemyKind.BOSS_RUNE_GOLEM, 65f, 7f),
        Triple(EnemyKind.BOSS_GOBLIN_PYROMANCER, 55f, 18f),
        Triple(EnemyKind.BOSS_CRYPT_HUNTER, 66f, 21f),
        Triple(EnemyKind.FALLEN_KNIGHT, 64f, 29f),
        // Pantano Maldito (5 bosses)
        Triple(EnemyKind.BOSS_PLAGUE_MATRON, 10f, 39f),
        Triple(EnemyKind.BOSS_MIRE_BEHEMOTH, 26f, 39f),
        Triple(EnemyKind.BOSS_HYDRA_SPAWN, 10f, 54f),
        Triple(EnemyKind.BOSS_DREAD_REAPER, 30f, 57f),
        Triple(EnemyKind.BOSS_BOG_KING, 18f, 65f),
        // Cumbre del Dragón (5 bosses)
        Triple(EnemyKind.BOSS_PYRO_DRAKONID, 50f, 40f),
        Triple(EnemyKind.BOSS_MAGMA_COLOSSUS, 65f, 41f),
        Triple(EnemyKind.BOSS_ASH_WYVERN, 51f, 54f),
        Triple(EnemyKind.BOSS_HELLFIRE_LICH, 66f, 54f),
        Triple(EnemyKind.DRAGON_BOSS, 60f, 60f)
    )

    init {
        buildWorld()
    }

    private fun buildWorld() {
        // 1. Base biomes by region
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                grid[y][x] = when {
                    x < 22 && y < 24 -> TileKind.GRASS // Valoria Village area
                    x in 22 until 48 && y < 34 -> TileKind.GRASS // Emerald Forest
                    x >= 48 && y < 36 -> TileKind.RUIN_FLOOR // Twilight Ruins
                    x < 44 && y >= 34 -> TileKind.SWAMP_MUD // Cursed Swamp
                    else -> TileKind.VOLCANIC_ROCK // Dragon Summit
                }
            }
        }

        // 2. Outer world borders
        for (x in 0 until WIDTH) {
            grid[0][x] = TileKind.STONE_WALL
            grid[HEIGHT - 1][x] = TileKind.STONE_WALL
        }
        for (y in 0 until HEIGHT) {
            grid[y][0] = TileKind.STONE_WALL
            grid[y][WIDTH - 1] = TileKind.STONE_WALL
        }

        // 3. Aldea de Valoria (x: 4..20, y: 4..20)
        for (y in 6..18) {
            for (x in 6..18) {
                grid[y][x] = TileKind.COBBLE
            }
        }
        // Village houses (2x2 / 3x2 blocks)
        placeRect(5, 5, 8, 7, TileKind.HOUSE_WALL)
        placeRect(15, 5, 18, 7, TileKind.HOUSE_WALL)
        placeRect(5, 16, 8, 18, TileKind.HOUSE_WALL)
        placeRect(15, 16, 18, 18, TileKind.HOUSE_WALL)
        // Healing fountain near Sacerdotisa Elara (x=11, y=15)
        grid[14][11] = TileKind.HEAL_FOUNTAIN

        // River separating Valoria & Emerald Forest / Swamp (x = 22)
        for (y in 1 until HEIGHT - 1) {
            grid[y][22] = TileKind.WATER
            if (y % 5 == 0) grid[y][21] = TileKind.WATER
        }
        // Main East Bridge out of Valoria (y = 11..12)
        for (y in 11..12) {
            grid[y][21] = TileKind.WOOD_BRIDGE
            grid[y][22] = TileKind.WOOD_BRIDGE
            grid[y][23] = TileKind.DIRT_ROAD
        }
        // South Bridge to Swamp (x=22, y=42..43)
        for (y in 42..43) {
            grid[y][21] = TileKind.WOOD_BRIDGE
            grid[y][22] = TileKind.WOOD_BRIDGE
        }

        // 4. Dirt Roads connecting all 5 zones clearly
        // Valoria -> Emerald Forest -> Twilight Ruins
        for (x in 18..56) {
            grid[11][x] = if (x == 21 || x == 22) TileKind.WOOD_BRIDGE else TileKind.DIRT_ROAD
            grid[12][x] = if (x == 21 || x == 22) TileKind.WOOD_BRIDGE else TileKind.DIRT_ROAD
        }
        // Forest North-South road down to Swamp
        for (y in 12..46) {
            grid[y][34] = TileKind.DIRT_ROAD
            grid[y][35] = TileKind.DIRT_ROAD
        }
        // Swamp Eastward road to Dragon Summit
        for (x in 24..60) {
            grid[46][x] = TileKind.DIRT_ROAD
            grid[47][x] = TileKind.DIRT_ROAD
        }
        // Dragon Summit ascent road
        for (y in 46..60) {
            grid[y][58] = TileKind.DIRT_ROAD
        }

        // 5. Emerald Forest Trees (organic clusters, keeping roads open)
        for (y in 3 until 33) {
            for (x in 24 until 47) {
                if (abs(y - 11) > 2 && abs(x - 34) > 2) {
                    if ((x * 17 + y * 31) % 11 == 0) {
                        grid[y][x] = TileKind.TREE
                    }
                }
            }
        }
        // Trees around Valoria outskirts
        for (y in 2 until 22) {
            for (x in 2 until 20) {
                if (x < 5 || x > 19 || y < 5 || y > 19) {
                    if ((x * 7 + y * 13) % 6 == 0) grid[y][x] = TileKind.TREE
                }
            }
        }

        // 6. Twilight Ruins Stone Pillars & Walls (x: 49..69, y: 3..33)
        for (y in 5 until 31 step 6) {
            for (x in 51 until 68 step 6) {
                if (abs(y - 11) > 2) {
                    grid[y][x] = TileKind.STONE_WALL
                    grid[y][x + 1] = TileKind.STONE_WALL
                }
            }
        }

        // 7. Cursed Swamp Bogs (x: 3..42, y: 36..69)
        for (y in 36 until 69) {
            for (x in 3 until 42) {
                if (abs(y - 46) > 2 && abs(x - 34) > 2 && abs(x - 22) > 2) {
                    if ((x * 23 + y * 19) % 13 == 0) {
                        grid[y][x] = TileKind.SWAMP_BOG
                    }
                }
            }
        }

        // 8. Dragon Summit Lava Pools (x: 46..69, y: 38..69)
        for (y in 38 until 69) {
            for (x in 46 until 69) {
                if (abs(y - 46) > 2 && abs(x - 58) > 3 && !(x in 55..63 && y in 55..63)) {
                    if ((x * 29 + y * 17) % 14 == 0) {
                        grid[y][x] = TileKind.LAVA
                    }
                }
            }
        }

        // 9. Ensure 3x3 walkable dodge arenas around all 20 Zone Boss spawns
        zoneBossSpawns.forEach { (kind, bx, by) ->
            val cx = bx.toInt()
            val cy = by.toInt()
            val floorTile = when (kind.zone) {
                ZoneId.VALORIA_VILLAGE -> TileKind.COBBLE
                ZoneId.EMERALD_FOREST -> TileKind.GRASS
                ZoneId.TWILIGHT_RUINS -> TileKind.RUIN_FLOOR
                ZoneId.CURSED_SWAMP -> TileKind.SWAMP_MUD
                ZoneId.DRAGON_SUMMIT -> TileKind.VOLCANIC_ROCK
            }
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val tx = cx + dx
                    val ty = cy + dy
                    if (tx in 2 until WIDTH - 2 && ty in 2 until HEIGHT - 2 && !grid[ty][tx].walkable) {
                        grid[ty][tx] = floorTile
                    }
                }
            }
        }
    }

    private fun placeRect(x1: Int, y1: Int, x2: Int, y2: Int, kind: TileKind) {
        for (y in y1..y2) {
            for (x in x1..x2) {
                if (x in 0 until WIDTH && y in 0 until HEIGHT) {
                    grid[y][x] = kind
                }
            }
        }
    }

    fun getTile(tx: Int, ty: Int): TileKind {
        if (tx !in 0 until WIDTH || ty !in 0 until HEIGHT) return TileKind.STONE_WALL
        return grid[ty][tx]
    }

    fun isWalkable(x: Float, y: Float): Boolean {
        val tx = x.toInt()
        val ty = y.toInt()
        if (tx !in 1 until WIDTH - 1 || ty !in 1 until HEIGHT - 1) return false
        return grid[ty][tx].walkable
    }

    fun zoneAt(x: Float, y: Float): ZoneId {
        return when {
            x < 22f && y < 24f -> ZoneId.VALORIA_VILLAGE
            x in 22f..48f && y < 34f -> ZoneId.EMERALD_FOREST
            x > 48f && y < 36f -> ZoneId.TWILIGHT_RUINS
            x < 44f && y >= 34f -> ZoneId.CURSED_SWAMP
            else -> ZoneId.DRAGON_SUMMIT
        }
    }

    fun initialEnemies(): List<EnemyInstance> {
        val list = mutableListOf<EnemyInstance>()
        var idx = 1
        fun add(kind: EnemyKind, x: Float, y: Float) {
            list += EnemyInstance(
                id = "en_${idx++}",
                kind = kind,
                spawnX = x,
                spawnY = y,
                x = x,
                y = y,
                hp = kind.baseHp,
                maxHp = kind.baseHp,
                scaledXpReward = com.example.engine.ExperienceSystem.calculateKillXp(
                    mobLevel = kind.baseLevel,
                    isBoss = kind.isBoss
                )
            )
        }

        // Bosque Esmeralda (Levels 1-7)
        val wolfCoords = listOf(26f to 8f, 29f to 14f, 32f to 7f, 27f to 18f, 36f to 9f, 39f to 15f)
        wolfCoords.forEach { (x, y) -> add(EnemyKind.WOLF, x, y) }

        val boarCoords = listOf(31f to 22f, 38f to 21f, 43f to 14f, 41f to 25f)
        boarCoords.forEach { (x, y) -> add(EnemyKind.BOAR, x, y) }

        val banditCoords = listOf(30f to 28f, 37f to 29f, 43f to 28f, 45f to 20f)
        banditCoords.forEach { (x, y) -> add(EnemyKind.BANDIT, x, y) }

        // Ruinas del Crepúsculo (Levels 8-15)
        val skelCoords = listOf(52f to 8f, 56f to 14f, 60f to 9f, 54f to 20f, 63f to 18f)
        skelCoords.forEach { (x, y) -> add(EnemyKind.SKELETON, x, y) }

        val marauderCoords = listOf(51f to 26f, 58f to 26f, 64f to 24f, 66f to 12f)
        marauderCoords.forEach { (x, y) -> add(EnemyKind.MARAUDER, x, y) }

        // Pantano Maldito (Levels 16-23)
        val witchCoords = listOf(12f to 40f, 18f to 48f, 26f to 42f, 30f to 52f)
        witchCoords.forEach { (x, y) -> add(EnemyKind.SWAMP_WITCH, x, y) }

        val trollCoords = listOf(15f to 56f, 24f to 55f, 32f to 60f, 38f to 50f)
        trollCoords.forEach { (x, y) -> add(EnemyKind.TROLL, x, y) }

        val specterCoords = listOf(8f to 62f, 20f to 64f, 36f to 64f)
        specterCoords.forEach { (x, y) -> add(EnemyKind.SPECTER, x, y) }

        // Cumbre del Dragón (Levels 24-30)
        val drakonidCoords = listOf(50f to 42f, 55f to 44f, 62f to 42f, 52f to 52f, 64f to 50f)
        drakonidCoords.forEach { (x, y) -> add(EnemyKind.DRAKONID, x, y) }

        val golemCoords = listOf(49f to 58f, 55f to 62f, 65f to 58f)
        golemCoords.forEach { (x, y) -> add(EnemyKind.OBSIDIAN_GOLEM, x, y) }

        // 20 Varied Zone Bosses (5 per Combat Zone)
        zoneBossSpawns.forEach { (bossKind, bx, by) ->
            add(bossKind, bx, by)
        }

        return list
    }

    fun initialChests(): List<WorldChest> = listOf(
        WorldChest("ch_forest_1", 28f, 5f, ZoneId.EMERALD_FOREST, ItemRarity.RARE),
        WorldChest("ch_forest_2", 44f, 26f, ZoneId.EMERALD_FOREST, ItemRarity.RARE),
        WorldChest("ch_ruins_1", 66f, 7f, ZoneId.TWILIGHT_RUINS, ItemRarity.EPIC),
        WorldChest("ch_swamp_1", 10f, 64f, ZoneId.CURSED_SWAMP, ItemRarity.EPIC),
        WorldChest("ch_dragon_1", 66f, 64f, ZoneId.DRAGON_SUMMIT, ItemRarity.LEGENDARY)
    )
}
