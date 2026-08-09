import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports
imports = [
    "import com.example.ui.theme.LocalAppSettings",
    "import com.example.ui.theme.AppThemeMode",
    "import com.example.ui.theme.AppFontFamily"
]
for imp in imports:
    if imp not in content:
        content = content.replace("package com.example\n", f"package com.example\n\n{imp}")

# Add Screen.Settings
if 'object Settings : Screen("settings"' not in content:
    content = content.replace(
        'object Scanner : Screen("scanner", "Scan Manual", Icons.Filled.DocumentScanner)',
        'object Scanner : Screen("scanner", "Scan Manual", Icons.Filled.DocumentScanner)\n    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)'
    )

# Add to bottom nav if we want it there, but probably better in the dashboard or just bottom nav
if 'Screen.Collaboration' in content and 'Screen.Settings' not in content:
    content = content.replace(
        'Screen.Collaboration,',
        'Screen.Collaboration,\n        Screen.Settings,'
    )

# Add to AppNavHost
if 'composable(Screen.Settings.route)' not in content:
    content = content.replace(
        'composable(Screen.Scanner.route) { ScannerScreen(onBack = { navController.popBackStack() }) }',
        'composable(Screen.Scanner.route) { ScannerScreen(onBack = { navController.popBackStack() }) }\n        composable(Screen.Settings.route) { SettingsScreen() }'
    )

# Add SettingsScreen function
settings_screen_func = '''
@Composable
fun SettingsScreen() {
    val appSettings = LocalAppSettings.current
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appSettings.themeMode.value == mode,
                    onClick = { appSettings.themeMode.value = mode },
                    label = { Text(mode.name.lowercase().capitalize()) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Font", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFontFamily.entries.forEach { font ->
                FilterChip(
                    selected = appSettings.fontFamily.value == font,
                    onClick = { appSettings.fontFamily.value = font },
                    label = { Text(font.displayName) }
                )
            }
        }
    }
}
'''

if 'fun SettingsScreen' not in content:
    content += "\n" + settings_screen_func

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
