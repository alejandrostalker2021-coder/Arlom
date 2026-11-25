package com.example.model

import kotlin.random.Random

object LootAndQuestData {

    fun xpToNextLevel(level: Int): Int {
        if (level >= MAX_PLAYER_LEVEL) return Int.MAX_VALUE
        return 80 + level * 45 + level * level * 7
    }

    val setBonuses: Map<String, SetBonusDef> = listOf(
        SetBonusDef(
            setId = "set_cloth_scholar",
            setName = "Erudito de Valoria",
            armorWeight = ArmorWeight.CLOTH,
            twoPieceDesc = "(2) +20 Poder Arcano y +75 Vida máxima",
            fourPieceDesc = "(4) +38 Poder Arcano, +135 Vida y +6% Crítico",
            fivePieceDesc = "(5) +55 Poder Arcano, +10% Crítico y +8% Velocidad",
            twoPieceAtk = 20,
            twoPieceHp = 75,
            fourPieceAtk = 38,
            fourPieceHp = 135,
            fourPieceCrit = 0.06f,
            fivePieceAtk = 55,
            fivePieceCrit = 0.10f,
            fivePieceSpeedPct = 0.08f
        ),
        SetBonusDef(
            setId = "set_arcane_aethel",
            setName = "Arcano de Aethel",
            armorWeight = ArmorWeight.CLOTH,
            twoPieceDesc = "(2) +36 Poder Arcano y +120 Vida máxima",
            fourPieceDesc = "(4) +68 Poder Arcano, +180 Vida y +12% Crítico",
            fivePieceDesc = "(5) +95 Poder Arcano, +16% Crítico y +8% Robo de Vida",
            twoPieceAtk = 36,
            twoPieceHp = 120,
            fourPieceAtk = 68,
            fourPieceHp = 180,
            fourPieceCrit = 0.12f,
            fivePieceAtk = 95,
            fivePieceCrit = 0.16f,
            fivePieceLifesteal = 0.08f
        ),
        SetBonusDef(
            setId = "set_leather_scout",
            setName = "Batidor del Crepúsculo",
            armorWeight = ArmorWeight.MAIL_LEATHER,
            twoPieceDesc = "(2) +18 Ataque y +16 Armadura",
            fourPieceDesc = "(4) +36 Ataque, +110 Vida y +8% Crítico",
            fivePieceDesc = "(5) +52 Ataque, +12% Crítico y +10% Evasión",
            twoPieceAtk = 18,
            twoPieceArmor = 16,
            fourPieceAtk = 36,
            fourPieceHp = 110,
            fourPieceCrit = 0.08f,
            fivePieceAtk = 52,
            fivePieceCrit = 0.12f,
            fivePieceDodge = 0.10f
        ),
        SetBonusDef(
            setId = "set_wild_stalker",
            setName = "Acechador Salvaje",
            armorWeight = ArmorWeight.MAIL_LEATHER,
            twoPieceDesc = "(2) +32 Ataque y +24 Armadura",
            fourPieceDesc = "(4) +62 Ataque, +160 Vida y +14% Crítico",
            fivePieceDesc = "(5) +88 Ataque, +16% Evasión y +10% Velocidad",
            twoPieceAtk = 32,
            twoPieceArmor = 24,
            fourPieceAtk = 62,
            fourPieceHp = 160,
            fourPieceCrit = 0.14f,
            fivePieceAtk = 88,
            fivePieceDodge = 0.16f,
            fivePieceSpeedPct = 0.10f
        ),
        SetBonusDef(
            setId = "set_dawn_vanguard",
            setName = "Bastión del Alba",
            armorWeight = ArmorWeight.PLATE,
            twoPieceDesc = "(2) +35 Armadura y +140 Vida máxima",
            fourPieceDesc = "(4) +48 Ataque, +240 Vida y +8% Crítico",
            fivePieceDesc = "(5) +65 Ataque, +60 Armadura y +320 Vida",
            twoPieceArmor = 35,
            twoPieceHp = 140,
            fourPieceAtk = 48,
            fourPieceHp = 240,
            fourPieceCrit = 0.08f,
            fivePieceAtk = 65,
            fivePieceArmor = 60,
            fivePieceHp = 320
        ),
        SetBonusDef(
            setId = "set_crimson_oath",
            setName = "Juramento Carmesí",
            armorWeight = ArmorWeight.PLATE,
            twoPieceDesc = "(2) +34 Ataque y +150 Vida máxima",
            fourPieceDesc = "(4) +64 Ataque, +10% Robo de Vida y +10% Crítico",
            fivePieceDesc = "(5) +90 Ataque, +75 Armadura y +15% Robo de Vida",
            twoPieceAtk = 34,
            twoPieceHp = 150,
            fourPieceAtk = 64,
            fourPieceLifesteal = 0.10f,
            fourPieceCrit = 0.10f,
            fivePieceAtk = 90,
            fivePieceArmor = 75,
            fivePieceLifesteal = 0.15f
        )
    ).associateBy { it.setId }

