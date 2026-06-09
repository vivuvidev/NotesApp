# NotesApp Design System

Derived from Stitch screens: **Notes Home** and **Add/Edit Note**.  
Font: **Inter** · Style: Material 3 · Supports light + dark.

---

## Colors

### Brand

| Token | Light | Dark | Usage |
|---|---|---|---|
| `primary` | `#324fc3` | `#324fc3` | Buttons, active icons, FAB, save action |
| `onPrimary` | `#ffffff` | `#ffffff` | Text/icons on primary surfaces |
| `primaryContainer` (hover tint) | `#324fc3` @ 10% alpha | `#324fc3` @ 10% alpha | Icon button hover state |

### Backgrounds & Surfaces

| Token | Light | Dark | Usage |
|---|---|---|---|
| `background` | `#f6f6f8` | `#13151f` | Screen background |
| `onBackground` | `#0f172a` (slate-900) | `#f1f5f9` (slate-100) | Primary text |
| `surface` | `#ffffff` | `#1e293b` (slate-800) | Note cards |
| `onSurface` | `#0f172a` (slate-900) | `#f1f5f9` (slate-100) | Card title text |
| `surfaceVariant` | `#ffffff` @ 80% | `#13151f` @ 80% | Frosted top bar (Notes Home) |
| `outline` | `#e2e8f0` (slate-200) | `#334155` (slate-700) | Card borders, dividers |
| `outlineVariant` | `#e2e8f0` @ 50% | `#334155` @ 50% | Card border (subtle) |

### Text Roles

| Token | Light | Dark | Usage |
|---|---|---|---|
| `onSurfaceVariant` | `#475569` (slate-600) | `#94a3b8` (slate-400) | Note card body preview |
| `tertiary` | `#94a3b8` (slate-400) | `#64748b` (slate-500) | Dates, metadata, inactive nav |
| `error` | `#ef4444` | `#f87171` | Destructive actions only |

### In Compose

```kotlin
// Color.kt
val Primary = Color(0xFF324FC3)
val BackgroundLight = Color(0xFFF6F6F8)
val BackgroundDark = Color(0xFF13151F)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1E293B)  // slate-800
val OnBackgroundLight = Color(0xFF0F172A) // slate-900
val OnBackgroundDark = Color(0xFFF1F5F9)  // slate-100
val OnSurfaceVariantLight = Color(0xFF475569) // slate-600
val OnSurfaceVariantDark = Color(0xFF94A3B8)  // slate-400
val OutlineLight = Color(0xFFE2E8F0)  // slate-200
val OutlineDark = Color(0xFF334155)   // slate-700
val TertiaryLight = Color(0xFF94A3B8) // slate-400
val TertiaryDark = Color(0xFF64748B)  // slate-500
```

Always reference via `MaterialTheme.colorScheme.*` — no hardcoded hex in composables.

---

## Typography

Font family: **Inter** (weights 400, 500, 600, 700).

| Style | Size | Weight | Line height | Letter spacing | Usage |
|---|---|---|---|---|---|
| `displayLarge` | 30sp | Bold (700) | 38sp | –0.02em | Note editor title field |
| `headlineMedium` | 24sp | SemiBold (600) | 32sp | –0.02em | Notes Home app bar title |
| `titleLarge` | 20sp | Medium (500) | 28sp | 0 | Edit Note app bar title |
| `titleMedium` | 18sp | Bold (700) | 26sp | 0 | Note card title |
| `bodyLarge` | 18sp | Normal (400) | 28sp | 0 | Note editor body textarea |
| `bodyMedium` | 14sp | Normal (400) | 22sp | 0 | Note card body preview |
| `labelSmall` | 12sp | Medium (500) | 16sp | +0.08em | Date chips, bottom nav labels |

In Compose, add `fontFamily = interFontFamily` to each `TextStyle` in `Type.kt`.

---

## Spacing

All values are `Dp`.

| Token | Value | Source | Usage |
|---|---|---|---|
| `spacingXS` | 4.dp | — | Tight internal gaps |
| `spacingSM` | 8.dp | gap-2 | Card internal gap, icon gap |
| `spacingMD` | 16.dp | px-4 / p-4 / gap-4 | Screen H-padding, card padding, card list gap |
| `spacingLG` | 24.dp | px-6 / mb-6 | Edit screen content H-padding, title→meta spacing |
| `spacingXL` | 32.dp | — | Section separators |

