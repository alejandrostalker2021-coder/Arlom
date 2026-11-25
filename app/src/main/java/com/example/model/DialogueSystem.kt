package com.example.model

enum class DialogueEffectType {
    NONE,
    ACCEPT_QUEST,
    ACCEPT_WITH_SUPPLIES,
    ADVANCE_TALK_QUEST,
    TURN_IN_QUEST,
    HEAL_RESTORE,
    OPEN_FORGE,
    CLOSE_DIALOG
}

enum class DialogueTagKind(val label: String) {
    QUEST("Misión"),
    TURN_IN("Entregar"),
    OBJECTIVE("Objetivo"),
    PERSUASION("Persuasión"),
    FORGE("Forja"),
    BLESSING("Sanación"),
    LORE("Sabiduría"),
    FAREWELL("Salir")
}

data class DialogueEffect(
    val type: DialogueEffectType = DialogueEffectType.NONE,
    val questId: String? = null,
    val bonusGold: Int = 0,
    val bonusPotions: Int = 0
)

data class DialogueOption(
    val id: String,
    val text: String,
    val tag: DialogueTagKind = DialogueTagKind.LORE,
    val nextNodeId: String? = null,
    val effect: DialogueEffect = DialogueEffect(),
    val isAvailable: (CharacterProfile) -> Boolean = { true }
)

data class DialogueNode(
    val id: String,
    val speakerName: String,
    val speakerTitle: String,
    val text: String,
    val options: List<DialogueOption>
)

data class NpcDialogueTree(
    val npcId: String,
    val nodes: Map<String, DialogueNode>,
    val rootSelector: (CharacterProfile) -> String
)

data class DialogueOutcome(
    val updatedProfile: CharacterProfile,
    val feedbackMessage: String?,
    val logMessage: String?,
    val floatingText: String?,
    val floatingKind: FloatingTextKind = FloatingTextKind.STATUS,
    val leveledUp: Boolean = false,
    val restoreVitals: Boolean = false,
    val openForge: Boolean = false,
    val shouldClose: Boolean = false
)

object DialogueEngine {

    fun isQuestAvailable(profile: CharacterProfile, questId: String): Boolean {
        val q = LootAndQuestData.quests.firstOrNull { it.id == questId } ?: return false
        return profile.quests.none { it.questId == q.id } &&
            (q.prerequisiteQuestId == null || profile.quests.any { it.questId == q.prerequisiteQuestId && it.turnedIn })
    }

    fun isQuestActiveIncomplete(profile: CharacterProfile, questId: String): Boolean {
        return profile.quests.any { it.questId == questId && !it.completed && !it.turnedIn }
    }

    fun isQuestReadyToTurnIn(profile: CharacterProfile, questId: String): Boolean {
        return profile.quests.any { it.questId == questId && it.completed && !it.turnedIn }
    }