    fun skillsForClass(heroClass: HeroClass): List<SkillDef> = when (heroClass) {
        HeroClass.PALADIN -> listOf(
            SkillDef("pal_holy_strike", "Golpe Sagrado", "Impacto consagrado que inflige daño medio y genera amenaza.", 14, 3.0f, 0xFFFACC15, "DPS/Amenaza"),
            SkillDef("pal_flash_light", "Destello de Luz", "Sana tus heridas y a los aliados cercanos del grupo.", 22, 6.0f, 0xFF10B981, "Sanación"),
            SkillDef("pal_divine_shield", "Escudo Divino", "Aumenta armadura y otorga escudo protector por 5s.", 24, 12.0f, 0xFF38BDF8, "Tanque"),
            SkillDef("pal_consecration", "Consagración", "Área sagrada que quema enemigos y regenera HP.", 28, 9.0f, 0xFFF59E0B, "Área")
        )
        HeroClass.WARRIOR -> listOf(
            SkillDef("war_brutal_slam", "Embate Brutal", "Golpe pesado de placas con daño físico contundente.", 12, 2.8f, 0xFFEF4444, "DPS"),
            SkillDef("war_whirlwind", "Torbellino", "Gira con tu arma golpeando a todos los enemigos cercanos.", 20, 6.0f, 0xFFF97316, "Área"),
            SkillDef("war_shield_wall", "Muro de Escudos", "Muy alta resistencia: -45% daño recibido por 6s.", 22, 13.0f, 0xFF64748B, "Resistencia"),
            SkillDef("war_taunt", "Provocar", "Ruge atrayendo a los enemigos hacia ti y ganando armadura.", 15, 8.0f, 0xFFDC2626, "Tanque")
        )
        HeroClass.KNIGHT -> listOf(
            SkillDef("kni_crimson_slash", "Tajo Carmesí", "Golpe sombrío que roba el 40% del daño como vida.", 14, 3.2f, 0xFFDC2626, "Robo Vida"),
            SkillDef("kni_blood_pact", "Pacto de Sangre", "Aura vampírica: +18% robo de vida y +15% daño por 6s.", 22, 11.0f, 0xFF991B1B, "DPS/Robo"),
            SkillDef("kni_runic_strike", "Golpe Rúnico", "Estocada perforante de alto daño crítico.", 18, 5.0f, 0xFFA855F7, "DPS"),
            SkillDef("kni_bone_armor", "Armadura Ósea", "Resistencia intermedia (superior al Paladín) por 7s.", 20, 12.0f, 0xFFCBD5E1, "Defensa")
        )
        HeroClass.HUNTER_SURVIVAL -> listOf(
            SkillDef("hs_rapid_shot", "Disparo Rápido", "Ráfaga veloz de flechas de alta precisión.", 14, 2.6f, 0xFF22C55E, "DPS"),
            SkillDef("hs_wild_instinct", "Instinto Salvaje", "+35% velocidad de movimiento y +25% evasión por 5s.", 18, 10.0f, 0xFF10B981, "Evasión"),
            SkillDef("hs_spear_lunge", "Tajo de Lanza", "Ataque ágil con sangrado rápido.", 16, 4.5f, 0xFFF59E0B, "DPS"),
            SkillDef("hs_evasive_leap", "Salto Evasivo", "Salto táctico que esquiva golpes y daña al frente.", 20, 8.0f, 0xFF38BDF8, "Movilidad")
        )
        HeroClass.HUNTER_BEASTMASTER -> listOf(
            SkillDef("hb_call_beast", "Llamar Bestia", "Doma una Bestia Guardiana tanque con alta vida y +220% amenaza.", 26, 14.0f, 0xFFD97706, "Invocar Tanque"),
            SkillDef("hb_roar", "Rugido Amenazador", "La bestia provoca a los monstruos cercanos alejándolos del anfitrión.", 16, 7.0f, 0xFFEF4444, "Amenaza"),
            SkillDef("hb_bestial_wrath", "Furia Bestial", "Tu bestia y tú ganan +30% daño y resistencia por 6s.", 22, 12.0f, 0xFFF59E0B, "Buff"),
            SkillDef("hb_kill_command", "Orden de Caza", "Disparo que marca al objetivo para que la bestia embista.", 14, 3.5f, 0xFF84CC16, "DPS")
        )
        HeroClass.HUNTER_TRAPPER -> listOf(
            SkillDef("ht_snare_trap", "Trampa de Cepo", "Coloca trampa que inmoviliza (Root) al enemigo por 3.5s.", 18, 7.5f, 0xFF84CC16, "Inmoviliza"),
            SkillDef("ht_slow_shot", "Disparo Ralentizador", "Flecha pesada que ralentiza 50% al enemigo por 4.5s.", 14, 4.0f, 0xFF38BDF8, "Ralentiza"),
            SkillDef("ht_venom_arrow", "Flecha Venenosa", "Veneno que quita vida continua y reduce curación en 50%.", 16, 5.0f, 0xFF10B981, "Veneno"),
            SkillDef("ht_pierce_shot", "Disparo Perforante", "Daño masivo a objetivos atrapados o envenenados.", 20, 6.0f, 0xFFFACC15, "DPS")
        )
        HeroClass.MAGE_WARLOCK -> listOf(
            SkillDef("wl_curse_agony", "Maldición de Agonía", "Maldición sombría que inflige daño creciente por 6s.", 16, 4.0f, 0xFF9333EA, "Maldición"),
            SkillDef("wl_curse_weak", "Maldición Debilidad", "Reduce 30% el ataque y armadura del enemigo por 6s.", 16, 6.0f, 0xFF6B21A8, "Maldición"),
            SkillDef("wl_demon_pact", "Pacto Demoníaco", "Invoca un Demonio aliado que potencia tus maldiciones y fuego +25%.", 28, 14.0f, 0xFFDC2626, "Demonio"),
            SkillDef("wl_demon_bolt", "Descarga Demoníaca", "Daño caótico que escala +35% si el objetivo tiene maldiciones.", 22, 5.0f, 0xFFA855F7, "DPS")
        )
        HeroClass.MAGE_ELEMENTAL -> listOf(
            SkillDef("el_fireball", "Bola de Fuego", "Explosión ígnea de alto daño elemental.", 18, 3.0f, 0xFFEF4444, "Fuego"),
            SkillDef("el_frost_nova", "Nova de Escarcha", "Congela y ralentiza a todos los enemigos cercanos.", 22, 7.5f, 0xFF38BDF8, "Hielo"),
            SkillDef("el_chain_lightning", "Cadena Relámpagos", "Rayo que salta hasta entre 3 enemigos.", 24, 5.5f, 0xFFFACC15, "Rayo"),
            SkillDef("el_elem_barrier", "Barrera Elemental", "Escudo arcano de los elementos que absorbe daño.", 22, 11.0f, 0xFF818CF8, "Escudo")
        )
        HeroClass.MAGE_SUMMONER -> listOf(
            SkillDef("sm_arcane_orb", "Orbe Arcano", "Proyectil mágico a distancia que ralentiza 40% al objetivo.", 14, 2.8f, 0xFF6366F1, "Ralentiza"),
            SkillDef("sm_surface_blast", "Explosión Superficial", "Detonación en área que aturde (Stun) a enemigos por 2.2s.", 22, 7.0f, 0xFFF59E0B, "Stun Área"),
            SkillDef("sm_call_sentinel", "Invocar Centinela", "Invoca un Centinela Arcano a distancia para combatir.", 26, 13.0f, 0xFF818CF8, "Invocar"),
            SkillDef("sm_forced_duel", "Duelo Obligatorio", "Tu invocación desafía al enemigo a combate obligatorio 1v1 por 3s.", 18, 8.5f, 0xFFEC4899, "Duelo 3s")
        )
        HeroClass.SHAMAN -> listOf(
            SkillDef("sh_earth_shock", "Choque Elemental", "Descarga rápida de tierra y relámpago.", 15, 3.0f, 0xFF0EA5E9, "Choque"),
            SkillDef("sh_storm_totem", "Tótem de Tormenta", "Coloca un tótem que lanza rayos a enemigos cercanos por 10s.", 24, 11.0f, 0xFF38BDF8, "Tótem DPS"),
            SkillDef("sh_heal_totem", "Tótem Curativo", "Coloca un tótem que sana continuamente a aliados en el área.", 24, 12.0f, 0xFF10B981, "Tótem Cura"),
            SkillDef("sh_windfury", "Furia del Viento", "Bendición ancestral: +30% velocidad ataque y daño por 6s.", 20, 10.0f, 0xFFFACC15, "Buff")
        )
        HeroClass.STALKER -> listOf(
            SkillDef("st_stealth", "Sigilo Sombrío", "Desapareces en las sombras; tu próximo golpe inflige 2.3x crítico.", 16, 9.0f, 0xFF64748B, "Sigilo"),
            SkillDef("st_sinister", "Puñalada Siniestra", "Ataque veloz de daga con alta probabilidad crítica.", 12, 2.2f, 0xFFE11D48, "Combo"),
            SkillDef("st_eviscerate", "Eviscerar", "Remate letal que consume combos para daño masivo.", 22, 5.5f, 0xFFDC2626, "Remate"),
            SkillDef("st_smoke_bomb", "Bomba de Humo", "+45% evasión y +25% velocidad por 4.5s.", 18, 11.0f, 0xFFA855F7, "Evasión")
        )
    }

