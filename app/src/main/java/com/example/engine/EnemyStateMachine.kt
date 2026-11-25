package com.example.engine

import com.example.model.EnemyAiState
import com.example.model.EnemyInstance

object EnemyStateMachine {
    const val AGGRO_RANGE_TILES = 6.5f
    const val ENGAGED_CHASE_RANGE_TILES = 10.0f
    const val ATTACK_RANGE_TILES = 1.45f
    const val FLEE_SAFE_DISTANCE_TILES = 8.5f
    const val LEASH_RANGE_TILES = 14.5f
    const val FLEE_HP_THRESHOLD = 0.22f

    fun evaluateNextState(
        enemy: EnemyInstance,
        distToTarget: Float,
        distFromSpawn: Float,
        playerInSafeZone: Boolean,
        playerStealthed: Boolean,
        isTargetingSummon: Boolean
    ): EnemyAiState {
        if (!enemy.isAlive) return EnemyAiState.IDLE

        val inActiveCombat = enemy.recentlyDamagedSec > 0f
        val hasForcedDuel = enemy.forcedDuelSummonId != null

        // Forced 1v1 duel overrides safe-zone, stealth, leash, and flee checks
        if (hasForcedDuel) {
            return if (distToTarget <= ATTACK_RANGE_TILES) EnemyAiState.ATTACK else EnemyAiState.CHASE
        }

        // Disengage to IDLE if player retreated to safe zone, exceeded leash range, or vanished in stealth out of combat
        if (!isTargetingSummon && playerInSafeZone) return EnemyAiState.IDLE
        if (distFromSpawn > LEASH_RANGE_TILES) return EnemyAiState.IDLE
        if (!isTargetingSummon && playerStealthed && !inActiveCombat) return EnemyAiState.IDLE

        // Non-boss enemies flee at critical HP while threatened or actively in combat
        val hpRatio = enemy.hp.toFloat() / enemy.maxHp.coerceAtLeast(1)
        val shouldFlee = !enemy.kind.isBoss &&
            hpRatio <= FLEE_HP_THRESHOLD &&
            (inActiveCombat || enemy.aiState == EnemyAiState.FLEE || distToTarget <= AGGRO_RANGE_TILES) &&
            distToTarget <= FLEE_SAFE_DISTANCE_TILES
        if (shouldFlee) {
            return EnemyAiState.FLEE
        }

        // Melee / close-range attack
        if (distToTarget <= ATTACK_RANGE_TILES) {
            return EnemyAiState.ATTACK
        }

        // Chase if within aggro radius or recently damaged from range
        if (distToTarget <= AGGRO_RANGE_TILES || (inActiveCombat && distToTarget <= ENGAGED_CHASE_RANGE_TILES)) {
            return EnemyAiState.CHASE
        }

        return EnemyAiState.IDLE
    }
}
