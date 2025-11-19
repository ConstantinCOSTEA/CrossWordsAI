package fr.univcotedazur.crosswordsai.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Data class to hold the current settings state (simulated for demonstration)
data class AppSettings(
    var enableSound: Boolean = true,
    var soundVolume: Float = 0.8f,
    var enableHapticFeedback: Boolean = true,
    var darkModeEnabled: Boolean = false,
    var enableAiSuggestions: Boolean = true,
    var showClueHints: Boolean = true,
    var autoCheckWords: Boolean = false,
    var confirmOnExit: Boolean = true,
    var selectedTheme: AppTheme = AppTheme.SYSTEM_DEFAULT
)

enum class AppTheme {
    SYSTEM_DEFAULT, LIGHT, DARK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    modifier: Modifier = Modifier,
    // In a real app, you would pass a ViewModel or a mechanism
    // to read/write settings to persistent storage.
    // For this example, we use remember to simulate state.
    currentSettings: AppSettings = remember { AppSettings() },
    onSettingsChange: (AppSettings) -> Unit = {}, // Callback to notify parent of changes
    isLoggedIn: Boolean = false,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // State holders for each setting, initialized from currentSettings
    var enableSound by remember { mutableStateOf(currentSettings.enableSound) }
    var soundVolume by remember { mutableFloatStateOf(currentSettings.soundVolume) }
    var enableHapticFeedback by remember { mutableStateOf(currentSettings.enableHapticFeedback) }
    var darkModeEnabled by remember { mutableStateOf(currentSettings.darkModeEnabled) } // This would typically control the app's overall theme
    var enableAiSuggestions by remember { mutableStateOf(currentSettings.enableAiSuggestions) }
    var showClueHints by remember { mutableStateOf(currentSettings.showClueHints) }
    var autoCheckWords by remember { mutableStateOf(currentSettings.autoCheckWords) }
    var confirmOnExit by remember { mutableStateOf(currentSettings.confirmOnExit) }
    var selectedTheme by remember { mutableStateOf(currentSettings.selectedTheme) }

    // State for showing the theme selection dialog
    var showThemeDialog by remember { mutableStateOf(false) }

    // Effect to update parent settings when local states change
    LaunchedEffect(
        enableSound, soundVolume, enableHapticFeedback, darkModeEnabled,
        enableAiSuggestions, showClueHints, autoCheckWords, confirmOnExit, selectedTheme
    ) {
        onSettingsChange(
            currentSettings.copy(
                enableSound = enableSound,
                soundVolume = soundVolume,
                enableHapticFeedback = enableHapticFeedback,
                darkModeEnabled = darkModeEnabled,
                enableAiSuggestions = enableAiSuggestions,
                showClueHints = showClueHints,
                autoCheckWords = autoCheckWords,
                confirmOnExit = confirmOnExit,
                selectedTheme = selectedTheme
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between items
        ) {
            item { SettingsSectionHeader(title = "Account") }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLoggedIn) Icons.Filled.AccountCircle else Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        tint = if (isLoggedIn)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) "Logged In" else "Not Logged In",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLoggedIn)
                                "You are currently signed in"
                            else
                                "Sign in to sync your data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (isLoggedIn) onLogout() else onLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoggedIn)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isLoggedIn) "Sign Out" else "Sign In",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // MARK: - General Settings Section
            item { SettingsSectionHeader(title = "General Settings") }

            item {
                SettingItemToggle(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Enable Sound",
                    description = "Play sounds for interactions and notifications.",
                    checked = enableSound,
                    onCheckedChange = { enableSound = it }
                )
            }

            item {
                SettingItemSlider(
                    title = "Sound Volume",
                    description = "Adjust the app's sound volume.",
                    value = soundVolume,
                    onValueChange = { soundVolume = it },
                    valueRange = 0f..1f,
                    steps = 0, // Continuous slider
                    enabled = enableSound // Only enabled if sound is on
                )
            }

            item {
                SettingItemToggle(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "Haptic Feedback",
                    description = "Enable vibration for button presses and actions.",
                    checked = enableHapticFeedback,
                    onCheckedChange = { enableHapticFeedback = it }
                )
            }

            // MARK: - Theme Settings Section
            item { Spacer(modifier = Modifier.height(16.dp)) } // Separator
            item { SettingsSectionHeader(title = "Appearance") }

            item {
                SettingItemClickable(
                    icon = Icons.Filled.Palette,
                    title = "App Theme",
                    description = "Current theme: ${selectedTheme.name.replace("_", " ")}",
                    onClick = { showThemeDialog = true }
                )
            }

            // Legacy dark mode toggle (if not using AppTheme directly)
            /*
            item {
                SettingItemToggle(
                    icon = Icons.Filled.Nightlight,
                    title = "Dark Mode",
                    description = "Toggle dark theme for the app interface.",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
            }
            */