    /**
     * Enforces the user's exact loot distribution caps:
     * - Common: 32%
     * - Rare: max 40%
     * - Epic: max 20%
     * - Legendary / Unique Set: max 8% (split 4% Legendary, 4% Unique Set)
     */
    fun rollLootRarity(minRarity: ItemRarity = ItemRarity.COMMON, isBoss: Boolean = false): ItemRarity {
        val roll = Random.nextInt(100)
        val rolled = when {
            roll < 4 -> ItemRarity.UNIQUE_SET     // 4% (8% combined Legendary + Unique Set)
            roll < 8 -> ItemRarity.LEGENDARY      // 4%
            roll < 28 -> ItemRarity.EPIC          // 20%
            roll < 68 -> ItemRarity.RARE          // 40%
            else -> ItemRarity.COMMON             // 32%
        }
        val effective = if (isBoss && rolled == ItemRarity.COMMON) ItemRarity.RARE else rolled
        return if (effective.ordinal < minRarity.ordinal) minRarity else effective
    }

    private fun slotDisplayNameForMaterial(
        slot: EquipSlot,
        armorWeight: ArmorWeight,
        weaponCategory: WeaponCategory
    ): String = when (slot) {
        EquipSlot.WEAPON -> when (weaponCategory) {
            WeaponCategory.STAFF_GRIMOIRE -> listOf("Báculo", "Grimorio", "Cetro").random()
            WeaponCategory.BOW_SPEAR -> listOf("Arco Largo", "Ballesta", "Lanza").random()
            WeaponCategory.TWIN_DAGGERS -> listOf("Dagas Gemelas", "Hoja Oculta").random()
            WeaponCategory.RUNIC_AXE_TOTEM -> listOf("Hacha Rúnica", "Tótem de Mano").random()
            WeaponCategory.HEAVY_MELEE -> listOf("Mandoble", "Espada Bastarda", "Martillo de Guerra").random()
        }
        EquipSlot.HELM -> when (armorWeight) {
            ArmorWeight.CLOTH -> "Capucha de Tela"
            ArmorWeight.MAIL_LEATHER -> "Casco de Cuero"
            ArmorWeight.PLATE -> "Yelmo de Placas"
        }
        EquipSlot.BRACERS -> when (armorWeight) {
            ArmorWeight.CLOTH -> "Brazales de Tela"
            ArmorWeight.MAIL_LEATHER -> "Brazales de Cuero"
            ArmorWeight.PLATE -> "Brazales de Placas"
        }
        EquipSlot.ARMOR -> when (armorWeight) {
            ArmorWeight.CLOTH -> "Túnica de Tela"
            ArmorWeight.MAIL_LEATHER -> "Pechera de Cuero"
            ArmorWeight.PLATE -> "Peto de Placas"
        }
        EquipSlot.BOOTS -> when (armorWeight) {
            ArmorWeight.CLOTH -> "Botas de Tela"
            ArmorWeight.MAIL_LEATHER -> "Botas de Cuero"
            ArmorWeight.PLATE -> "Grebas de Placas"
        }
        EquipSlot.TRINKET -> listOf("Amuleto", "Anillo Sellado", "Talismán").random()
    }

