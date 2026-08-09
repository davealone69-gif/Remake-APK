with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(
    'fun DashboardScreen(onNavigateToViewer: () -> Unit, onNavigateToVIN: () -> Unit) {',
    'fun DashboardScreen(onNavigateToViewer: () -> Unit, onNavigateToVIN: () -> Unit) {\n    val context = androidx.compose.ui.platform.LocalContext.current'
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
