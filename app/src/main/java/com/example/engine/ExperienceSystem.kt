package com.example.engine

import com.example.model.EnemyInstance
import com.example.model.LootAndQuestData
import com.example.model.MAX_PLAYER_LEVEL
import kotlin.math.roundToInt

data class XpProgressionResult(
    val newLevel: Int,
    val newXp: Int,
    val levelsGained: Int,
    val xpGained: Int
)

object ExperienceSystem {

    const val BOSS_XP_MULTIPLIER = 4

    /**
     * Base experience granted by defeating a regular mob of [mobLevel] (1..30).
     * Strictly increases with the mob's level.
     */
    fun baseXpForMobLevel(mobLevel: Int): Int {
        val lv = mobLevel.coerceIn(1, MAX_PLAYER_LEVEL)
        return (22f + lv * 9.5f + lv * lv * 0.36f).roundToInt()
    }

    /**
     * Calculates experience earned from defeating an enemy of [mobLevel].
     * Bosses grant 4x the experience of a regular mob of the same level.
     */
    fun calculateKillXp(
        mobLevel: Int,
        isBoss: Boolean,
        xpBonusPct: Float = 0f
    ): Int {
        val baseXp = baseXpForMobLevel(mobLevel)
        val tierXp = if (isBoss) baseXp * BOSS_XP_MULTIPLIER else baseXp
        return (tierXp * (1f + xpBonusPct.coerceAtLeast(0f))).roundToInt().coerceAtLeast(1)
    }

    fun calculateEnemyKillXp(enemy: EnemyInstance, xpBonusPct: Float = 0f): Int {
        return calculateKillXp(
            mobLevel = enemy.effectiveLevel,
            isBoss = enemy.kind.isBoss,
            xpBonusPct = xpBonusPct
        )
    }

    fun applyXpGain(
        currentLevel: Int,
        currentXp: Int,
        xpGained: Int
    ): XpProgressionResult {
        var nextLevel = currentLevel.coerceIn(1, MAX_PLAYER_LEVEL)
        var nextXp = (currentXp + xpGained.coerceAtLeast(0)).coerceAtLeast(0)
        var gainedLevels = 0
        while (nextLevel < MAX_PLAYER_LEVEL && nextXp >= LootAndQuestData.xpToNextLevel(nextLevel)) {
            nextXp -= LootAndQuestData.xpToNextLevel(nextLevel)
            nextLevel++
            gainedLevels++
        }
        if (nextLevel >= MAX_PLAYER_LEVEL) nextXp = 0
        return XpProgressionResult(
            newLevel = nextLevel,
            newXp = nextXp,
            levelsGained = gainedLevels,
            xpGained = xpGained
        )
    }
}