    fun setDefForClass(heroClass: HeroClass, tier: Int): SetBonusDef {
        val highTier = tier >= 3
        val key = when (heroClass.armorWeight) {
            ArmorWeight.CLOTH -> if (highTier) "set_arcane_aethel" else "set_cloth_scholar"
            ArmorWeight.MAIL_LEATHER -> if (highTier) "set_wild_stalker" else "set_leather_scout"
            ArmorWeight.PLATE -> when {
                highTier || heroClass == HeroClass.KNIGHT -> "set_crimson_oath"
                else -> "set_dawn_vanguard"
            }
        }
        return setBonuses.getValue(key)
    }

    fun generateEquipment(
        targetLevel: Int,
        rarity: ItemRarity,
        heroClass: HeroClass? = null,
        forcedSlot: EquipSlot? = null,
        forcedSetId: String? = null,
        forcedId: String? = null
    ): EquipmentItem {
        val resolvedClass = heroClass ?: HeroClass.entries.random()
        val armorWeight = resolvedClass.armorWeight
        val weaponCategory = resolvedClass.weaponCategory
        val clampedLevel = targetLevel.coerceIn(1, MAX_PLAYER_LEVEL)
        val slot = forcedSlot ?: EquipSlot.entries.random()
        val mult = rarity.statMultiplier
        val levelScale = 1f + (clampedLevel - 1) * 0.24f

        val setChoice = when {
            forcedSetId != null -> setBonuses[forcedSetId]
            rarity == ItemRarity.UNIQUE_SET -> setDefForClass(resolvedClass, tier = if (clampedLevel >= 16) 3 else 1)
            else -> null
        }

        val prefix = when (rarity) {
            ItemRarity.COMMON -> when (armorWeight) {
                ArmorWeight.CLOTH -> listOf("Lino", "Seda Simple", "Aprendiz").random()
                ArmorWeight.MAIL_LEATHER -> listOf("Cuero Curtido", "Monte", "Cazador").random()
                ArmorWeight.PLATE -> listOf("Hierro", "Acero Forjado", "Milicia").random()
            }
            ItemRarity.RARE -> listOf("Acero Real", "Guardián", "Bosque Antiguo", "Plata").random()
            ItemRarity.EPIC -> listOf("Crepúsculo", "Tormenta", "Obsidiana", "Arcano").random()
            ItemRarity.LEGENDARY -> listOf("Valoria Eterna", "Sangre de Dragón", "Corona Solar", "Sombra Suprema").random()
            ItemRarity.UNIQUE_SET -> setChoice?.setName ?: "Reliquia Única"
        }

        val baseSlotName = slotDisplayNameForMaterial(slot, armorWeight, weaponCategory)
        val itemName = if (setChoice != null) "$baseSlotName (${setChoice.setName})" else "$baseSlotName de $prefix"

        val atk = when (slot) {
            EquipSlot.WEAPON -> (10 * levelScale * mult).toInt()
            EquipSlot.BRACERS -> (4 * levelScale * mult).toInt()
            EquipSlot.TRINKET -> (4 * levelScale * mult).toInt()
            EquipSlot.HELM -> (2 * levelScale * mult).toInt()
            EquipSlot.BOOTS -> (2 * levelScale * mult).toInt()
            EquipSlot.ARMOR -> (2 * levelScale * mult).toInt()
        }
        val armorBase = when (armorWeight) {
            ArmorWeight.PLATE -> 1.25f
            ArmorWeight.MAIL_LEATHER -> 1.0f
            ArmorWeight.CLOTH -> 0.72f
        }
        val armor = when (slot) {
            EquipSlot.ARMOR -> (12 * levelScale * mult * armorBase).toInt()
            EquipSlot.HELM -> (7 * levelScale * mult * armorBase).toInt()
            EquipSlot.BRACERS -> (5 * levelScale * mult * armorBase).toInt()
            EquipSlot.BOOTS -> (6 * levelScale * mult * armorBase).toInt()
            EquipSlot.TRINKET -> (3 * levelScale * mult).toInt()
            EquipSlot.WEAPON -> (2 * levelScale * mult).toInt()
        }
        val hp = when (slot) {
            EquipSlot.ARMOR -> (32 * levelScale * mult).toInt()
            EquipSlot.HELM -> (20 * levelScale * mult).toInt()
            EquipSlot.BRACERS -> (16 * levelScale * mult).toInt()
            EquipSlot.BOOTS -> (18 * levelScale * mult).toInt()
            EquipSlot.TRINKET -> (24 * levelScale * mult).toInt()
            EquipSlot.WEAPON -> (10 * levelScale * mult).toInt()
        }
        val mana = when {
            slot == EquipSlot.TRINKET || slot == EquipSlot.HELM ||
                (armorWeight == ArmorWeight.CLOTH && slot in listOf(EquipSlot.BRACERS, EquipSlot.BOOTS)) ->
                (18 * levelScale * mult).toInt()
            else -> 0
        }
        val crit = if (rarity.ordinal >= ItemRarity.RARE.ordinal && (slot == EquipSlot.WEAPON || slot == EquipSlot.BRACERS)) {
            0.025f * rarity.ordinal
        } else 0f
        val dodge = if (rarity.ordinal >= ItemRarity.RARE.ordinal && (slot == EquipSlot.HELM || slot == EquipSlot.BOOTS)) {
            0.02f * rarity.ordinal
        } else 0f
        val speedPct = if (slot == EquipSlot.BOOTS && rarity.ordinal >= ItemRarity.RARE.ordinal) 0.04f * rarity.ordinal else 0f
        val lifesteal = if (rarity >= ItemRarity.LEGENDARY && slot == EquipSlot.WEAPON) 0.05f else 0f

        val goldValue = (12 * clampedLevel * mult).toInt()
        val classReqText = when (slot) {
            EquipSlot.WEAPON -> "Armamento: ${weaponCategory.label}."
            EquipSlot.TRINKET -> "Reliquia universal."
            else -> "Armadura de ${armorWeight.label}."
        }
        val masteryNote = if (rarity.maxLevelBypass > 0) {
            "$classReqText Equipable hasta ${rarity.maxLevelBypass} niveles antes."
        } else {
            "$classReqText Equipo estándar."
        }

        return EquipmentItem(
            id = forcedId ?: "eq_${System.nanoTime()}_${Random.nextInt(1000, 9999)}",
            name = itemName,
            slot = slot,
            rarity = rarity,
            requiredLevel = clampedLevel,
            attackBonus = atk,
            armorBonus = armor,
            hpBonus = hp,
            manaBonus = mana,
            critPctBonus = crit,
            dodgePctBonus = dodge,
            speedPctBonus = speedPct,
            lifestealPctBonus = lifesteal,
            setId = setChoice?.setId,
            setName = setChoice?.setName,
            goldValue = goldValue,
            description = masteryNote,
            armorWeight = if (slot != EquipSlot.WEAPON && slot != EquipSlot.TRINKET) armorWeight else null,
            weaponCategory = if (slot == EquipSlot.WEAPON) weaponCategory else null
        )
    }

