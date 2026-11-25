package com.example.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.EnemyKind
import com.example.model.HeroClass
import com.example.model.Race
import com.example.model.SummonKind
import com.example.world.TileKind

/**
 * Generates crisp 16x16 pixel-art sprites cached as ImageBitmaps for FilterQuality.None rendering.
 */
object PixelSprites {

    private val tileCache = mutableMapOf<TileKind, ImageBitmap>()
    private val heroCache = mutableMapOf<String, ImageBitmap>()
    private val enemyCache = mutableMapOf<EnemyKind, ImageBitmap>()
    private val summonCache = mutableMapOf<SummonKind, ImageBitmap>()
    private val npcCache = mutableMapOf<Long, ImageBitmap>()
    private var chestClosedImg: ImageBitmap? = null
    private var chestOpenImg: ImageBitmap? = null

    private fun buildBitmap(w: Int = 16, h: Int = 16, draw: (IntArray) -> Unit): ImageBitmap {
        val pixels = IntArray(w * h)
        draw(pixels)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp.asImageBitmap()
    }

    private fun setPx(px: IntArray, x: Int, y: Int, color: Int, w: Int = 16, h: Int = 16) {
        if (x in 0 until w && y in 0 until h) {
            px[y * w + x] = color
        }
    }

    private fun fillRect(px: IntArray, x1: Int, y1: Int, x2: Int, y2: Int, color: Int, w: Int = 16) {
        for (y in y1..y2) {
            for (x in x1..x2) {
                setPx(px, x, y, color, w)
            }
        }
    }

