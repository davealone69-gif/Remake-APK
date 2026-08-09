import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# 1. Add Scanner to Screen
if 'object Scanner : Screen("scanner"' not in content:
    content = content.replace(
        'object VINDecoder : Screen("vin", "VIN Decoder", Icons.Filled.DirectionsCar)',
        'object VINDecoder : Screen("vin", "VIN Decoder", Icons.Filled.DirectionsCar)\n    object Scanner : Screen("scanner", "Scan Manual", Icons.Filled.DocumentScanner)'
    )

# 2. Add Scanner route to AppNavHost
if 'composable(Screen.Scanner.route)' not in content:
    content = content.replace(
        'composable(Screen.VINDecoder.route) { VINDecoderScreen() }',
        'composable(Screen.VINDecoder.route) { VINDecoderScreen() }\n        composable(Screen.Scanner.route) { ScannerScreen(onBack = { navController.popBackStack() }) }'
    )

# 3. Add Print Button to ManualViewerScreen
# Look for TopAppBar actions in ManualViewerScreen
print_action = '''
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Printing to connected printer...", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }'''
if 'Icons.Filled.Print' not in content:
    content = re.sub(
        r'(TopAppBar\([\s\S]*?actions = \{)',
        r'\1' + print_action,
        content
    )
    # Also need to add val context = LocalContext.current inside ManualViewerScreen
    if 'val context = LocalContext.current' not in content:
        content = re.sub(
            r'(@Composable\s*fun ManualViewerScreen.*?\{)',
            r'\1\n    val context = androidx.compose.ui.platform.LocalContext.current',
            content
        )

# 4. Add "Digitize Manual" to LibraryScreen
library_screen_replacement = '''
@Composable
fun LibraryScreen(navController: NavHostController? = null) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController?.navigate(Screen.Scanner.route) },
                icon = { Icon(Icons.Filled.DocumentScanner, "Scan") },
                text = { Text("Digitize Manual") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Library & Tags", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                FilterChip(selected = false, onClick = {}, label = { Text("Engines") })
                FilterChip(selected = false, onClick = {}, label = { Text("Wiring") })
            }
        }
    }
}
'''
if 'Digitize Manual' not in content:
    content = re.sub(
        r'@Composable\s*fun LibraryScreen\(\)\s*\{[\s\S]*?\}\s*\}',
        library_screen_replacement.strip(),
        content
    )
    
    # Update call in AppNavHost
    content = content.replace(
        'composable(Screen.Library.route) { LibraryScreen() }',
        'composable(Screen.Library.route) { LibraryScreen(navController) }'
    )


# 5. Add ScannerScreen function
scanner_screen_func = '''
@Composable
fun ScannerScreen(onBack: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var scannedPages by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Digitize Manual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Scanning page...", color = Color.White, modifier = Modifier.padding(top = 64.dp))
                } else if (scanComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, "Done", tint = Color.Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("$scannedPages pages digitized", color = Color.White)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DocumentScanner, "Scanner", tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Align manual page in frame", color = Color.White)
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        isScanning = true
                        scanComplete = false
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, "Capture")
                    Spacer(Modifier.width(8.dp))
                    Text("Capture Page")
                }
                
                if (scannedPages > 0) {
                    Button(
                        onClick = {
                            scanComplete = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.Save, "Save")
                        Spacer(Modifier.width(8.dp))
                        Text("Save as PDF")
                    }
                }
            }
        }
    }
    
    LaunchedEffect(isScanning) {
        if (isScanning) {
            kotlinx.coroutines.delay(1500)
            scannedPages++
            isScanning = false
        }
    }
}
'''
if 'fun ScannerScreen' not in content:
    content += "\n" + scanner_screen_func

# Handle imports
imports = [
    "import androidx.compose.material3.ExtendedFloatingActionButton",
    "import androidx.compose.material.icons.filled.Print",
    "import androidx.compose.material.icons.filled.CameraAlt",
    "import androidx.compose.material.icons.filled.Save",
    "import androidx.compose.material.icons.filled.CheckCircle",
    "import androidx.compose.material.icons.filled.DocumentScanner",
    "import androidx.compose.material3.CircularProgressIndicator"
]

for imp in imports:
    if imp not in content:
        content = content.replace("package com.example\n", f"package com.example\n\n{imp}")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