            // MARK: - Crossword & AI Features Section
            item { Spacer(modifier = Modifier.height(16.dp)) } // Separator
            item { SettingsSectionHeader(title = "Crossword & AI Features") }

            item {
                SettingItemToggle(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "AI Suggestions",
                    description = "Get AI-powered hints and word suggestions.",
                    checked = enableAiSuggestions,
                    onCheckedChange = { enableAiSuggestions = it }
                )
            }

            item {
                SettingItemToggle(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "Clue Hints",
                    description = "Show additional hints for difficult clues.",
                    checked = showClueHints,
                    onCheckedChange = { showClueHints = it }
                )
            }

            item {
                SettingItemToggle(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "Auto-check Words",
                    description = "Automatically check words as you complete them.",
                    checked = autoCheckWords,
                    onCheckedChange = { autoCheckWords = it }
                )
            }

            // MARK: - Application Settings Section
            item { Spacer(modifier = Modifier.height(16.dp)) } // Separator
            item { SettingsSectionHeader(title = "Application") }

            item {
                SettingItemToggle(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "Confirm on Exit",
                    description = "Ask for confirmation before exiting the application.",
                    checked = confirmOnExit,
                    onCheckedChange = { confirmOnExit = it }
                )
            }

            item {
                SettingItemClickable(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "About",
                    description = "Information about the app, version, and licenses.",
                    onClick = { /* TODO: Navigate to About screen */ }
                )
            }

            item {
                SettingItemClickable(
                    icon = Icons.Filled.Info, // Placeholder icon
                    title = "Privacy Policy",
                    description = "Read our privacy policy.",
                    onClick = { /* TODO: Open privacy policy URL */ }
                )
            }
        }

        // Theme selection dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                selectedTheme = selectedTheme,
                onThemeSelected = { newTheme ->
                    selectedTheme = newTheme
                    showThemeDialog = false
                    // Here you might trigger a recomposition of the main app theme
                },
                onDismissRequest = { showThemeDialog = false }
            )
        }
    }
}

// MARK: - Reusable Setting Item Composable

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    // A subtle line to separate sections visually
}

@Composable
fun SettingItemToggle(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Content description can be part of the title
                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingItemSlider(
    title: String,
    description: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0, // 0 for continuous, >0 for discrete steps
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}

@Composable
fun SettingItemClickable(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Content description can be part of the title
                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        // Optional: Add a chevron icon to indicate it's clickable
        Icon(
            imageVector = Icons.Filled.Info, // Example: Use a chevron_right icon if available
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select App Theme") },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == selectedTheme),
                            onClick = { onThemeSelected(theme) }
                        )
                        Text(
                            text = theme.name.replace("_", " "),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}


// MARK: - Preview
@Preview(showBackground = true)
@Composable
fun PreviewSettingsView() {
    MaterialTheme {
        SettingsView()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsSectionHeader() {
    MaterialTheme {
        Column {
            SettingsSectionHeader(title = "General")
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader(title = "Advanced Features")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingItemToggle() {
    MaterialTheme {
        Column {
            SettingItemToggle(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Enable Sounds",
                description = "Turn on sound effects.",
                checked = true,
                onCheckedChange = {}
            )
            SettingItemToggle(
                icon = Icons.Filled.Nightlight,
                title = "Dark Mode",
                checked = false,
                onCheckedChange = {}
            )
            SettingItemToggle(
                icon = Icons.Filled.Nightlight,
                title = "Disabled Setting",
                checked = true,
                onCheckedChange = {},
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingItemSlider() {
    MaterialTheme {
        Column {
            SettingItemSlider(
                title = "Volume",
                description = "Adjust the app's volume.",
                value = 0.5f,
                onValueChange = {},
                valueRange = 0f..1f
            )
            SettingItemSlider(
                title = "Difficulty",
                description = "Set the puzzle difficulty.",
                value = 2f,
                onValueChange = {},
                valueRange = 1f..5f,
                steps = 4
            )
            SettingItemSlider(
                title = "Disabled Slider",
                value = 0.7f,
                onValueChange = {},
                valueRange = 0f..1f,
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingItemClickable() {
    MaterialTheme {
        Column {
            SettingItemClickable(
                icon = Icons.Filled.Info,
                title = "About App",
                description = "View app version and license info.",
                onClick = {}
            )
            SettingItemClickable(
                title = "Privacy Policy",
                onClick = {}
            )
            SettingItemClickable(
                icon = Icons.Filled.Info,
                title = "Disabled Clickable",
                description = "This item is currently disabled.",
                onClick = {},
                enabled = false
            )
        }
    }
}

@Preview
@Composable
fun PreviewThemeSelectionDialog() {
    MaterialTheme {
        ThemeSelectionDialog(
            selectedTheme = AppTheme.LIGHT,
            onThemeSelected = {},
            onDismissRequest = {}
        )
    }
}