    fun buildClassSetQuestRewards(
        questId: String,
        tier: Int,
        slots: List<EquipSlot>,
        heroClass: HeroClass,
        baseLevel: Int
    ): List<EquipmentItem> {
        val setDef = setDefForClass(heroClass, tier)
        val rarity = when (tier) {
            1 -> ItemRarity.RARE
            2, 3, 4 -> ItemRarity.UNIQUE_SET
            else -> ItemRarity.UNIQUE_SET
        }
        val reqLevel = when (tier) {
            1 -> baseLevel.coerceIn(3, 8)
            2 -> baseLevel.coerceIn(8, 15)
            3 -> baseLevel.coerceIn(16, 23)
            else -> baseLevel.coerceIn(24, MAX_PLAYER_LEVEL)
        }
        return slots.map { slot ->
            val fixedNamePrefix = when (slot) {
                EquipSlot.HELM -> when (heroClass.armorWeight) {
                    ArmorWeight.CLOTH -> "Capucha de Tela"
                    ArmorWeight.MAIL_LEATHER -> "Casco de Cuero"
                    ArmorWeight.PLATE -> "Casco de Placas"
                }
                EquipSlot.BRACERS -> when (heroClass.armorWeight) {
                    ArmorWeight.CLOTH -> "Brazales de Tela"
                    ArmorWeight.MAIL_LEATHER -> "Brazales de Cuero"
                    ArmorWeight.PLATE -> "Brazales de Placas"
                }
                EquipSlot.ARMOR -> when (heroClass.armorWeight) {
                    ArmorWeight.CLOTH -> "Pecho de Tela"
                    ArmorWeight.MAIL_LEATHER -> "Pecho de Cuero"
                    ArmorWeight.PLATE -> "Pecho de Placas"
                }
                EquipSlot.BOOTS -> when (heroClass.armorWeight) {
                    ArmorWeight.CLOTH -> "Botas de Tela"
                    ArmorWeight.MAIL_LEATHER -> "Botas de Cuero"
                    ArmorWeight.PLATE -> "Botas de Placas"
                }
                EquipSlot.WEAPON -> when (heroClass.weaponCategory) {
                    WeaponCategory.STAFF_GRIMOIRE -> "Báculo"
                    WeaponCategory.BOW_SPEAR -> "Arco Rúnico"
                    WeaponCategory.TWIN_DAGGERS -> "Dagas Gemelas"
                    WeaponCategory.RUNIC_AXE_TOTEM -> "Hacha Rúnica"
                    WeaponCategory.HEAVY_MELEE -> "Mandoble"
                }
                EquipSlot.TRINKET -> "Reliquia"
            }
            val generated = generateEquipment(
                targetLevel = reqLevel,
                rarity = rarity,
                heroClass = heroClass,
                forcedSlot = slot,
                forcedSetId = setDef.setId,
                forcedId = "${questId}_reward_${heroClass.name.lowercase()}_${slot.name.lowercase()}"
            )
            generated.copy(name = "$fixedNamePrefix (${setDef.setName})")
        }
    }