    fun applyOption(
        profile: CharacterProfile,
        npcId: String,
        option: DialogueOption
    ): DialogueOutcome {
        val effect = option.effect
        return when (effect.type) {
            DialogueEffectType.NONE -> DialogueOutcome(
                updatedProfile = profile,
                feedbackMessage = null,
                logMessage = null,
                floatingText = null
            )

            DialogueEffectType.CLOSE_DIALOG -> DialogueOutcome(
                updatedProfile = profile,
                feedbackMessage = null,
                logMessage = null,
                floatingText = null,
                shouldClose = true
            )

            DialogueEffectType.OPEN_FORGE -> DialogueOutcome(
                updatedProfile = profile,
                feedbackMessage = "Forja y provisiones abiertas.",
                logMessage = null,
                floatingText = null,
                openForge = true
            )

            DialogueEffectType.HEAL_RESTORE -> DialogueOutcome(
                updatedProfile = profile,
                feedbackMessage = "La Fuente de Vida restaura por completo tu salud y maná.",
                logMessage = "Bendición de Elara: salud y maná restaurados.",
                floatingText = "¡Vida y Maná Restaurados!",
                floatingKind = FloatingTextKind.HEAL,
                restoreVitals = true
            )

            DialogueEffectType.ACCEPT_QUEST -> {
                val quest = effect.questId?.let { id ->
                    LootAndQuestData.quests.firstOrNull { it.id == id }
                }
                if (quest == null || profile.quests.any { it.questId == quest.id }) {
                    DialogueOutcome(profile, null, null, null)
                } else {
                    val updated = profile.copy(
                        quests = profile.quests + QuestProgress(questId = quest.id)
                    )
                    DialogueOutcome(
                        updatedProfile = updated,
                        feedbackMessage = "Misión aceptada: ${quest.title}",
                        logMessage = "Misión aceptada: ${quest.title}",
                        floatingText = "¡Nueva Misión!",
                        floatingKind = FloatingTextKind.LOOT
                    )
                }
            }

            DialogueEffectType.ACCEPT_WITH_SUPPLIES -> {
                val quest = effect.questId?.let { id ->
                    LootAndQuestData.quests.firstOrNull { it.id == id }
                }
                if (quest == null || profile.quests.any { it.questId == quest.id }) {
                    DialogueOutcome(profile, null, null, null)
                } else {
                    val updated = profile.copy(
                        gold = profile.gold + effect.bonusGold,
                        potions = profile.potions + effect.bonusPotions,
                        quests = profile.quests + QuestProgress(questId = quest.id)
                    )
                    val supplyParts = buildList {
                        if (effect.bonusPotions > 0) add("+${effect.bonusPotions} Poción")
                        if (effect.bonusGold > 0) add("+${effect.bonusGold} Oro")
                    }.joinToString(", ")
                    val detail = if (supplyParts.isNotEmpty()) " ($supplyParts)" else ""
                    DialogueOutcome(
                        updatedProfile = updated,
                        feedbackMessage = "Misión aceptada: ${quest.title}$detail",
                        logMessage = "Persuasión exitosa: ${quest.title}$detail",
                        floatingText = "¡Misión + Provisiones!",
                        floatingKind = FloatingTextKind.LOOT
                    )
                }
            }

            DialogueEffectType.ADVANCE_TALK_QUEST -> {
                var completedTitle: String? = null
                var changed = false
                val nextQuests = profile.quests.map { qp ->
                    val def = LootAndQuestData.quests.firstOrNull { it.id == qp.questId }
                    val matchesQuest = effect.questId == null || qp.questId == effect.questId
                    if (matchesQuest &&
                        !qp.completed &&
                        def?.objectiveType == QuestObjectiveType.TALK_NPC &&
                        def.targetNpcId == npcId
                    ) {
                        completedTitle = def.title
                        changed = true
                        qp.copy(currentCount = def.requiredCount.coerceAtLeast(1), completed = true)
                    } else {
                        qp
                    }
                }
                if (!changed) {
                    DialogueOutcome(profile, null, null, null)
                } else {
                    val updated = profile.copy(
                        gold = profile.gold + effect.bonusGold,
                        potions = profile.potions + effect.bonusPotions,
                        quests = nextQuests
                    )
                    val supplyParts = buildList {
                        if (effect.bonusPotions > 0) add("+${effect.bonusPotions} Poción")
                        if (effect.bonusGold > 0) add("+${effect.bonusGold} Oro")
                    }.joinToString(", ")
                    val bonusNote = if (supplyParts.isNotEmpty()) " | Extra: $supplyParts" else ""
                    DialogueOutcome(
                        updatedProfile = updated,
                        feedbackMessage = "Objetivo completado: $completedTitle$bonusNote",
                        logMessage = "Misión lista para entregar: $completedTitle$bonusNote",
                        floatingText = "¡Objetivo completado!",
                        floatingKind = FloatingTextKind.LOOT
                    )
                }
            }

            DialogueEffectType.TURN_IN_QUEST -> {
                val quest = effect.questId?.let { id ->
                    LootAndQuestData.quests.firstOrNull { it.id == id }
                }
                val qp = quest?.let { q ->
                    profile.quests.firstOrNull { it.questId == q.id && it.completed && !it.turnedIn }
                }
                if (quest == null || qp == null) {
                    DialogueOutcome(profile, null, null, null)
                } else {
                    var nextLevel = profile.level
                    var nextXp = profile.xp + quest.rewardXp
                    while (nextLevel < MAX_PLAYER_LEVEL && nextXp >= LootAndQuestData.xpToNextLevel(nextLevel)) {
                        nextXp -= LootAndQuestData.xpToNextLevel(nextLevel)
                        nextLevel++
                    }
                    val leveled = nextLevel > profile.level
                    val nextInv = if (quest.rewardItem != null && profile.inventory.size < 36) {
                        profile.inventory + quest.rewardItem
                    } else {
                        profile.inventory
                    }
                    val nextQuests = profile.quests.map {
                        if (it.questId == quest.id) qp.copy(turnedIn = true) else it
                    }
                    val updated = profile.copy(
                        level = nextLevel,
                        xp = if (nextLevel >= MAX_PLAYER_LEVEL) 0 else nextXp,
                        gold = profile.gold + quest.rewardGold,
                        inventory = nextInv,
                        quests = nextQuests
                    )
                    val itemText = quest.rewardItem?.let { " + ${it.name}" } ?: ""
                    DialogueOutcome(
                        updatedProfile = updated,
                        feedbackMessage = "¡Misión completada! +${quest.rewardXp} XP, +${quest.rewardGold} Oro$itemText",
                        logMessage = "¡Misión completada! +${quest.rewardXp} XP, +${quest.rewardGold} Oro",
                        floatingText = if (leveled) "¡NIVEL $nextLevel!" else "+${quest.rewardXp} XP  +${quest.rewardGold}g",
                        floatingKind = FloatingTextKind.LOOT,
                        leveledUp = leveled
                    )
                }
            }
        }
    }
}

