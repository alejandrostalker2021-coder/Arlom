package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.AppScreen
import com.example.engine.GameViewModel
import com.example.ui.CharacterCreatorScreen
import com.example.ui.GameWorldScreen
import com.example.ui.TitleSelectScreen
import com.example.ui.theme.AethelgardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AethelgardTheme {
                val screen by viewModel.screen.collectAsStateWithLifecycle()
                val savedChars by viewModel.savedCharacters.collectAsStateWithLifecycle()
                val profile by viewModel.profile.collectAsStateWithLifecycle()
                val world by viewModel.world.collectAsStateWithLifecycle()
                val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
                val activeNpc by viewModel.activeNpc.collectAsStateWithLifecycle()
                val coopMode by viewModel.coopMode.collectAsStateWithLifecycle()
                val remotePlayers by viewModel.remotePlayers.collectAsStateWithLifecycle()

                when (screen) {
                    AppScreen.TITLE_SELECT -> {
                        TitleSelectScreen(
                            savedCharacters = savedChars,
                            onSelectCharacter = { viewModel.startPlaying(it) },
                            onDeleteCharacter = { viewModel.deleteCharacter(it) },
                            onCreateNew = { viewModel.navigateTo(AppScreen.CHARACTER_CREATOR) }
                        )
                    }
                    AppScreen.CHARACTER_CREATOR -> {
                        CharacterCreatorScreen(
                            onBack = { viewModel.navigateTo(AppScreen.TITLE_SELECT) },
                            onConfirmCreate = { name, race, heroClass ->
                                viewModel.createAndStartCharacter(name, race, heroClass)
                            }
                        )
                    }
                    AppScreen.IN_GAME -> {
                        val currentProfile = profile
                        if (currentProfile != null) {
                            GameWorldScreen(
                                viewModel = viewModel,
                                profile = currentProfile,
                                world = world,
                                activeModal = activeModal,
                                activeNpc = activeNpc,
                                coopMode = coopMode,
                                remotePlayers = remotePlayers
                            )
                        } else {
                            viewModel.navigateTo(AppScreen.TITLE_SELECT)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveCurrentProfile()
    }
}
