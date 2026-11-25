package com.example

import com.example.model.*
import com.example.world.OpenWorldMap
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun allRacesAndClassesConfigured() {
        assertEquals(4, Race.entries.size)
        assertEquals(11, HeroClass.entries.size)
        HeroClass.entries.forEach { cls ->
            val skills = LootAndQuestData.skillsForClass(cls)
            assertEquals("Class ${cls.name} must have 4 skills", 4, skills.size)
        }
    }

    @Test
    fun knightResistanceBetweenPaladinAndWarrior() {
        assertTrue(HeroClass.KNIGHT.baseArmor > HeroClass.PALADIN.baseArmor)
        assertTrue(HeroClass.KNIGHT.baseArmor < HeroClass.WARRIOR.baseArmor)
    }

    @Test
    fun uniqueSetAndLegendaryAllowPlusSixLevelEquip() {
        val setItem = LootAndQuestData.generateEquipment(
            targetLevel = 16,
            rarity = ItemRarity.UNIQUE_SET,
            heroClass = HeroClass.PALADIN
        )
        assertTrue(setItem.canEquipAtLevel(10))
        assertFalse(setItem.canEquipAtLevel(9))

        val legItem = LootAndQuestData.generateEquipment(
            targetLevel = 26,
            rarity = ItemRarity.LEGENDARY,
            heroClass = HeroClass.WARRIOR
        )
        assertTrue(legItem.canEquipAtLevel(20))
        assertFalse(legItem.canEquipAtLevel(19))
    }

    @Test
    fun lootRarityDistributionRespectsCaps() {
        val sample = 10_000
        var rare = 0
        var epic = 0
        var legOrSet = 0
        repeat(sample) {
            when (LootAndQuestData.rollLootRarity()) {
                ItemRarity.RARE -> rare++
                ItemRarity.EPIC -> epic++
                ItemRarity.LEGENDARY, ItemRarity.UNIQUE_SET -> legOrSet++
                ItemRarity.COMMON -> {}
            }
        }
        val rarePct = rare.toDouble() / sample
        val epicPct = epic.toDouble() / sample
        val topPct = legOrSet.toDouble() / sample

        assertTrue("Rare pct $rarePct should be ~0.40", rarePct in 0.37..0.43)
        assertTrue("Epic pct $epicPct should be ~0.20", epicPct in 0.17..0.23)
        assertTrue("Legendary+Set pct $topPct should be ~0.08", topPct in 0.06..0.10)
    }

    @Test
    fun openWorldHasWalkableVillageAndBoss() {
        assertTrue(OpenWorldMap.isWalkable(11f, 11f))
        assertEquals(ZoneId.VALORIA_VILLAGE, OpenWorldMap.zoneAt(11f, 11f))
        assertEquals(ZoneId.DRAGON_SUMMIT, OpenWorldMap.zoneAt(60f, 60f))
        assertEquals(MAX_PLAYER_LEVEL, 30)
    }

    @Test
    fun inventoryEntityAndConvertersMapping() {
        val conv = com.example.data.GameTypeConverters()
        assertEquals(EquipSlot.ARMOR, conv.toEquipSlot(conv.fromEquipSlot(EquipSlot.ARMOR)))
        assertEquals(ItemRarity.UNIQUE_SET, conv.toItemRarity(conv.fromItemRarity(ItemRarity.UNIQUE_SET)))

        val weapon = LootAndQuestData.generateEquipment(
            targetLevel = 12,
            rarity = ItemRarity.EPIC,
            heroClass = HeroClass.PALADIN,
            forcedSlot = EquipSlot.WEAPON
        )
        val entity = com.example.data.InventoryItemEntity(
            id = weapon.id,
            characterId = "hero_1",
            name = weapon.name,
            slot = weapon.slot,
            rarity = weapon.rarity,
            isEquipped = true,
            requiredLevel = weapon.requiredLevel,
            attackBonus = weapon.attackBonus,
            armorBonus = weapon.armorBonus,
            hpBonus = weapon.hpBonus,
            manaBonus = weapon.manaBonus,
            critPctBonus = weapon.critPctBonus,
            dodgePctBonus = weapon.dodgePctBonus,
            speedPctBonus = weapon.speedPctBonus,
            lifestealPctBonus = weapon.lifestealPctBonus,
            setId = weapon.setId,
            setName = weapon.setName,
            goldValue = weapon.goldValue,
            description = weapon.description
        )
        assertEquals(EquipSlot.WEAPON, entity.slot)
        assertEquals(ItemRarity.EPIC, entity.rarity)
        assertTrue(entity.isEquipped)
    }

    @Test
    fun skillSlotStateCooldownFractionAndStatus() {
        val ready = com.example.engine.SkillSlotState(
            remainingSec = 0f,
            totalSec = 6f,
            status = com.example.engine.SkillExecutionStatus.READY
        )
        assertEquals(0f, ready.cooldownFraction, 0.001f)

        val halfCd = com.example.engine.SkillSlotState(
            remainingSec = 3f,
            totalSec = 6f,
            status = com.example.engine.SkillExecutionStatus.ON_COOLDOWN
        )
        assertEquals(0.5f, halfCd.cooldownFraction, 0.001f)

        val onGcd = com.example.engine.SkillSlotState(
            remainingSec = 0.225f,
            totalSec = 0.45f,
            status = com.example.engine.SkillExecutionStatus.ON_GLOBAL_COOLDOWN
        )
        assertEquals(0.5f, onGcd.cooldownFraction, 0.001f)
    }

    @Test
    fun floatingTextDistinctKindsAndLifeProgress() {
        val kinds = listOf(
            FloatingTextKind.NORMAL_DAMAGE,
            FloatingTextKind.CRITICAL_HIT,
            FloatingTextKind.ENEMY_DAMAGE,
            FloatingTextKind.HEAL
        )
        assertEquals(4, kinds.map { it.defaultColor }.distinct().size)

        val ft = FloatingText(
            id = 1L,
            x = 10f,
            y = 10f,
            text = "¡CRÍT 48!",
            color = FloatingTextKind.CRITICAL_HIT.defaultColor,
            kind = FloatingTextKind.CRITICAL_HIT,
            maxTtlSec = 1.4f,
            ttlSec = 0.7f
        )
        assertEquals(0.5f, ft.lifeProgress, 0.001f)
    }

    @Test
    fun dynamicQuestTrackerMappingAndPriority() {
        val profile = CharacterProfile(
            id = "hero_q",
            name = "Arthas",
            race = Race.HUMAN,
            heroClass = HeroClass.PALADIN,
            quests = listOf(
                QuestProgress(questId = "q_first_steps", currentCount = 1, completed = true, turnedIn = true),
                QuestProgress(questId = "q_emerald_wolves", currentCount = 2, completed = false, turnedIn = false),
                QuestProgress(questId = "q_bandit_camp", currentCount = 3, completed = true, turnedIn = false)
            )
        )

        val entries = com.example.ui.buildActiveQuestEntries(profile)
        assertEquals(2, entries.size)
        // Ready-to-turn-in quest comes first
        assertEquals("q_bandit_camp", entries[0].quest.id)
        assertTrue(entries[0].isReadyToTurnIn)
        assertEquals(1.0f, entries[0].progressFraction, 0.001f)

        // In-progress quest shows live count fraction
        assertEquals("q_emerald_wolves", entries[1].quest.id)
        assertFalse(entries[1].isReadyToTurnIn)
        assertEquals(0.5f, entries[1].progressFraction, 0.001f)
        assertTrue(entries[1].objectiveSummary.contains("2/4"))
    }

    @Test
    fun enemyStateMachineTransitions() {
        val baseWolf = EnemyInstance(
            id = "e_wolf",
            kind = EnemyKind.WOLF,
            spawnX = 25f,
            spawnY = 12f,
            x = 25f,
            y = 12f,
            hp = 70,
            maxHp = 70
        )

        // Far away & out of combat -> IDLE
        assertEquals(
            EnemyAiState.IDLE,
            com.example.engine.EnemyStateMachine.evaluateNextState(
                enemy = baseWolf,
                distToTarget = 12f,
                distFromSpawn = 0f,
                playerInSafeZone = false,
                playerStealthed = false,
                isTargetingSummon = false
            )
        )

        // Within aggro radius -> CHASE
        assertEquals(
            EnemyAiState.CHASE,
            com.example.engine.EnemyStateMachine.evaluateNextState(
                enemy = baseWolf,
                distToTarget = 4.5f,
                distFromSpawn = 1.5f,
                playerInSafeZone = false,
                playerStealthed = false,
                isTargetingSummon = false
            )
        )

        // Within attack range -> ATTACK
        assertEquals(
            EnemyAiState.ATTACK,
            com.example.engine.EnemyStateMachine.evaluateNextState(
                enemy = baseWolf,
                distToTarget = 1.1f,
                distFromSpawn = 2.0f,
                playerInSafeZone = false,
                playerStealthed = false,
                isTargetingSummon = false
            )
        )

        // Critical low HP (<= 22%) in combat -> FLEE
        val woundedWolf = baseWolf.copy(hp = 12, recentlyDamagedSec = 2.5f)
        assertEquals(
            EnemyAiState.FLEE,
            com.example.engine.EnemyStateMachine.evaluateNextState(
                enemy = woundedWolf,
                distToTarget = 3.0f,
                distFromSpawn = 2.0f,
                playerInSafeZone = false,
                playerStealthed = false,
                isTargetingSummon = false
            )
        )

        // Bosses never flee at low HP
        val woundedBoss = EnemyInstance(
            id = "e_boss",
            kind = EnemyKind.DRAGON_BOSS,
            spawnX = 65f,
            spawnY = 60f,
            x = 65f,
            y = 60f,
            hp = 200,
            maxHp = 2600,
            recentlyDamagedSec = 3.0f
        )
        assertEquals(
            EnemyAiState.ATTACK,
            com.example.engine.EnemyStateMachine.evaluateNextState(
                enemy = woundedBoss,
                distToTarget = 1.2f,
                distFromSpawn = 0.5f,
                playerInSafeZone = false,
                playerStealthed = false,
                isTargetingSummon = false
            )
        )
    }

    @Test
    fun miniMapProjectionAndQuestMarkers() {
        // Inside radar radius -> un-clamped exact relative coordinates
        val inside = com.example.ui.projectToMiniMap(
            playerX = 10f,
            playerY = 10f,
            targetX = 16f,
            targetY = 10f,
            radiusTiles = 15f,
            clampToEdge = false
        )
        assertNotNull(inside)
        assertEquals(0.4f, inside!!.first, 0.001f)
        assertEquals(0f, inside.second, 0.001f)
        assertFalse(inside.third)

        // Outside radar radius without clamping -> hidden
        val hiddenFar = com.example.ui.projectToMiniMap(
            playerX = 10f,
            playerY = 10f,
            targetX = 40f,
            targetY = 10f,
            radiusTiles = 15f,
            clampToEdge = false
        )
        assertNull(hiddenFar)

        // Quest & NPC markers include nearby NPCs and clamped distant quest objectives
        val profile = CharacterProfile(
            id = "hero_map",
            name = "Valerius",
            race = Race.HUMAN,
            heroClass = HeroClass.PALADIN,
            quests = listOf(QuestProgress(questId = "q_emerald_wolves", currentCount = 1))
        )
        val world = com.example.engine.WorldRenderState(
            playerX = 10f,
            playerY = 10f,
            enemies = OpenWorldMap.initialEnemies()
        )
        val markers = com.example.ui.buildMiniMapMarkers(profile, world, emptyMap(), radiusTiles = 15f)
        assertTrue(markers.any { it.kind == com.example.ui.MiniMapMarkerKind.QUEST_OBJECTIVE && it.isClampedToEdge })
        assertTrue(markers.any { it.kind == com.example.ui.MiniMapMarkerKind.NPC && !it.isClampedToEdge })
    }

    @Test
    fun dialogueTreeOptionsAffectQuests() {
        val vance = LootAndQuestData.npcs.first { it.id == "npc_vance" }
        val borin = LootAndQuestData.npcs.first { it.id == "npc_borin" }

        val initialProfile = CharacterProfile(
            id = "hero_dlg",
            name = "Kaelen",
            race = Race.HUMAN,
            heroClass = HeroClass.WARRIOR,
            gold = 50,
            potions = 2,
            quests = listOf(QuestProgress(questId = "q_first_steps"))
        )

        // 1. Borin tree exposes the active TALK_NPC branch and persuasion option completes objective + adds supplies
        val borinTree = DialogueCatalog.treeForNpc(borin)
        val borinRoot = borinTree.nodes.getValue(borinTree.rootSelector(initialProfile))
        assertTrue(borinRoot.options.any { it.id == "b_vance_quest" && it.isAvailable(initialProfile) })

        val borinOrderNode = borinTree.nodes.getValue("borin_vance_order")
        val persuadeSupplies = borinOrderNode.options.first { it.id == "b_persuade_supplies" }
        val afterBorin = DialogueEngine.applyOption(initialProfile, borin.id, persuadeSupplies)

        assertTrue(afterBorin.updatedProfile.quests.first { it.questId == "q_first_steps" }.completed)
        assertEquals(65, afterBorin.updatedProfile.gold)
        assertEquals(3, afterBorin.updatedProfile.potions)
        assertNotNull(afterBorin.feedbackMessage)

        // 2. Vance tree lets player turn in q_first_steps, granting XP, gold, and reward weapon
        val vanceTree = DialogueCatalog.treeForNpc(vance)
        val vanceRoot = vanceTree.nodes.getValue(vanceTree.rootSelector(afterBorin.updatedProfile))
        val turnInOpt = vanceRoot.options.first { it.id == "v_turnin_first" && it.isAvailable(afterBorin.updatedProfile) }
        val afterTurnIn = DialogueEngine.applyOption(afterBorin.updatedProfile, vance.id, turnInOpt)

        assertTrue(afterTurnIn.updatedProfile.quests.first { it.questId == "q_first_steps" }.turnedIn)
        assertEquals(65 + 45, afterTurnIn.updatedProfile.gold)
        assertTrue(afterTurnIn.updatedProfile.inventory.any { it.id == "q_reward_starter_weapon" })

        // 3. Follow-up quest q_emerald_wolves is now available via dialogue persuasion branch
        val afterFirstNode = vanceTree.nodes.getValue("vance_after_first")
        assertTrue(afterFirstNode.options.any { it.id == "v_next_wolves" && it.isAvailable(afterTurnIn.updatedProfile) })
        val wolvesBrief = vanceTree.nodes.getValue("vance_brief_wolves")
        val acceptWolvesPersuade = wolvesBrief.options.first { it.id == "v_accept_wolves_persuade" }
        val afterAcceptWolves = DialogueEngine.applyOption(afterTurnIn.updatedProfile, vance.id, acceptWolvesPersuade)

        assertTrue(afterAcceptWolves.updatedProfile.quests.any { it.questId == "q_emerald_wolves" && !it.completed })
        assertEquals(4, afterAcceptWolves.updatedProfile.potions)
        assertEquals(65 + 45 + 15, afterAcceptWolves.updatedProfile.gold)
    }

    @Test
    fun cameraFollowAndBoundsClamping() {
        val tilePx = 68f
        val vpW = 1080f
        val vpH = 1920f
        val maxX = OpenWorldMap.WIDTH * tilePx - vpW
        val maxY = OpenWorldMap.HEIGHT * tilePx - vpH

        // Top-left map boundary clamping
        val topLeft = com.example.ui.computeClampedCameraOffset(
            targetTileX = 1.5f,
            targetTileY = 1.5f,
            facingRight = false,
            viewportWidthPx = vpW,
            viewportHeightPx = vpH,
            tilePx = tilePx
        )
        assertEquals(0f, topLeft.x, 0.01f)
        assertEquals(0f, topLeft.y, 0.01f)

        // Bottom-right map boundary clamping
        val bottomRight = com.example.ui.computeClampedCameraOffset(
            targetTileX = 70.5f,
            targetTileY = 70.5f,
            facingRight = true,
            viewportWidthPx = vpW,
            viewportHeightPx = vpH,
            tilePx = tilePx
        )
        assertEquals(maxX, bottomRight.x, 0.01f)
        assertEquals(maxY, bottomRight.y, 0.01f)

        // Directional look-ahead shifts camera ahead of facing direction in open field
        val lookRight = com.example.ui.computeClampedCameraOffset(
            targetTileX = 36f,
            targetTileY = 36f,
            facingRight = true,
            viewportWidthPx = vpW,
            viewportHeightPx = vpH,
            tilePx = tilePx
        )
        val lookLeft = com.example.ui.computeClampedCameraOffset(
            targetTileX = 36f,
            targetTileY = 36f,
            facingRight = false,
            viewportWidthPx = vpW,
            viewportHeightPx = vpH,
            tilePx = tilePx
        )
        assertTrue(lookRight.x > lookLeft.x)
        assertEquals(lookRight.y, lookLeft.y, 0.01f)

        // Smooth interpolation moves partially toward nearby target, snaps on large teleport
        val current = androidx.compose.ui.geometry.Offset(500f, 500f)
        val nearTarget = androidx.compose.ui.geometry.Offset(600f, 500f)
        val stepped = com.example.ui.stepSmoothCamera(current, nearTarget, dtSec = 0.016f)
        assertTrue(stepped.x > current.x && stepped.x < nearTarget.x)

        val farTarget = androidx.compose.ui.geometry.Offset(2500f, 2500f)
        val snapped = com.example.ui.stepSmoothCamera(current, farTarget, dtSec = 0.016f)
        assertEquals(farTarget.x, snapped.x, 0.01f)
        assertEquals(farTarget.y, snapped.y, 0.01f)
    }

    @Test
    fun twentyZoneBossesWithSuperiorStatsAndWalkableSpawns() {
        val bosses = com.example.engine.BossTactics.allZoneBosses()
        assertEquals("Must have exactly 20 distinct zone boss variants", 20, bosses.size)

        val combatZones = listOf(
            ZoneId.EMERALD_FOREST,
            ZoneId.TWILIGHT_RUINS,
            ZoneId.CURSED_SWAMP,
            ZoneId.DRAGON_SUMMIT
        )
        combatZones.forEach { zone ->
            val zoneBosses = com.example.engine.BossTactics.bossesForZone(zone)
            assertEquals("Zone $zone should have 5 boss variants", 5, zoneBosses.size)

            val maxMobHp = EnemyKind.entries.filter { !it.isBoss && it.zone == zone }.maxOf { it.baseHp }
            val maxMobAtk = EnemyKind.entries.filter { !it.isBoss && it.zone == zone }.maxOf { it.baseAttack }

            zoneBosses.forEach { boss ->
                assertNotNull("${boss.name} must have a dodgeable bossTactic", boss.bossTactic)
                assertTrue("${boss.name} HP (${boss.baseHp}) must exceed max zone mob HP ($maxMobHp)", boss.baseHp > maxMobHp)
                assertTrue("${boss.name} Atk (${boss.baseAttack}) must exceed max zone mob Atk ($maxMobAtk)", boss.baseAttack > maxMobAtk)
            }
        }

        val spawnedEnemies = OpenWorldMap.initialEnemies()
        val spawnedBosses = spawnedEnemies.filter { it.kind.isBoss }
        assertEquals(20, spawnedBosses.size)
        assertEquals(20, spawnedBosses.map { it.kind }.distinct().size)

        spawnedBosses.forEach { boss ->
            assertTrue("Boss ${boss.kind.displayName} spawn must be walkable", OpenWorldMap.isWalkable(boss.spawnX, boss.spawnY))
            assertEquals("Boss ${boss.kind.displayName} must be in zone ${boss.kind.zone}", boss.kind.zone, OpenWorldMap.zoneAt(boss.spawnX, boss.spawnY))
        }
    }

    @Test
    fun bossTelegraphsSimpleDodgeTactics() {
        // 1. RADIAL_SLAM: step outside radius to dodge
        val slamBoss = EnemyInstance("b1", EnemyKind.BOSS_THORN_ENT, 30f, 19f, 30f, 19f, 410, 410)
        val slamTel = com.example.engine.BossTactics.createTelegraph(1L, slamBoss, 31f, 19f)!!
        assertTrue(com.example.engine.BossTactics.isPointHitByTelegraph(slamTel, 31.5f, 19f))
        assertFalse(com.example.engine.BossTactics.isPointHitByTelegraph(slamTel, 33.0f, 19f))

        // 2. LINEAR_CHARGE: step sideways perpendicular to charge line to dodge
        val chargeBoss = EnemyInstance("b2", EnemyKind.BOSS_ALPHA_WOLF, 27f, 6f, 27f, 6f, 310, 310)
        val chargeTel = com.example.engine.BossTactics.createTelegraph(2L, chargeBoss, 30f, 6f)!!
        assertTrue(com.example.engine.BossTactics.isPointHitByTelegraph(chargeTel, 29.5f, 6.2f))
        assertFalse(com.example.engine.BossTactics.isPointHitByTelegraph(chargeTel, 29.5f, 7.2f))

        // 3. METEOR_ZONE: move out of marked target circle to dodge
        val meteorBoss = EnemyInstance("b3", EnemyKind.BOSS_PLAGUE_MATRON, 10f, 39f, 10f, 39f, 1180, 1180)
        val meteorTel = com.example.engine.BossTactics.createTelegraph(3L, meteorBoss, 12f, 40f)!!
        assertTrue(com.example.engine.BossTactics.isPointHitByTelegraph(meteorTel, 12.4f, 40.2f))
        assertFalse(com.example.engine.BossTactics.isPointHitByTelegraph(meteorTel, 14.2f, 40f))

        // 4. SHOCKWAVE_CONE: circle behind or to the flank of the boss to dodge
        val coneBoss = EnemyInstance("b4", EnemyKind.DRAGON_BOSS, 60f, 60f, 60f, 60f, 3100, 3100)
        val coneTel = com.example.engine.BossTactics.createTelegraph(4L, coneBoss, 62f, 60f)!!
        assertTrue(com.example.engine.BossTactics.isPointHitByTelegraph(coneTel, 62.0f, 60.3f))
        assertFalse(com.example.engine.BossTactics.isPointHitByTelegraph(coneTel, 58.5f, 60f))
        assertFalse(com.example.engine.BossTactics.isPointHitByTelegraph(coneTel, 60.0f, 62.0f))
    }

    @Test
    fun bossDifficultyScalesAcrossLevelsOneToThirty() {
        val bosses = com.example.engine.BossTactics.allZoneBosses()
        val distinctBaseLevels = bosses.map { it.baseLevel }.distinct().sorted()
        assertEquals(2, distinctBaseLevels.first())
        assertEquals(30, distinctBaseLevels.last())
        assertTrue("Bosses should span at least 15 varied difficulty levels 1..30", distinctBaseLevels.size >= 15)

        // At player level 1, every boss still keeps at least base stats & beats regular zone mobs
        bosses.forEach { boss ->
            val scaledLv1 = com.example.engine.BossTactics.scaleBossStats(boss, playerLevel = 1)
            assertEquals(boss.baseLevel, scaledLv1.effectiveLevel)
            assertTrue(scaledLv1.maxHp >= boss.baseHp)
            assertTrue(scaledLv1.attack >= boss.baseAttack)
        }

        // Dynamic scaling across player levels 1 -> 10 -> 20 -> 30 increases stats monotonically while staying dodgeable
        val earlyBoss = EnemyKind.BOSS_ALPHA_WOLF
        val s1 = com.example.engine.BossTactics.scaleBossStats(earlyBoss, 1)
        val s10 = com.example.engine.BossTactics.scaleBossStats(earlyBoss, 10)
        val s20 = com.example.engine.BossTactics.scaleBossStats(earlyBoss, 20)
        val s30 = com.example.engine.BossTactics.scaleBossStats(earlyBoss, 30)

        assertTrue(s1.effectiveLevel < s10.effectiveLevel && s10.effectiveLevel < s20.effectiveLevel && s20.effectiveLevel < s30.effectiveLevel)
        assertEquals(30, s30.effectiveLevel)
        assertTrue(s1.maxHp < s10.maxHp && s10.maxHp < s20.maxHp && s20.maxHp < s30.maxHp)
        assertTrue(s1.attack < s10.attack && s10.attack < s20.attack && s20.attack < s30.attack)
        assertTrue(s1.xpReward < s10.xpReward && s10.xpReward < s20.xpReward && s20.xpReward < s30.xpReward)
        assertTrue(s1.goldReward < s10.goldReward && s10.goldReward < s20.goldReward && s20.goldReward < s30.goldReward)
        assertTrue(s30.windupSec in 0.85f..s1.windupSec)
        assertTrue(s30.telegraphCooldownSec in 3.5f..s1.telegraphCooldownSec)

        // Apex dragon remains deadlier than early boss even when both scale to level 30
        val apex30 = com.example.engine.BossTactics.scaleBossStats(EnemyKind.DRAGON_BOSS, 30)
        assertTrue(apex30.maxHp > s30.maxHp)
        assertTrue(apex30.attack > s30.attack)

        // applyBossScaling updates EnemyInstance fields and scaled telegraph damage
        val baseInstance = EnemyInstance(
            id = "b_scale",
            kind = earlyBoss,
            spawnX = 27f,
            spawnY = 6f,
            x = 27f,
            y = 6f,
            hp = earlyBoss.baseHp,
            maxHp = earlyBoss.baseHp
        )
        val scaledInstance = com.example.engine.BossTactics.applyBossScaling(baseInstance, playerLevel = 20)
        assertEquals(20, scaledInstance.effectiveLevel)
        assertEquals(s20.maxHp, scaledInstance.maxHp)
        assertEquals(s20.maxHp, scaledInstance.hp)
        assertEquals(s20.attack, scaledInstance.scaledAttack)
        assertEquals(s20.xpReward, scaledInstance.scaledXpReward)
        assertEquals(s20.goldReward, scaledInstance.scaledGoldReward)

        val scaledTel = com.example.engine.BossTactics.createTelegraph(99L, scaledInstance, 29f, 6f)!!
        assertTrue(scaledTel.damage >= scaledInstance.scaledAttack)
        assertEquals(s20.windupSec, scaledTel.totalWindupSec, 0.001f)
    }

    @Test
    fun experienceScalesWithMobLevelAndBossFourXMultiplier() {
        // Mob XP strictly increases from level 1 to level 30
        var prevXp = 0
        for (lv in 1..MAX_PLAYER_LEVEL) {
            val mobXp = com.example.engine.ExperienceSystem.calculateKillXp(mobLevel = lv, isBoss = false)
            val bossXp = com.example.engine.ExperienceSystem.calculateKillXp(mobLevel = lv, isBoss = true)
            assertTrue("Level $lv mob XP ($mobXp) must exceed level ${lv - 1} ($prevXp)", mobXp > prevXp)
            assertEquals("Level $lv boss must give exactly 4x XP of a level $lv mob", mobXp * 4, bossXp)
            prevXp = mobXp
        }

        // Initialized world enemies carry consistent level-based XP (and 4x for bosses)
        val initial = OpenWorldMap.initialEnemies()
        val wolf = initial.first { it.kind == EnemyKind.WOLF }
        val golem = initial.first { it.kind == EnemyKind.OBSIDIAN_GOLEM }
        assertTrue(golem.scaledXpReward > wolf.scaledXpReward)

        val alphaBoss = initial.first { it.kind == EnemyKind.BOSS_ALPHA_WOLF }
        assertEquals(
            com.example.engine.ExperienceSystem.calculateKillXp(alphaBoss.effectiveLevel, isBoss = false) * 4,
            alphaBoss.scaledXpReward
        )

        // Scaled boss at level 25 gives 4x the XP of a level 25 mob
        val scaledBoss25 = com.example.engine.BossTactics.applyBossScaling(alphaBoss, playerLevel = 25)
        assertEquals(25, scaledBoss25.effectiveLevel)
        assertEquals(
            com.example.engine.ExperienceSystem.calculateKillXp(25, isBoss = false) * 4,
            com.example.engine.ExperienceSystem.calculateEnemyKillXp(scaledBoss25)
        )

        // Multi-level rollover and cap at level 30
        val lvlUp = com.example.engine.ExperienceSystem.applyXpGain(currentLevel = 1, currentXp = 0, xpGained = 500)
        assertTrue(lvlUp.newLevel >= 3)
        assertEquals(lvlUp.newLevel - 1, lvlUp.levelsGained)

        val capped = com.example.engine.ExperienceSystem.applyXpGain(currentLevel = MAX_PLAYER_LEVEL, currentXp = 0, xpGained = 9999)
        assertEquals(MAX_PLAYER_LEVEL, capped.newLevel)
        assertEquals(0, capped.newXp)
    }
}
