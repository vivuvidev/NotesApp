# NotesApp

## Stack
- Jetpack Compose + Material 3
- Kotlin, MVVM, Hilt, StateFlow
- Refer to DESIGN.md for all colors, typography, spacing

## UI Rules
- Map Stitch HTML/CSS → Compose Row/Column/Box
- Colors via MaterialTheme.colorScheme only, no hardcoded hex
- Every screen needs a @Preview
- State hoisting — no state inside leaf composables

## Do not modify
- local.properties
- app/build.gradle.kts without asking
