package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.*
import com.example.pixel.PixelSprites
import com.example.ui.theme.*

@Composable
fun TitleSelectScreen(
    savedCharacters: List<CharacterProfile>,
    onSelectCharacter: (CharacterProfile) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_title_backdrop_1795635524078),
            contentDescription = "Paisaje medieval pixel art de Aethelgard",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x550F0C16),
                            Color(0xCC0F0C16),
                            ObsidianBg
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 720.dp)
                .align(Alignment.Center)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AETHELGARD",
                style = MaterialTheme.typography.displaySmall,
                color = MoltenGold
            )
            Text(
                text = "Mundo Abierto Pixel-Art · Misiones · Sets (+6 Nv.) · Wi-Fi Coop",
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentText.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_hero_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoltenGold,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "CREAR NUEVO HÉROE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "HÉROES GUARDADOS (${savedCharacters.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MutedStone
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (savedCharacters.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MoltenGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Aún no tienes héroes creados",
                            style = MaterialTheme.typography.titleSmall,
                            color = ParchmentText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Elige entre 4 razas y 11 especializaciones para explorar Aldea de Valoria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedStone
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedCharacters, key = { it.id }) { hero ->
                        val sprite = remember(hero.race, hero.heroClass) {
                            PixelSprites.getHero(hero.race, hero.heroClass)
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCharacter(hero) }
                                .testTag("hero_card_${hero.id}"),
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianBg)
                                        .border(1.dp, Color(hero.heroClass.accentHex), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = sprite,
                                        contentDescription = hero.name,
                                        filterQuality = FilterQuality.None,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hero.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ParchmentText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Nv.${hero.level}/$MAX_PLAYER_LEVEL · ${hero.race.displayName} · ${hero.heroClass.displayName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MoltenGold
                                    )
                                    Text(
                                        text = "${hero.gold} Oro · ${hero.inventory.size} Objetos en mochila",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedStone
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCharacter(hero.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar héroe",
                                        tint = MutedStone
                                    )
                                }
                                IconButton(
                                    onClick = { onSelectCharacter(hero) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Jugar",
                                        tint = EmeraldHeal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCreatorScreen(
    onBack: () -> Unit,
    onConfirmCreate: (name: String, race: Race, heroClass: HeroClass) -> Unit
) {
    BackHandler { onBack() }

    var heroName by remember { mutableStateOf("") }
    var selectedRace by remember { mutableStateOf(Race.HUMAN) }
    var selectedGroup by remember { mutableStateOf(ClassGroup.PLATE_VANGUARD) }
    var selectedClass by remember { mutableStateOf(HeroClass.PALADIN) }

    val classesInGroup = remember(selectedGroup) {
        HeroClass.entries.filter { it.group == selectedGroup }
    }
    LaunchedEffect(selectedGroup) {
        if (selectedClass.group != selectedGroup) {
            selectedClass = classesInGroup.first()
        }
    }

    val heroSprite = remember(selectedRace, selectedClass) {
        PixelSprites.getHero(selectedRace, selectedClass)
    }
    val classSkills = remember(selectedClass) {
        LootAndQuestData.skillsForClass(selectedClass)
    }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = ParchmentText
                    )
                }
                Text(
                    "FORJAR HÉROE",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoltenGold
                )
            }
        },
        bottomBar = {
            Surface(
                color = ObsidianSurface,
                tonalElevation = 4.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Button(
                    onClick = { onConfirmCreate(heroName, selectedRace, selectedClass) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                        .testTag("start_adventure_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoltenGold,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "ENTRAR A AETHELGARD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 720.dp)
            ) {
            // Live Hero Sprite & Name
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg)
                            .border(2.dp, Color(selectedClass.accentHex), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = heroSprite,
                            contentDescription = "Vista previa del héroe",
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = heroName,
                            onValueChange = { if (it.length <= 18) heroName = it },
                            label = { Text("Nombre del Héroe") },
                            placeholder = { Text("${selectedRace.displayName} ${selectedClass.displayName}") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hero_name_input")
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("1. ELIGE TU RAZA (4 RAZAS)", style = MaterialTheme.typography.labelMedium, color = MoltenGold)
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Race.entries) { race ->
                    val isSel = race == selectedRace
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedRace = race },
                        label = {
                            Text(
                                race.displayName,
                                style = MaterialTheme.typography.titleSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MoltenGold,
                            selectedLabelColor = ObsidianBg,
                            containerColor = ObsidianCard,
                            labelColor = ParchmentText
                        ),
                        modifier = Modifier.testTag("race_chip_${race.name}")
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "${selectedRace.displayName} · ${selectedRace.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MoltenGold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pasiva (${selectedRace.passiveName}): ${selectedRace.passiveDesc}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ParchmentText
                    )
                    Text(
                        "Racial (${selectedRace.activeSkillName}): ${selectedRace.activeSkillDesc}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SapphireMana
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("2. ARQUETIPO Y ESPECIALIZACIÓN", style = MaterialTheme.typography.labelMedium, color = MoltenGold)
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ClassGroup.entries) { grp ->
                    FilterChip(
                        selected = grp == selectedGroup,
                        onClick = { selectedGroup = grp },
                        label = { Text(grp.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SapphireMana,
                            selectedLabelColor = Color.White,
                            containerColor = ObsidianCard,
                            labelColor = ParchmentText
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(classesInGroup) { cls ->
                    FilterChip(
                        selected = cls == selectedClass,
                        onClick = { selectedClass = cls },
                        label = { Text(cls.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(cls.accentHex),
                            selectedLabelColor = ObsidianBg,
                            containerColor = ObsidianSurface,
                            labelColor = ParchmentText
                        ),
                        modifier = Modifier.testTag("class_chip_${cls.name}")
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, Color(selectedClass.accentHex).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            selectedClass.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(selectedClass.accentHex)
                        )
                        Text(
                            "Armadura: ${selectedClass.armorWeight.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedStone
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        selectedClass.roleSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ParchmentText
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "HP Base: ${selectedClass.baseHp}  |  Ataque: ${selectedClass.baseAttack}  |  Armadura: ${selectedClass.baseArmor}  |  Evasión: ${(selectedClass.baseDodge * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MoltenGold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("HABILIDADES DE CLASE:", style = MaterialTheme.typography.labelMedium, color = MutedStone)
                    Spacer(Modifier.height(6.dp))
                    classSkills.forEach { sk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(sk.colorHex), RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${sk.name} [${sk.tag}] · ${sk.manaCost} MP · ${sk.cooldownSec.toInt()}s CD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ParchmentText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    sk.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedStone
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}