### Component-specific

| Token | Value | Usage |
|---|---|---|
| `topBarHeightHome` | 56.dp | Notes Home sticky bar (py-4 + content) |
| `topBarHeightEdit` | 64.dp | Edit Note bar (h-16) |
| `bottomNavHeight` | 60.dp | Bottom navigation bar |
| `fabSize` | 56.dp | FAB width & height (w-14 h-14) |
| `fabEndPadding` | 24.dp | FAB distance from end edge (right-6) |
| `fabBottomPadding` | 96.dp | FAB distance from bottom edge (bottom-24) |
| `iconButtonSize` | 48.dp | Top bar icon buttons (w-12 h-12) |
| `toolbarButtonSize` | 40.dp | Bottom editor toolbar buttons (w-10 h-10) |
| `cardPadding` | 16.dp | Padding inside note cards |
| `cardListGap` | 16.dp | Vertical gap between note cards |
| `titleToMetaSpacing` | 8.dp | Gap between title and meta row in card (mt-2) |
| `titleFieldToMeta` | 16.dp | Editor title → meta row (mb-4) |
| `metaToBody` | 24.dp | Editor meta row → body field (mb-6) |

---

## Shape (Border Radius)

| Token | Radius | Source | Usage |
|---|---|---|---|
| `ShapeSmall` | 8.dp | `rounded` (0.5rem) | Toolbar buttons, small chips |
| `ShapeMedium` | 16.dp | `rounded-lg` (1rem) | Toolbar buttons hover area |
| `ShapeLarge` | 24.dp | `rounded-xl` (1.5rem) | Note cards, FAB |
| `ShapeFull` | 50% (CircleShape) | `rounded-full` | Icon buttons, pill badges |

```kotlin
// In MaterialTheme shapes:
small = RoundedCornerShape(8.dp)
medium = RoundedCornerShape(16.dp)
large = RoundedCornerShape(24.dp)
```

---

## Elevation & Borders

- **Cards:** `shadow-sm` → `shadowElevation = 1.dp`, `tonalElevation = 0.dp` + `Border(1.dp, outline.copy(alpha = 0.5f))`
- **FAB:** `shadow-lg` → `shadowElevation = 6.dp`
- **Top bar (Home):** Frosted glass effect → `BlurMaskFilter` or `surfaceColorAtElevation` with alpha overlay; add `Modifier.shadow(2.dp)` when scrolled
- **Top bar (Edit):** No elevation, solid background, 0.dp shadow
- **Bottom toolbar / nav bar:** Top border only — `Divider(color = outline)`, no shadow
- No decorative shadows on any other surface

---

## Component Patterns

### AppTopBar — Notes Home

```
Row(sticky, z-elevated, frosted-bg):
  Text("My Notes", style=headlineMedium, trackingTight)
  Row(gap=8.dp):
    IconButton(48.dp, CircleShape, hover=primaryContainer): SearchIcon
    IconButton(48.dp, CircleShape, hover=primaryContainer): MoreVertIcon
```

- Background: `background.copy(alpha = 0.8f)` + `Modifier.blur` (or `BlurMaskFilter`)
- Icons: `onSurfaceVariant`

### AppTopBar — Edit Note

```
Row(h=64.dp, solid bg):
  Row(gap=16.dp):
    IconButton(48.dp, CircleShape): ArrowBackIcon (onSurfaceVariant)
    Text("Edit Note", style=titleLarge)
  Row(gap=4.dp):
    IconButton(48.dp, CircleShape): PushPinIcon (onSurfaceVariant)
    IconButton(48.dp, CircleShape): ArchiveIcon (onSurfaceVariant)
    IconButton(48.dp, CircleShape): CheckIcon (primary — save action)
```

### NoteCard

