with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(
    'label = { Text(mode.name.lowercase().capitalize()) }',
    'label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }'
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