    fun adaptQuestItemForClass(item: EquipmentItem, heroClass: HeroClass): EquipmentItem {
        if (item.id == "q_reward_starter_weapon") {
            val wName = when (heroClass.weaponCategory) {
                WeaponCategory.STAFF_GRIMOIRE -> "Báculo del Recluta de Valoria"
                WeaponCategory.BOW_SPEAR -> "Arco del Recluta de Valoria"
                WeaponCategory.TWIN_DAGGERS -> "Dagas del Recluta de Valoria"
                WeaponCategory.RUNIC_AXE_TOTEM -> "Hacha del Recluta de Valoria"
                WeaponCategory.HEAVY_MELEE -> "Hoja del Recluta de Valoria"
            }
            return item.copy(
                name = wName,
                weaponCategory = heroClass.weaponCategory,
                description = "Armamento: ${heroClass.weaponCategory.label}. Equipable hasta 2 niveles antes."
            )
        }
        return item.copy(
            armorWeight = if (item.slot != EquipSlot.WEAPON && item.slot != EquipSlot.TRINKET) heroClass.armorWeight else null,
            weaponCategory = if (item.slot == EquipSlot.WEAPON) heroClass.weaponCategory else null
        )
    }

    val npcs: List<NpcDef> = listOf(
        NpcDef(
            id = "npc_vance",
            name = "Capitán Vance",
            title = "Comandante de Valoria",
            role = NpcRole.QUEST_GIVER,
            x = 12f,
            y = 10f,
            tunicColorHex = 0xFF2563EB,
            greeting = "¡Bienvenido a Valoria, aventurero! El reino necesita héroes que detengan la oscuridad."
        ),
        NpcDef(
            id = "npc_lyra",
            name = "Maestra Lyra",
            title = "Erudita Arcana",
            role = NpcRole.QUEST_GIVER,
            x = 16f,
            y = 12f,
            tunicColorHex = 0xFF9333EA,
            greeting = "Las ruinas y el pantano laten con energía antigua. Investiga con cautela."
        ),
        NpcDef(
            id = "npc_borin",
            name = "Herrero Borin",
            title = "Forja y Provisiones",
            role = NpcRole.MERCHANT_BLACKSMITH,
            x = 8f,
            y = 12f,
            tunicColorHex = 0xFFD97706,
            greeting = "Acero enano, pociones y cofres forjados para tu clase. Equípate para la batalla."
        ),
        NpcDef(
            id = "npc_elara",
            name = "Sacerdotisa Elara",
            title = "Fuente de Vida",
            role = NpcRole.HEALER_INNKEEPER,
            x = 11f,
            y = 15f,
            tunicColorHex = 0xFF10B981,
            greeting = "Que la luz restaure tus fuerzas. Tu vida y maná han sido completamente renovados."
        )
    )