```
Card(
  modifier = Modifier.fillMaxWidth(),
  shape = ShapeLarge,            // 24.dp
  colors = CardDefaults.cardColors(containerColor = surface),
  border = BorderStroke(1.dp, outlineVariant),
  elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
):
  Column(padding=16.dp, gap=8.dp):
    Text(title, style=titleMedium, color=onSurface, maxLines=1)
    Text(body, style=bodyMedium, color=onSurfaceVariant, maxLines=2, overflow=Ellipsis)
    Row(horizontalArrangement=End):
      Text(date, style=labelSmall, color=tertiary, uppercase=true)
```

### FloatingActionButton

```
FloatingActionButton(
  shape = ShapeLarge,            // rounded-xl, not CircleShape
  containerColor = primary,
  contentColor = onPrimary,
  modifier = Modifier.size(56.dp)
):
  Icon(AddIcon or MicIcon, size=28.dp)
```

- Position: `Scaffold` FAB slot — aligns to `bottom=96.dp, end=24.dp` above the bottom nav
- Interaction: `scale = 0.95f` on press, `brightness` on hover

### Bottom Navigation Bar

```
NavigationBar(
  containerColor = surface,
  tonalElevation = 0.dp,
  modifier = Modifier.border(top = 1.dp, color = outline)
):
  NavigationBarItem each:
    selected: icon filled (FILL=1), label color = primary
    unselected: icon outlined, label color = tertiary
    label: style=labelSmall
```

Items: **Notes** (`description`), **Checklist** (`check_box`), **Settings** (`settings`)

### Note Editor Fields

```
// Title
BasicTextField(
  textStyle = displayLarge.copy(color=onBackground),
  decorationBox = { it() },      // no outline, no background
  placeholder: onSurfaceVariant @ 60% alpha
)

// Meta row (between title and body)
Row(gap=8.dp):
  Text("Edited HH:MM", style=bodyMedium, color=tertiary)
  Text("•", color=tertiary)
  Text("NNN characters", style=bodyMedium, color=tertiary)

// Body
BasicTextField(
  modifier = Modifier.fillMaxSize(),
  textStyle = bodyLarge.copy(color=onBackground),
  decorationBox = { it() },
  placeholder: onSurfaceVariant @ 60% alpha
)
```

### Bottom Editor Toolbar

```
Row(border-top=outline, padding=horizontal:16.dp vertical:8.dp):
  Row(gap=4.dp):                 // left group
    IconButton(40.dp, ShapeSmall, hover=primaryContainer): AddBoxIcon
    IconButton(40.dp, ShapeSmall, hover=primaryContainer): PaletteIcon
    IconButton(40.dp, ShapeSmall, hover=primaryContainer): FormatSizeIcon
    IconButton(40.dp, ShapeSmall, hover=primaryContainer): TextFormatIcon
  IconButton(40.dp, ShapeSmall, hover=primaryContainer): MoreVertIcon  // right
```

All toolbar icons: `onSurfaceVariant`

---

## Icons

Library: **Material Symbols Outlined** (variable font).  
In Compose: use `androidx.compose.material.icons.outlined.*` or the extended icons artifact.

Key icons used:

| Screen | Icon | Slot |
|---|---|---|
| Notes Home | `Search` | Top bar |
| Notes Home | `MoreVert` | Top bar |
| Notes Home | `Add` | FAB |
| Notes Home | `Description` (filled) | Bottom nav — active |
| Notes Home | `CheckBox` | Bottom nav |
| Notes Home | `Settings` | Bottom nav |
| Edit Note | `ArrowBack` | Top bar |
| Edit Note | `PushPin` | Top bar |
| Edit Note | `Archive` | Top bar |
| Edit Note | `Check` | Top bar (save) |
| Edit Note | `Mic` | FAB |
| Edit Note | `AddBox` | Editor toolbar |
| Edit Note | `Palette` | Editor toolbar |
| Edit Note | `FormatSize` | Editor toolbar |
| Edit Note | `TextFormat` | Editor toolbar |
| Edit Note | `MoreVert` | Editor toolbar |

---

## Dark Mode

Toggle via `darkMode: "class"` (Tailwind) → `isSystemInDarkTheme()` in Compose.  
Every color token has an explicit dark value defined in the Colors section above.  
Never use `if (isDark)` inline in composables — wire dark values through `MaterialTheme.colorScheme` only.