    fun getTile(kind: TileKind): ImageBitmap = tileCache.getOrPut(kind) {
        buildBitmap { px ->
            when (kind) {
                TileKind.GRASS -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF2E5C31.toInt())
                    setPx(px, 2, 3, 0xFF3E7A42.toInt())
                    setPx(px, 3, 2, 0xFF3E7A42.toInt())
                    setPx(px, 11, 10, 0xFF3E7A42.toInt())
                    setPx(px, 12, 9, 0xFF4F9453.toInt())
                    setPx(px, 6, 13, 0xFF234725.toInt())
                }
                TileKind.COBBLE -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF475569.toInt())
                    fillRect(px, 1, 1, 6, 6, 0xFF64748B.toInt())
                    fillRect(px, 8, 1, 14, 7, 0xFF5A687D.toInt())
                    fillRect(px, 1, 8, 8, 14, 0xFF5A687D.toInt())
                    fillRect(px, 10, 9, 14, 14, 0xFF64748B.toInt())
                    setPx(px, 2, 2, 0xFF94A3B8.toInt())
                    setPx(px, 9, 2, 0xFF94A3B8.toInt())
                }
                TileKind.DIRT_ROAD -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF78573A.toInt())
                    setPx(px, 3, 4, 0xFF8C6847.toInt())
                    setPx(px, 10, 11, 0xFF63462E.toInt())
                    setPx(px, 13, 3, 0xFF8C6847.toInt())
                    setPx(px, 5, 12, 0xFF63462E.toInt())
                }
                TileKind.WOOD_BRIDGE -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF5C3A21.toInt())
                    for (y in 0..15 step 4) {
                        fillRect(px, 0, y, 15, y + 2, 0xFF855632.toInt())
                        fillRect(px, 0, y + 3, 15, y + 3, 0xFF3D2514.toInt())
                    }
                    fillRect(px, 0, 0, 1, 15, 0xFF472C18.toInt())
                    fillRect(px, 14, 0, 15, 15, 0xFF472C18.toInt())
                }
                TileKind.WATER -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF1D4ED8.toInt())
                    fillRect(px, 2, 4, 6, 4, 0xFF60A5FA.toInt())
                    fillRect(px, 9, 11, 13, 11, 0xFF60A5FA.toInt())
                    fillRect(px, 4, 13, 7, 13, 0xFF3B82F6.toInt())
                }
                TileKind.TREE -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF2E5C31.toInt())
                    fillRect(px, 6, 10, 9, 15, 0xFF5C3A21.toInt())
                    fillRect(px, 3, 3, 12, 10, 0xFF14532D.toInt())
                    fillRect(px, 4, 1, 11, 8, 0xFF166534.toInt())
                    fillRect(px, 5, 2, 9, 5, 0xFF22C55E.toInt())
                }
                TileKind.STONE_WALL -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF1E293B.toInt())
                    fillRect(px, 1, 1, 14, 6, 0xFF475569.toInt())
                    fillRect(px, 1, 8, 7, 14, 0xFF334155.toInt())
                    fillRect(px, 9, 8, 14, 14, 0xFF475569.toInt())
                    fillRect(px, 2, 2, 13, 2, 0xFF64748B.toInt())
                }
                TileKind.RUIN_FLOOR -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF2D2A3D.toInt())
                    fillRect(px, 2, 2, 7, 7, 0xFF3B3652.toInt())
                    fillRect(px, 9, 8, 14, 14, 0xFF3B3652.toInt())
                    setPx(px, 4, 12, 0xFF4B5563.toInt())
                }
                TileKind.SWAMP_MUD -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF273B2C.toInt())
                    setPx(px, 4, 5, 0xFF36533D.toInt())
                    setPx(px, 11, 9, 0xFF1E2E22.toInt())
                    setPx(px, 8, 13, 0xFF4D7C0F.toInt())
                }
                TileKind.SWAMP_BOG -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF1A2E1A.toInt())
                    fillRect(px, 3, 4, 12, 11, 0xFF365314.toInt())
                    setPx(px, 6, 7, 0xFF84CC16.toInt())
                    setPx(px, 10, 6, 0xFF84CC16.toInt())
                }
                TileKind.VOLCANIC_ROCK -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF272126.toInt())
                    setPx(px, 3, 3, 0xFF3F343D.toInt())
                    setPx(px, 11, 10, 0xFF181418.toInt())
                    setPx(px, 7, 13, 0xFF522A2A.toInt())
                }
                TileKind.LAVA -> {
                    fillRect(px, 0, 0, 15, 15, 0xFFDC2626.toInt())
                    fillRect(px, 2, 3, 9, 6, 0xFFF97316.toInt())
                    fillRect(px, 6, 10, 13, 13, 0xFFFACC15.toInt())
                }
                TileKind.HOUSE_WALL -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF78350F.toInt())
                    fillRect(px, 1, 1, 14, 7, 0xFF991B1B.toInt())
                    fillRect(px, 1, 8, 14, 14, 0xFFD6D3D1.toInt())
                    fillRect(px, 6, 10, 9, 14, 0xFF451A03.toInt())
                    fillRect(px, 2, 10, 4, 12, 0xFFFACC15.toInt())
                }
                TileKind.HEAL_FOUNTAIN -> {
                    fillRect(px, 0, 0, 15, 15, 0xFF475569.toInt())
                    fillRect(px, 2, 2, 13, 13, 0xFF94A3B8.toInt())
                    fillRect(px, 4, 4, 11, 11, 0xFF38BDF8.toInt())
                    fillRect(px, 6, 6, 9, 9, 0xFF10B981.toInt())
                }
            }
        }
    }

    fun getHero(race: Race, heroClass: HeroClass): ImageBitmap {
        val key = "${race.name}_${heroClass.name}"
        return heroCache.getOrPut(key) {
            buildBitmap { px ->
                val outline = 0xFF110E1A.toInt()
                val skin = (0xFF000000L or (race.skinColorHex and 0xFFFFFFL)).toInt()
                val hair = (0xFF000000L or (race.hairColorHex and 0xFFFFFFL)).toInt()
                val armor = (0xFF000000L or (heroClass.accentHex and 0xFFFFFFL)).toInt()
                val darkArmor = 0xFF334155.toInt()
                val steel = 0xFFE2E8F0.toInt()

                // Head outline & fill
                fillRect(px, 5, 1, 10, 6, outline)
                fillRect(px, 6, 2, 9, 5, skin)
                // Hair / Helm top
                fillRect(px, 5, 1, 10, 2, if (heroClass.armorWeight.name == "PLATE") steel else hair)
                // Race distinctive pixels: Elf pointed ears, Orc tusks, Dwarf beard
                when (race) {
                    Race.ELF -> {
                        setPx(px, 4, 3, skin)
                        setPx(px, 11, 3, skin)
                    }
                    Race.ORC -> {
                        setPx(px, 6, 5, 0xFFFFFFFF.toInt())
                        setPx(px, 9, 5, 0xFFFFFFFF.toInt())
                    }
                    Race.DWARF -> {
                        fillRect(px, 5, 5, 10, 7, hair)
                    }
                    Race.HUMAN -> {}
                }
                // Eyes
                setPx(px, 6, 3, outline)
                setPx(px, 9, 3, outline)

                // Torso
                fillRect(px, 4, 7, 11, 11, outline)
                fillRect(px, 5, 7, 10, 11, armor)
                fillRect(px, 6, 8, 9, 10, darkArmor)

                // Legs
                fillRect(px, 5, 12, 7, 14, darkArmor)
                fillRect(px, 8, 12, 10, 14, darkArmor)

                // Weapon / Offhand pixel silhouette by class
                if (heroClass.isRanged) {
                    // Staff or Bow on right side
                    fillRect(px, 12, 4, 13, 12, 0xFF92400E.toInt())
                    setPx(px, 12, 3, armor)
                    setPx(px, 13, 3, 0xFFFFFFFF.toInt())
                } else {
                    // Sword/Dagger
                    fillRect(px, 12, 3, 13, 9, steel)
                    fillRect(px, 11, 9, 14, 10, 0xFFFACC15.toInt())
                    // Shield for Paladin / Warrior
                    if (heroClass == HeroClass.PALADIN || heroClass == HeroClass.WARRIOR) {
                        fillRect(px, 2, 7, 4, 11, armor)
                        setPx(px, 3, 9, steel)
                    }
                }
            }
        }
    }

    fun getEnemy(kind: EnemyKind): ImageBitmap = enemyCache.getOrPut(kind) {
        buildBitmap { px ->
            val outline = 0xFF0F0B14.toInt()
            val body = (0xFF000000L or (kind.bodyColorHex and 0xFFFFFFL)).toInt()
            val accent = (0xFF000000L or (kind.accentHex and 0xFFFFFFL)).toInt()

            when (kind) {
                EnemyKind.WOLF, EnemyKind.BOAR, EnemyKind.BOSS_ALPHA_WOLF, EnemyKind.BOSS_IRON_BOAR -> {
                    // Quadruped beast silhouette (with crown if boss)
                    fillRect(px, 2, 6, 13, 11, outline)
                    fillRect(px, 3, 7, 12, 10, body)
                    fillRect(px, 10, 5, 14, 8, body)
                    setPx(px, 11, 4, accent) // ears
                    setPx(px, 13, 6, 0xFFEF4444.toInt()) // red eye
                    fillRect(px, 3, 11, 4, 14, body)
                    fillRect(px, 10, 11, 11, 14, body)
                    if (kind.isBoss) {
                        fillRect(px, 5, 4, 10, 6, accent) // spiked mane
                        fillRect(px, 9, 2, 13, 4, 0xFFFACC15.toInt()) // Boss crown
                    }
                }
                EnemyKind.DRAGON_BOSS, EnemyKind.BOSS_ASH_WYVERN, EnemyKind.BOSS_HYDRA_SPAWN -> {
                    // Imposing Wyrm / Hydra sprite
                    fillRect(px, 1, 3, 4, 10, accent) // left wing / head
                    fillRect(px, 11, 3, 14, 10, accent) // right wing / head
                    fillRect(px, 4, 2, 11, 13, outline)
                    fillRect(px, 5, 3, 10, 12, body)
                    fillRect(px, 6, 1, 9, 4, accent) // horns / crest
                    setPx(px, 6, 4, 0xFFFACC15.toInt())
                    setPx(px, 9, 4, 0xFFFACC15.toInt())
                    fillRect(px, 6, 8, 9, 12, accent)
                }
                else -> {
                    // Humanoid / Monster silhouette
                    fillRect(px, 4, 2, 11, 6, outline)
                    fillRect(px, 5, 3, 10, 5, body)
                    setPx(px, 6, 4, accent)
                    setPx(px, 9, 4, accent)
                    fillRect(px, 3, 7, 12, 12, outline)
                    fillRect(px, 4, 7, 11, 11, body)
                    fillRect(px, 6, 8, 9, 10, accent)
                    fillRect(px, 5, 12, 7, 14, body)
                    fillRect(px, 8, 12, 10, 14, body)
                    if (kind.isBoss) {
                        fillRect(px, 2, 7, 3, 9, accent) // left pauldron
                        fillRect(px, 12, 7, 13, 9, accent) // right pauldron
                        fillRect(px, 5, 0, 10, 2, 0xFFFACC15.toInt()) // Boss crown
                    }
                }
            }
        }
    }

    fun getSummon(kind: SummonKind): ImageBitmap = summonCache.getOrPut(kind) {
        buildBitmap { px ->
            val col = (0xFF000000L or (kind.colorHex and 0xFFFFFFL)).toInt()
            val glow = 0xFFFFFFFF.toInt()
            when (kind) {
                SummonKind.TANK_BEAST -> {
                    fillRect(px, 2, 6, 13, 11, 0xFF78350F.toInt())
                    fillRect(px, 3, 7, 12, 10, col)
                    fillRect(px, 10, 5, 14, 9, col)
                    setPx(px, 13, 6, glow)
                    fillRect(px, 3, 11, 5, 14, col)
                    fillRect(px, 10, 11, 12, 14, col)
                }
                SummonKind.HEALING_TOTEM, SummonKind.STORM_TOTEM -> {
                    fillRect(px, 5, 3, 10, 14, 0xFF451A03.toInt())
                    fillRect(px, 6, 4, 9, 13, col)
                    fillRect(px, 4, 5, 11, 6, glow)
                }
                SummonKind.SNARE_TRAP -> {
                    fillRect(px, 3, 10, 12, 13, 0xFF475569.toInt())
                    setPx(px, 4, 9, col)
                    setPx(px, 7, 9, col)
                    setPx(px, 10, 9, col)
                }
                else -> {
                    fillRect(px, 5, 2, 10, 12, col)
                    fillRect(px, 6, 4, 9, 6, glow)
                    setPx(px, 4, 8, col)
                    setPx(px, 11, 8, col)
                }
            }
        }
    }

    fun getNpc(tunicColorHex: Long): ImageBitmap = npcCache.getOrPut(tunicColorHex) {
        buildBitmap { px ->
            val tunic = (0xFF000000L or (tunicColorHex and 0xFFFFFFL)).toInt()
            fillRect(px, 5, 2, 10, 6, 0xFFF1C27D.toInt())
            fillRect(px, 5, 1, 10, 2, 0xFF5C3A21.toInt())
            setPx(px, 6, 4, 0xFF0F172A.toInt())
            setPx(px, 9, 4, 0xFF0F172A.toInt())
            fillRect(px, 4, 7, 11, 12, tunic)
            fillRect(px, 5, 12, 7, 15, 0xFF334155.toInt())
            fillRect(px, 8, 12, 10, 15, 0xFF334155.toInt())
        }
    }

    fun getChest(opened: Boolean): ImageBitmap {
        if (!opened) {
            return chestClosedImg ?: buildBitmap { px ->
                fillRect(px, 2, 5, 13, 13, 0xFF78350F.toInt())
                fillRect(px, 3, 6, 12, 8, 0xFFB45309.toInt())
                fillRect(px, 2, 8, 13, 9, 0xFFFACC15.toInt())
                fillRect(px, 7, 8, 8, 11, 0xFFFACC15.toInt())
            }.also { chestClosedImg = it }
        }
        return chestOpenImg ?: buildBitmap { px ->
            fillRect(px, 2, 8, 13, 13, 0xFF78350F.toInt())
            fillRect(px, 3, 9, 12, 11, 0xFF1E293B.toInt())
            fillRect(px, 2, 5, 13, 7, 0xFFB45309.toInt())
        }.also { chestOpenImg = it }
    }
}
