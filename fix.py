with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip_context = False

for i, line in enumerate(lines):
    # Fix the double context in ManualViewerScreen
    if "val context = androidx.compose.ui.platform.LocalContext.current" in line:
        if skip_context:
            continue
        skip_context = True
    
    # Fix the syntax error around LibraryScreen
    if "    }\n" == line and i > 0 and lines[i-1] == "    }\n" and lines[i-2] == "}\n":
        if "LibraryScreen" in "".join(lines[i-20:i]):
            continue # skip the extra brace
    
    new_lines.append(line)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.writelines(new_lines)
