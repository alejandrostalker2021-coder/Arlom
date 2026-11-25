package com.example.engine

import com.example.model.BossTactic
import com.example.model.BossTelegraph
import com.example.model.EnemyInstance
import com.example.model.EnemyKind
import com.example.model.MAX_PLAYER_LEVEL
import com.example.model.ZoneId
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

data class ScaledBossStats(
    val effectiveLevel: Int,
    val maxHp: Int,
    val attack: Int,
    val xpReward: Int,
    val goldReward: Int,
    val windupSec: Float,
    val telegraphCooldownSec: Float,
    val meleeCooldownSec: Float
)

object BossTactics {

    const val LINEAR_CHARGE_HALF_WIDTH = 0.85f
    const val SHOCKWAVE_CONE_COS_HALF_ANGLE = 0.788f // ~38 deg half-angle (~76 deg cone)

    fun allZoneBosses(): List<EnemyKind> = EnemyKind.entries.filter { it.isBoss }

    fun bossesForZone(zone: ZoneId): List<EnemyKind> = EnemyKind.entries.filter { it.isBoss && it.zone == zone }

    fun windupSecForLevel(tactic: BossTactic, effectiveLevel: Int): Float {
        val clampedLv = effectiveLevel.coerceIn(1, MAX_PLAYER_LEVEL)
        val difficultyProgress = ((clampedLv - 1).toFloat() / (MAX_PLAYER_LEVEL - 1).toFloat()).coerceIn(0f, 1f)
        return (tactic.windupSec * (1.08f - 0.24f * difficultyProgress)).coerceIn(0.86f, 1.45f)
    }

    fun scaleBossStats(kind: EnemyKind, playerLevel: Int): ScaledBossStats {
        if (!kind.isBoss) {
            return ScaledBossStats(
                effectiveLevel = kind.baseLevel,
                maxHp = kind.baseHp,
                attack = kind.baseAttack,
                xpReward = ExperienceSystem.calculateKillXp(mobLevel = kind.baseLevel, isBoss = false),
                goldReward = kind.goldReward,
                windupSec = 1.25f,
                telegraphCooldownSec = 4.6f,
                meleeCooldownSec = 1.35f
            )
        }
        val clampedPlayer = playerLevel.coerceIn(1, MAX_PLAYER_LEVEL)
        val effectiveLevel = max(
            kind.baseLevel,
            (clampedPlayer + kind.bossDifficultyOffset).coerceIn(1, MAX_PLAYER_LEVEL)
        )
        val lvOffset = (effectiveLevel - 1).toFloat()
        val universalHp = (185f + lvOffset * 62f + lvOffset * lvOffset * 1.45f) * kind.bossHpMult
        val universalAtk = (16f + lvOffset * 3.15f) * kind.bossAtkMult

        val maxHp = max(kind.baseHp, universalHp.roundToInt())
        val attack = max(kind.baseAttack, universalAtk.roundToInt())

        val bonusLevels = (effectiveLevel - kind.baseLevel).coerceAtLeast(0)
        val xpReward = ExperienceSystem.calculateKillXp(mobLevel = effectiveLevel, isBoss = true)
        val goldReward = max(kind.goldReward, (kind.goldReward * (1f + bonusLevels * 0.12f)).roundToInt())

        val difficultyProgress = (lvOffset / (MAX_PLAYER_LEVEL - 1).toFloat()).coerceIn(0f, 1f)
        val windupSec = kind.bossTactic?.let { windupSecForLevel(it, effectiveLevel) } ?: 1.25f
        val telegraphCooldownSec = (5.2f - 1.5f * difficultyProgress).coerceIn(3.5f, 5.2f)
        val meleeCooldownSec = (1.42f - 0.28f * difficultyProgress).coerceIn(1.10f, 1.45f)

        return ScaledBossStats(
            effectiveLevel = effectiveLevel,
            maxHp = maxHp,
            attack = attack,
            xpReward = xpReward,
            goldReward = goldReward,
            windupSec = windupSec,
            telegraphCooldownSec = telegraphCooldownSec,
            meleeCooldownSec = meleeCooldownSec
        )
    }

    fun applyBossScaling(enemy: EnemyInstance, playerLevel: Int): EnemyInstance {
        if (!enemy.kind.isBoss) return enemy
        val scaled = scaleBossStats(enemy.kind, playerLevel)
        val nextHp = when {
            !enemy.isAlive -> 0
            enemy.hp >= enemy.maxHp -> scaled.maxHp
            else -> ((enemy.hp.toFloat() / enemy.maxHp.coerceAtLeast(1)) * scaled.maxHp)
                .roundToInt()
                .coerceIn(1, scaled.maxHp)
        }
        return enemy.copy(
            hp = nextHp,
            maxHp = scaled.maxHp,
            effectiveLevel = scaled.effectiveLevel,
            scaledAttack = scaled.attack,
            scaledXpReward = scaled.xpReward,
            scaledGoldReward = scaled.goldReward
        )
    }

    fun createTelegraph(
        id: Long,
        boss: EnemyInstance,
        targetX: Float,
        targetY: Float
    ): BossTelegraph? {
        val tactic = boss.kind.bossTactic ?: return null
        val dx = targetX - boss.x
        val dy = targetY - boss.y
        val dist = hypot(dx, dy)
        val dirX = if (dist > 0.001f) dx / dist else 1f
        val dirY = if (dist > 0.001f) dy / dist else 0f
        val weakMult = if (boss.cursedWeakSec > 0f) 0.75f else 1f
        val baseAtk = max(boss.kind.baseAttack, boss.scaledAttack)
        val dmg = (baseAtk * tactic.damageMult * weakMult).roundToInt().coerceAtLeast(baseAtk)
        val windupSec = windupSecForLevel(tactic, boss.effectiveLevel)

        return BossTelegraph(
            id = id,
            bossId = boss.id,
            bossName = boss.kind.displayName,
            tactic = tactic,
            originX = boss.x,
            originY = boss.y,
            targetX = targetX,
            targetY = targetY,
            dirX = dirX,
            dirY = dirY,
            radiusTiles = tactic.rangeTiles,
            totalWindupSec = windupSec,
            remainingSec = windupSec,
            damage = dmg,
            colorHex = boss.kind.accentHex
        )
    }

    fun isPointHitByTelegraph(telegraph: BossTelegraph, px: Float, py: Float): Boolean {
        return when (telegraph.tactic) {
            BossTactic.RADIAL_SLAM -> {
                hypot(px - telegraph.originX, py - telegraph.originY) <= telegraph.radiusTiles
            }
            BossTactic.METEOR_ZONE -> {
                hypot(px - telegraph.targetX, py - telegraph.targetY) <= telegraph.radiusTiles
            }
            BossTactic.LINEAR_CHARGE -> {
                val relX = px - telegraph.originX
                val relY = py - telegraph.originY
                val along = relX * telegraph.dirX + relY * telegraph.dirY
                val perp = abs(relX * (-telegraph.dirY) + relY * telegraph.dirX)
                along in -0.35f..telegraph.radiusTiles && perp <= LINEAR_CHARGE_HALF_WIDTH
            }
            BossTactic.SHOCKWAVE_CONE -> {
                val relX = px - telegraph.originX
                val relY = py - telegraph.originY
                val dist = hypot(relX, relY)
                if (dist > telegraph.radiusTiles) return false
                if (dist <= 0.6f) return true
                val cosAngle = (relX / dist) * telegraph.dirX + (relY / dist) * telegraph.dirY
                cosAngle >= SHOCKWAVE_CONE_COS_HALF_ANGLE
            }
        }
    }
}