object DialogueCatalog {

    fun treeForNpc(npc: NpcDef): NpcDialogueTree {
        return when (npc.id) {
            "npc_vance" -> vanceTree(npc)
            "npc_borin" -> borinTree(npc)
            "npc_lyra" -> lyraTree(npc)
            "npc_elara" -> elaraTree(npc)
            else -> fallbackTree(npc)
        }
    }

    private fun vanceTree(npc: NpcDef): NpcDialogueTree {
        val nodes = mapOf(
            "vance_root" to DialogueNode(
                id = "vance_root",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Saludos, campeón. Las patrullas de Valoria vigilan el bosque, pero las bestias están inquietas. ¿En qué podemos avanzar hoy?",
                options = listOf(
                    DialogueOption(
                        id = "v_turnin_first",
                        text = "Ya hablé con el Herrero Borin sobre los suministros.",
                        tag = DialogueTagKind.TURN_IN,
                        nextNodeId = "vance_after_first",
                        effect = DialogueEffect(DialogueEffectType.TURN_IN_QUEST, questId = "q_first_steps"),
                        isAvailable = { DialogueEngine.isQuestReadyToTurnIn(it, "q_first_steps") }
                    ),
                    DialogueOption(
                        id = "v_turnin_wolves",
                        text = "He abatido a los Lobos Salvajes del Bosque Esmeralda.",
                        tag = DialogueTagKind.TURN_IN,
                        nextNodeId = "vance_after_wolves",
                        effect = DialogueEffect(DialogueEffectType.TURN_IN_QUEST, questId = "q_emerald_wolves"),
                        isAvailable = { DialogueEngine.isQuestReadyToTurnIn(it, "q_emerald_wolves") }
                    ),
                    DialogueOption(
                        id = "v_turnin_swamp",
                        text = "Los Trolls del Pantano Maldito han sido derrotados.",
                        tag = DialogueTagKind.TURN_IN,
                        nextNodeId = "vance_after_swamp",
                        effect = DialogueEffect(DialogueEffectType.TURN_IN_QUEST, questId = "q_cursed_swamp"),
                        isAvailable = { DialogueEngine.isQuestReadyToTurnIn(it, "q_cursed_swamp") }
                    ),
                    DialogueOption(
                        id = "v_turnin_dragon",
                        text = "¡Ignis, el Wyrm Ancestral, ha caído en la Cumbre!",
                        tag = DialogueTagKind.TURN_IN,
                        nextNodeId = "vance_victory",
                        effect = DialogueEffect(DialogueEffectType.TURN_IN_QUEST, questId = "q_dragon_summit"),
                        isAvailable = { DialogueEngine.isQuestReadyToTurnIn(it, "q_dragon_summit") }
                    ),
                    DialogueOption(
                        id = "v_offer_first",
                        text = "¿Cuál es mi primera tarea en Valoria?",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_first",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_first_steps") }
                    ),
                    DialogueOption(
                        id = "v_offer_wolves",
                        text = "¿Qué amenaza acecha en el Bosque Esmeralda?",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_wolves",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_emerald_wolves") }
                    ),
                    DialogueOption(
                        id = "v_offer_bandits",
                        text = "Escuché que hay bandidos atacando las caravanas del sur.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_bandits",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_bandit_camp") }
                    ),
                    DialogueOption(
                        id = "v_offer_dragon",
                        text = "Háblame de la Cumbre del Dragón y de Ignis.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_dragon",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_dragon_summit") }
                    ),
                    DialogueOption(
                        id = "v_status_first",
                        text = "¿Dónde encuentro exactamente al Herrero Borin?",
                        tag = DialogueTagKind.OBJECTIVE,
                        nextNodeId = "vance_hint_borin",
                        isAvailable = { DialogueEngine.isQuestActiveIncomplete(it, "q_first_steps") }
                    ),
                    DialogueOption(
                        id = "v_lore",
                        text = "¿Qué peligros rodean la Aldea de Valoria?",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_lore"
                    ),
                    DialogueOption(
                        id = "v_close",
                        text = "Debo volver al combate. Hasta pronto.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "vance_brief_first" to DialogueNode(
                id = "vance_brief_first",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Antes de cruzar el puente hacia el este, habla con el Herrero Borin en la plaza oeste para verificar nuestras reservas de acero.",
                options = listOf(
                    DialogueOption(
                        id = "v_accept_first",
                        text = "Iré a hablar con Borin ahora mismo.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_root",
                        effect = DialogueEffect(DialogueEffectType.ACCEPT_QUEST, questId = "q_first_steps")
                    ),
                    DialogueOption(
                        id = "v_back_from_first",
                        text = "Volver atrás.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_after_first" to DialogueNode(
                id = "vance_after_first",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Excelente trabajo. Con la forja lista, podemos contraatacar. Las manadas de Lobos Salvajes han estado rondando el Bosque Esmeralda al este.",
                options = listOf(
                    DialogueOption(
                        id = "v_next_wolves",
                        text = "¿Qué necesitas que haga en el Bosque Esmeralda?",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_wolves",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_emerald_wolves") }
                    ),
                    DialogueOption(
                        id = "v_after_first_back",
                        text = "Volver a conversar.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_brief_wolves" to DialogueNode(
                id = "vance_brief_wolves",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Cruza el puente oriental hacia el Bosque Esmeralda y abate 4 Lobos Salvajes para despejar la ruta comercial. ¿Cómo deseas prepararte?",
                options = listOf(
                    DialogueOption(
                        id = "v_accept_wolves_std",
                        text = "Acepto el encargo. Despejaré el bosque sin demora.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_root",
                        effect = DialogueEffect(DialogueEffectType.ACCEPT_QUEST, questId = "q_emerald_wolves")
                    ),
                    DialogueOption(
                        id = "v_accept_wolves_persuade",
                        text = "[Persuasión] Si voy a internarme solo, dame raciones de guardia.",
                        tag = DialogueTagKind.PERSUASION,
                        nextNodeId = "vance_persuade_ok",
                        effect = DialogueEffect(
                            type = DialogueEffectType.ACCEPT_WITH_SUPPLIES,
                            questId = "q_emerald_wolves",
                            bonusGold = 15,
                            bonusPotions = 1
                        )
                    ),
                    DialogueOption(
                        id = "v_wolves_back",
                        text = "Déjame pensarlo primero.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_after_wolves" to DialogueNode(
                id = "vance_after_wolves",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Valoria vuelve a respirar tranquila. Sin embargo, nuestros exploradores avistaron bandidos emboscando caravanas en el sendero sur.",
                options = listOf(
                    DialogueOption(
                        id = "v_next_bandits",
                        text = "Cuéntame sobre esos bandidos.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_bandits",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_bandit_camp") }
                    ),
                    DialogueOption(
                        id = "v_wolves_done_back",
                        text = "Volver al menú de diálogo.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_brief_bandits" to DialogueNode(
                id = "vance_brief_bandits",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Elimina a 3 Bandidos del Bosque hacia el sur. Cuando acabes, informa directamente a la Maestra Lyra; ella sospecha magia oscura tras los asaltos.",
                options = listOf(
                    DialogueOption(
                        id = "v_accept_bandits_std",
                        text = "Entendido. Neutralizaré a los bandidos e informaré a Lyra.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_root",
                        effect = DialogueEffect(DialogueEffectType.ACCEPT_QUEST, questId = "q_bandit_camp")
                    ),
                    DialogueOption(
                        id = "v_accept_bandits_persuade",
                        text = "[Persuasión] Necesito refuerzos médicos antes de enfrentar su campamento.",
                        tag = DialogueTagKind.PERSUASION,
                        nextNodeId = "vance_persuade_ok",
                        effect = DialogueEffect(
                            type = DialogueEffectType.ACCEPT_WITH_SUPPLIES,
                            questId = "q_bandit_camp",
                            bonusGold = 25,
                            bonusPotions = 1
                        )
                    ),
                    DialogueOption(
                        id = "v_bandits_back",
                        text = "Volver atrás.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_after_swamp" to DialogueNode(
                id = "vance_after_swamp",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Has demostrado un temple inquebrantable en el pantano. Solo queda la amenaza suprema: Ignis despierta en la Cumbre del Dragón.",
                options = listOf(
                    DialogueOption(
                        id = "v_next_dragon",
                        text = "Estoy listo para saber cómo enfrentar a Ignis.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_brief_dragon",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_dragon_summit") }
                    ),
                    DialogueOption(
                        id = "v_swamp_done_back",
                        text = "Volver al diálogo.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_brief_dragon" to DialogueNode(
                id = "vance_brief_dragon",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Asciende al extremo este hacia la Cumbre del Dragón (Nivel recomendado 20+) y derrota a Ignis, Wyrm Ancestral. La gloria eterna de Valoria aguarda.",
                options = listOf(
                    DialogueOption(
                        id = "v_accept_dragon",
                        text = "Juramento de acero: ¡acabaré con el Wyrm Ancestral!",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "vance_root",
                        effect = DialogueEffect(DialogueEffectType.ACCEPT_QUEST, questId = "q_dragon_summit")
                    ),
                    DialogueOption(
                        id = "v_accept_dragon_supplies",
                        text = "[Persuasión] Dame el último cofre de pociones de la guardia real.",
                        tag = DialogueTagKind.PERSUASION,
                        nextNodeId = "vance_persuade_ok",
                        effect = DialogueEffect(
                            type = DialogueEffectType.ACCEPT_WITH_SUPPLIES,
                            questId = "q_dragon_summit",
                            bonusGold = 60,
                            bonusPotions = 2
                        )
                    ),
                    DialogueOption(
                        id = "v_dragon_back",
                        text = "Me prepararé mejor antes de aceptar.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_persuade_ok" to DialogueNode(
                id = "vance_persuade_ok",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Tienes razón, un soldado bien abastecido vuelve vivo. Toma estas provisiones de la reserva de la guarnición y cumple la misión.",
                options = listOf(
                    DialogueOption(
                        id = "v_persuade_continue",
                        text = "Gracias, Capitán. Seguiré conversando.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    ),
                    DialogueOption(
                        id = "v_persuade_close",
                        text = "¡En marcha!",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "vance_hint_borin" to DialogueNode(
                id = "vance_hint_borin",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Borin trabaja en su yunque hacia el oeste de la plaza (cerca de las coordenadas 8, 12). Habla con él y confirma el inventario de armas.",
                options = listOf(
                    DialogueOption(
                        id = "v_hint_back",
                        text = "Entendido, iré hacia el oeste.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_lore" to DialogueNode(
                id = "vance_lore",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Valoria es nuestro último bastión. Al este yace el Bosque Esmeralda; al noreste las Ruinas del Crepúsculo; al sur el Pantano Maldito; y al extremo este arde la Cumbre del Dragón.",
                options = listOf(
                    DialogueOption(
                        id = "v_lore_back",
                        text = "Gracias por la orientación táctica.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            ),
            "vance_victory" to DialogueNode(
                id = "vance_victory",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "¡Los bardos cantarán tu nombre durante siglos! Has salvado a Aethelgard de la sombra de Ignis.",
                options = listOf(
                    DialogueOption(
                        id = "v_victory_back",
                        text = "Ha sido un honor defender Valoria.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "vance_root"
                    )
                )
            )
        )

        return NpcDialogueTree(
            npcId = npc.id,
            nodes = nodes,
            rootSelector = { "vance_root" }
        )
    }

    private fun borinTree(npc: NpcDef): NpcDialogueTree {
        val nodes = mapOf(
            "borin_root" to DialogueNode(
                id = "borin_root",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "¡Ah, el sonido del martillo sobre el yunque! Acero enano, pociones frescas y cofres forjados a tu servicio.",
                options = listOf(
                    DialogueOption(
                        id = "b_vance_quest",
                        text = "Vance me envía a revisar el inventario de suministros.",
                        tag = DialogueTagKind.OBJECTIVE,
                        nextNodeId = "borin_vance_order",
                        isAvailable = { DialogueEngine.isQuestActiveIncomplete(it, "q_first_steps") }
                    ),
                    DialogueOption(
                        id = "b_open_forge",
                        text = "Muéstrame la forja, pociones y cofres.",
                        tag = DialogueTagKind.FORGE,
                        effect = DialogueEffect(DialogueEffectType.OPEN_FORGE)
                    ),
                    DialogueOption(
                        id = "b_craft_lore",
                        text = "¿Cómo funciona la maestría de equipo y los Sets Únicos?",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "borin_craft_lore"
                    ),
                    DialogueOption(
                        id = "b_close",
                        text = "Nos vemos luego, maestro herrero.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "borin_vance_order" to DialogueNode(
                id = "borin_vance_order",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "¡Ja! El viejo Vance siempre desconfía de mis cuentas. Dile que tenemos veinte hojas templadas listas. ¿Necesitas algo más antes de volver con él?",
                options = listOf(
                    DialogueOption(
                        id = "b_confirm_supplies",
                        text = "Todo en orden, Borin. Informaré al Capitán Vance.",
                        tag = DialogueTagKind.OBJECTIVE,
                        nextNodeId = "borin_supplies_done",
                        effect = DialogueEffect(DialogueEffectType.ADVANCE_TALK_QUEST, questId = "q_first_steps")
                    ),
                    DialogueOption(
                        id = "b_persuade_supplies",
                        text = "[Persuasión] Dame una poción de muestra y unas monedas para probar la calidad.",
                        tag = DialogueTagKind.PERSUASION,
                        nextNodeId = "borin_supplies_bonus",
                        effect = DialogueEffect(
                            type = DialogueEffectType.ADVANCE_TALK_QUEST,
                            questId = "q_first_steps",
                            bonusGold = 15,
                            bonusPotions = 1
                        )
                    )
                )
            ),
            "borin_supplies_done" to DialogueNode(
                id = "borin_supplies_done",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "¡Así se habla! Vuelve con el Capitán Vance para reclamar tu hoja de recluta.",
                options = listOf(
                    DialogueOption(
                        id = "b_done_forge",
                        text = "Ya que estoy aquí, quiero ver la forja.",
                        tag = DialogueTagKind.FORGE,
                        nextNodeId = "borin_root",
                        effect = DialogueEffect(DialogueEffectType.OPEN_FORGE)
                    ),
                    DialogueOption(
                        id = "b_done_close",
                        text = "Voy hacia donde está Vance.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "borin_supplies_bonus" to DialogueNode(
                id = "borin_supplies_bonus",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "¡Tienes labia de mercader! Toma una poción y 15 piezas de oro, pero dile a Vance que mi forja es impecable.",
                options = listOf(
                    DialogueOption(
                        id = "b_bonus_back",
                        text = "Trato hecho, Borin.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "borin_root"
                    ),
                    DialogueOption(
                        id = "b_bonus_close",
                        text = "Iré a entregar el reporte.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "borin_craft_lore" to DialogueNode(
                id = "borin_craft_lore",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Las piezas Raras pueden equiparse 2 niveles antes, las Épicas 4 niveles antes, y las Legendarias o de Set Único hasta 6 niveles antes de su requisito. ¡Combina piezas del mismo Set para bonos masivos!",
                options = listOf(
                    DialogueOption(
                        id = "b_lore_back",
                        text = "Buen consejo, gracias.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "borin_root"
                    )
                )
            )
        )
        return NpcDialogueTree(
            npcId = npc.id,
            nodes = nodes,
            rootSelector = { "borin_root" }
        )
    }

    private fun lyraTree(npc: NpcDef): NpcDialogueTree {
        val nodes = mapOf(
            "lyra_root" to DialogueNode(
                id = "lyra_root",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Las líneas ley vibran con una perturbación inquietante. ¿Traes noticias de las rutas o buscas guía arcana?",
                options = listOf(
                    DialogueOption(
                        id = "l_turnin_bandits",
                        text = "Derroté a los bandidos del sendero sur.",
                        tag = DialogueTagKind.TURN_IN,
                        nextNodeId = "lyra_after_bandits",
                        effect = DialogueEffect(DialogueEffectType.TURN_IN_QUEST, questId = "q_bandit_camp"),
                        isAvailable = { DialogueEngine.isQuestReadyToTurnIn(it, "q_bandit_camp") }
                    ),
                    DialogueOption(
                        id = "l_turnin_ruins",
                        text = "Los Esqueletos Guerreros de las Ruinas han caído.",
                        tag = DialogueTagKind.TURN_IN,
                        nextNodeId = "lyra_after_ruins",
                        effect = DialogueEffect(DialogueEffectType.TURN_IN_QUEST, questId = "q_twilight_ruins"),
                        isAvailable = { DialogueEngine.isQuestReadyToTurnIn(it, "q_twilight_ruins") }
                    ),
                    DialogueOption(
                        id = "l_offer_ruins",
                        text = "¿Qué descubriste sobre las Ruinas del Crepúsculo?",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "lyra_brief_ruins",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_twilight_ruins") }
                    ),
                    DialogueOption(
                        id = "l_offer_swamp",
                        text = "¿Cuál es el origen de la corrupción en el Pantano Maldito?",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "lyra_brief_swamp",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_cursed_swamp") }
                    ),
                    DialogueOption(
                        id = "l_lore",
                        text = "Háblame sobre los nexos mágicos de Aethelgard.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_lore"
                    ),
                    DialogueOption(
                        id = "l_close",
                        text = "Seguiré investigando. Hasta pronto.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "lyra_after_bandits" to DialogueNode(
                id = "lyra_after_bandits",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Tus esfuerzos han salvado las rutas del sur. Mis cristales revelan que los bandidos huían de algo peor en las Ruinas del Crepúsculo.",
                options = listOf(
                    DialogueOption(
                        id = "l_next_ruins",
                        text = "¿Qué hay en las Ruinas del Crepúsculo?",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "lyra_brief_ruins",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_twilight_ruins") }
                    ),
                    DialogueOption(
                        id = "l_bandits_back",
                        text = "Volver a conversar.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_root"
                    )
                )
            ),
            "lyra_brief_ruins" to DialogueNode(
                id = "lyra_brief_ruins",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Al noreste, los muertos vivientes protegen antiguos artefactos. Derrota a 4 Esqueletos Guerreros y regresa conmigo.",
                options = listOf(
                    DialogueOption(
                        id = "l_accept_ruins_std",
                        text = "Ir hacia las Ruinas del Crepúsculo y purgarlas.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "lyra_root",
                        effect = DialogueEffect(DialogueEffectType.ACCEPT_QUEST, questId = "q_twilight_ruins")
                    ),
                    DialogueOption(
                        id = "l_accept_ruins_persuade",
                        text = "[Persuasión] Necesito un tónico arcano de tu laboratorio para resistir las ruinas.",
                        tag = DialogueTagKind.PERSUASION,
                        nextNodeId = "lyra_persuade_ok",
                        effect = DialogueEffect(
                            type = DialogueEffectType.ACCEPT_WITH_SUPPLIES,
                            questId = "q_twilight_ruins",
                            bonusGold = 25,
                            bonusPotions = 1
                        )
                    ),
                    DialogueOption(
                        id = "l_ruins_back",
                        text = "Volver atrás.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_root"
                    )
                )
            ),
            "lyra_after_ruins" to DialogueNode(
                id = "lyra_after_ruins",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Extraordinario. El sello de las ruinas indica que el miasma fluye desde el Pantano Maldito al sureste.",
                options = listOf(
                    DialogueOption(
                        id = "l_next_swamp",
                        text = "Dime qué debemos hacer en el Pantano Maldito.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "lyra_brief_swamp",
                        isAvailable = { DialogueEngine.isQuestAvailable(it, "q_cursed_swamp") }
                    ),
                    DialogueOption(
                        id = "l_ruins_done_back",
                        text = "Volver a conversar.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_root"
                    )
                )
            ),
            "lyra_brief_swamp" to DialogueNode(
                id = "lyra_brief_swamp",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Viaja al Pantano Maldito y derrota a 3 Trolls de Ciénaga que alimentan la maldición. Luego presenta tu informe final ante el Capitán Vance.",
                options = listOf(
                    DialogueOption(
                        id = "l_accept_swamp_std",
                        text = "Purgaré el Pantano Maldito y avisaré a Vance.",
                        tag = DialogueTagKind.QUEST,
                        nextNodeId = "lyra_root",
                        effect = DialogueEffect(DialogueEffectType.ACCEPT_QUEST, questId = "q_cursed_swamp")
                    ),
                    DialogueOption(
                        id = "l_accept_swamp_persuade",
                        text = "[Persuasión] Comparte tus reservas de alquimia para el viaje al pantano.",
                        tag = DialogueTagKind.PERSUASION,
                        nextNodeId = "lyra_persuade_ok",
                        effect = DialogueEffect(
                            type = DialogueEffectType.ACCEPT_WITH_SUPPLIES,
                            questId = "q_cursed_swamp",
                            bonusGold = 40,
                            bonusPotions = 2
                        )
                    ),
                    DialogueOption(
                        id = "l_swamp_back",
                        text = "Aún debo equiparme mejor.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_root"
                    )
                )
            ),
            "lyra_persuade_ok" to DialogueNode(
                id = "lyra_persuade_ok",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Tu prudencia es digna de un erudito. Toma estas reservas alquímicas y mantén la mente despejada.",
                options = listOf(
                    DialogueOption(
                        id = "l_persuade_back",
                        text = "Gracias, Maestra Lyra.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_root"
                    )
                )
            ),
            "lyra_lore" to DialogueNode(
                id = "lyra_lore",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Los dragones antiguos ligaron su aliento a las montañas del este. Mientras Ignis permanezca despierto, los muertos no descansarán en paz.",
                options = listOf(
                    DialogueOption(
                        id = "l_lore_back",
                        text = "Comprendo. Volveré cuando sea necesario.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "lyra_root"
                    )
                )
            )
        )
        return NpcDialogueTree(
            npcId = npc.id,
            nodes = nodes,
            rootSelector = { "lyra_root" }
        )
    }

    private fun elaraTree(npc: NpcDef): NpcDialogueTree {
        val nodes = mapOf(
            "elara_root" to DialogueNode(
                id = "elara_root",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "La luz de la Fuente de Vida acoge a todo viajero cansado. Descansa un instante y deja que tus heridas sanen.",
                options = listOf(
                    DialogueOption(
                        id = "e_heal",
                        text = "Recibir la bendición de la Fuente (Restaurar Salud y Maná).",
                        tag = DialogueTagKind.BLESSING,
                        nextNodeId = "elara_blessed",
                        effect = DialogueEffect(DialogueEffectType.HEAL_RESTORE)
                    ),
                    DialogueOption(
                        id = "e_lore",
                        text = "Háblame sobre el poder de la Fuente de Vida.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "elara_lore"
                    ),
                    DialogueOption(
                        id = "e_close",
                        text = "Gracias, Sacerdotisa. Continuaré mi camino.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "elara_blessed" to DialogueNode(
                id = "elara_blessed",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "Que la serenidad guíe tu hoja y tu espíritu. Tu vitalidad y tu energía arcana brillan con plenitud.",
                options = listOf(
                    DialogueOption(
                        id = "e_blessed_back",
                        text = "Volver a conversar.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "elara_root"
                    ),
                    DialogueOption(
                        id = "e_blessed_close",
                        text = "Me siento renovado. Adiós.",
                        tag = DialogueTagKind.FAREWELL,
                        effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                    )
                )
            ),
            "elara_lore" to DialogueNode(
                id = "elara_lore",
                speakerName = npc.name,
                speakerTitle = npc.title,
                text = "La fuente brotó cuando los primeros fundadores alzaron Valoria. Si alguna vez caes en combate, su luz te traerá de vuelta sano y salvo.",
                options = listOf(
                    DialogueOption(
                        id = "e_lore_back",
                        text = "Es reconfortante saberlo.",
                        tag = DialogueTagKind.LORE,
                        nextNodeId = "elara_root"
                    )
                )
            )
        )
        return NpcDialogueTree(
            npcId = npc.id,
            nodes = nodes,
            rootSelector = { "elara_root" }
        )
    }

    private fun fallbackTree(npc: NpcDef): NpcDialogueTree {
        return NpcDialogueTree(
            npcId = npc.id,
            nodes = mapOf(
                "root" to DialogueNode(
                    id = "root",
                    speakerName = npc.name,
                    speakerTitle = npc.title,
                    text = npc.greeting,
                    options = listOf(
                        DialogueOption(
                            id = "close",
                            text = "Cerrar",
                            tag = DialogueTagKind.FAREWELL,
                            effect = DialogueEffect(DialogueEffectType.CLOSE_DIALOG)
                        )
                    )
                )
            ),
            rootSelector = { "root" }
        )
    }
}