    val quests: List<QuestDef> = listOf(
        QuestDef(
            id = "q_first_steps",
            title = "1. Primeros Pasos en Valoria",
            description = "Habla con el Herrero Borin en la plaza oeste para recibir tu armamento inicial de clase.",
            giverNpcId = "npc_vance",
            turnInNpcId = "npc_vance",
            requiredLevel = 1,
            prerequisiteQuestId = null,
            objectiveType = QuestObjectiveType.TALK_NPC,
            targetNpcId = "npc_borin",
            requiredCount = 1,
            rewardXp = 70,
            rewardGold = 45,
            rewardItem = EquipmentItem(
                id = "q_reward_starter_weapon",
                name = "Hoja del Recluta de Valoria",
                slot = EquipSlot.WEAPON,
                rarity = ItemRarity.RARE,
                requiredLevel = 3,
                attackBonus = 14,
                hpBonus = 25,
                critPctBonus = 0.04f,
                goldValue = 35,
                description = "Equipable hasta 2 niveles antes del requisito."
            )
        ),
        QuestDef(
            id = "q_emerald_wolves",
            title = "2. Set del Bosque (Casco y Brazales)",
            description = "Cruza al Bosque Esmeralda y derrota 4 Lobos Salvajes para abrirte paso hacia los enclaves.",
            giverNpcId = "npc_vance",
            turnInNpcId = "npc_vance",
            requiredLevel = 2,
            prerequisiteQuestId = "q_first_steps",
            objectiveType = QuestObjectiveType.KILL_ENEMY,
            targetEnemyKind = EnemyKind.WOLF,
            requiredCount = 4,
            rewardXp = 160,
            rewardGold = 80,
            rewardSetTier = 1,
            rewardSetSlots = listOf(EquipSlot.HELM, EquipSlot.BRACERS)
        ),
        QuestDef(
            id = "q_bandit_camp",
            title = "3. Set del Bosque (Pecho y Botas)",
            description = "Elimina 3 Bandidos del Bosque en el Enclave de los Bandidos y reporta a Maestra Lyra.",
            giverNpcId = "npc_vance",
            turnInNpcId = "npc_lyra",
            requiredLevel = 4,
            prerequisiteQuestId = "q_emerald_wolves",
            objectiveType = QuestObjectiveType.KILL_ENEMY,
            targetEnemyKind = EnemyKind.BANDIT,
            requiredCount = 3,
            rewardXp = 260,
            rewardGold = 120,
            rewardSetTier = 1,
            rewardSetSlots = listOf(EquipSlot.ARMOR, EquipSlot.BOOTS)
        ),
        QuestDef(
            id = "q_forest_enclaves",
            title = "4. Enclaves del Bosque (Arma de Set)",
            description = "Despeja 3 pequeños enclaves del Bosque Esmeralda (mata sus mobs raciales y minijefe) para desbloquear el Portón a las Ruinas.",
            giverNpcId = "npc_vance",
            turnInNpcId = "npc_vance",
            requiredLevel = 5,
            prerequisiteQuestId = "q_emerald_wolves",
            objectiveType = QuestObjectiveType.CLEAR_ENCLAVES,
            targetZone = ZoneId.EMERALD_FOREST,
            requiredCount = 3,
            rewardXp = 380,
            rewardGold = 180,
            rewardSetTier = 1,
            rewardSetSlots = listOf(EquipSlot.WEAPON)
        ),
        QuestDef(
            id = "q_twilight_ruins",
            title = "5. Set de Ruinas (Casco y Brazales)",
            description = "Cruza el Portón hacia las Ruinas del Crepúsculo y derrota 4 Esqueletos Guerreros en sus enclaves.",
            giverNpcId = "npc_lyra",
            turnInNpcId = "npc_lyra",
            requiredLevel = 8,
            prerequisiteQuestId = "q_bandit_camp",
            objectiveType = QuestObjectiveType.KILL_ENEMY,
            targetEnemyKind = EnemyKind.SKELETON,
            requiredCount = 4,
            rewardXp = 480,
            rewardGold = 210,
            rewardSetTier = 2,
            rewardSetSlots = listOf(EquipSlot.HELM, EquipSlot.BRACERS)
        ),
        QuestDef(
            id = "q_ruins_enclaves",
            title = "6. Enclaves del Crepúsculo (Set Completo)",
            description = "Despeja 3 enclaves de las Ruinas del Crepúsculo venciendo a sus guardias y minijefes para recibir Pecho, Botas y Arma de Set Único.",
            giverNpcId = "npc_lyra",
            turnInNpcId = "npc_lyra",
            requiredLevel = 11,
            prerequisiteQuestId = "q_twilight_ruins",
            objectiveType = QuestObjectiveType.CLEAR_ENCLAVES,
            targetZone = ZoneId.TWILIGHT_RUINS,
            requiredCount = 3,
            rewardXp = 720,
            rewardGold = 320,
            rewardSetTier = 2,
            rewardSetSlots = listOf(EquipSlot.ARMOR, EquipSlot.BOOTS, EquipSlot.WEAPON)
        ),
        QuestDef(
            id = "q_cursed_swamp",
            title = "7. Set del Pantano (Casco y Brazales)",
            description = "Avanza al Pantano Maldito y derrota 3 Trolls de Ciénaga en el enclave de Grimgor.",
            giverNpcId = "npc_lyra",
            turnInNpcId = "npc_vance",
            requiredLevel = 16,
            prerequisiteQuestId = "q_twilight_ruins",
            objectiveType = QuestObjectiveType.KILL_ENEMY,
            targetEnemyKind = EnemyKind.TROLL,
            requiredCount = 3,
            rewardXp = 920,
            rewardGold = 400,
            rewardSetTier = 3,
            rewardSetSlots = listOf(EquipSlot.HELM, EquipSlot.BRACERS)
        ),
        QuestDef(
            id = "q_swamp_enclaves",
            title = "8. Enclaves del Pantano (Pecho, Botas y Arma)",
            description = "Despeja 3 enclaves del Pantano Maldito (mobs raciales + minijefes) para abrir el Portón a la Cumbre y completar tu Set Épico.",
            giverNpcId = "npc_lyra",
            turnInNpcId = "npc_vance",
            requiredLevel = 19,
            prerequisiteQuestId = "q_cursed_swamp",
            objectiveType = QuestObjectiveType.CLEAR_ENCLAVES,
            targetZone = ZoneId.CURSED_SWAMP,
            requiredCount = 3,
            rewardXp = 1350,
            rewardGold = 580,
            rewardSetTier = 3,
            rewardSetSlots = listOf(EquipSlot.ARMOR, EquipSlot.BOOTS, EquipSlot.WEAPON)
        ),
        QuestDef(
            id = "q_dragon_enclaves",
            title = "9. Vanguardia de la Cumbre (Casco, Brazales y Botas)",
            description = "Despeja 3 enclaves dracónicos en la Cumbre del Dragón para romper el sello hacia Ignis.",
            giverNpcId = "npc_vance",
            turnInNpcId = "npc_vance",
            requiredLevel = 24,
            prerequisiteQuestId = "q_cursed_swamp",
            objectiveType = QuestObjectiveType.CLEAR_ENCLAVES,
            targetZone = ZoneId.DRAGON_SUMMIT,
            requiredCount = 3,
            rewardXp = 1850,
            rewardGold = 820,
            rewardSetTier = 4,
            rewardSetSlots = listOf(EquipSlot.HELM, EquipSlot.BRACERS, EquipSlot.BOOTS)
        ),
        QuestDef(
            id = "q_dragon_summit",
            title = "10. El Wyrm de la Cumbre (Set Supremo)",
            description = "Entra al santuario final de la Cumbre del Dragón y derrota a Ignis, Wyrm Ancestral.",
            giverNpcId = "npc_vance",
            turnInNpcId = "npc_vance",
            requiredLevel = 27,
            prerequisiteQuestId = "q_cursed_swamp",
            objectiveType = QuestObjectiveType.KILL_ENEMY,
            targetEnemyKind = EnemyKind.DRAGON_BOSS,
            requiredCount = 1,
            rewardXp = 2600,
            rewardGold = 1200,
            rewardSetTier = 4,
            rewardSetSlots = listOf(EquipSlot.ARMOR, EquipSlot.WEAPON)
        )
    )
}
