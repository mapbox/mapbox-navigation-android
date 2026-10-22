# Navigation SDK for Android - Architecture Diagrams Manifest

This document defines the design rules, conventions, and guidelines for creating architectural diagrams for the Navigation SDK for Android release architecture records under `docs/architecture/<version>/`, where `<version>` is `v` plus the SDK's own version (`v3.32.0-rc.1`), not the git tag.

## Design Principles

### 1. Visual Consistency
- All diagrams use a unified color palette and styling
- Typography is consistent across all diagrams (same fonts, sizes, weights)
- Spacing and padding follow a grid system
- Border radius: 4px for boxes, 6px for major containers

### 2. Clarity Over Complexity
- One diagram = one architectural aspect
- Avoid overloading diagrams with excessive detail
- Use legends and annotations to explain elements
- Keep text concise (5-10 words per label)

### 3. Scalability
- SVG format for editability and resolution independence
- Diagrams work at multiple zoom levels
- Text remains readable at both screen and print sizes
- Render to PDF only if the user asks; SVG alone satisfies the Architecture design CI type

---

## Color Palette

### Group-Specific Colors

| Group | Background | Border | Usage |
|---|---|---|---|
| **Core libraries** | `#E3F2FD` | `#1976D2` | Route state, trip data, dispatchers, metrics |
| **UI libraries** | `#F3E5F5` | `#7B1FA2` | Map rendering, camera, turn-by-turn components |
| **Optional libraries** | `#FFF3E0` | `#F57C00` | EV, ADASIS, coordination, MapGPT, road camera, weather, audio, compose |
| **Native bindings** | `#E8EAF6` | `#3F51B5` | Modules that declare a bound native SDK directly (`navigator`, `base`, and the optional libraries named in diagram 04) |
| **Bound Mapbox SDKs (external)** | `#E8F5E9` | `#388E3C` | Maps SDK, Navigation Native, Core Common, Nav SDK C++, Search SDK, Mapbox Java SDK. Not designed here |
| **Calling app (external actor)** | `#f8f9fa` | `#495057` | The integrator's application |

### Neutral Colors

| Element | Color | Usage |
|---|---|---|
| **Text Primary** | `#212529` | Titles, headings, primary labels |
| **Text Secondary** | `#495057` | Descriptions, body text |
| **Text Tertiary** | `#868e96` | Annotations, metadata |
| **Arrows** | `#6c757d` | Connection lines |

---

## Typography

```css
/* Diagram Title */
font-size: 20px;
font-weight: 700;
fill: #212529;

/* Group Labels */
font-size: 15px;
font-weight: 700;
fill: #212529;

/* Module Name */
font-size: 12-13px;
font-weight: 600;
font-family: monospace;
fill: #212529;

/* Descriptions */
font-size: 10-11px;
font-weight: 400;
fill: #495057;

/* Annotations / Metadata */
font-size: 10-11px;
font-style: italic;
fill: #868e96;
```

### Text Guidelines
- **Module names**: monospace font for identifiers (`:navigation`, `navigator`, and similar)
- **Descriptions**: max 2 lines per box, ~40 characters per line
- **Annotations**: italics for implementation notes and version labels

---

## SVG Structure Template

```xml
<svg viewBox="0 0 [WIDTH] [HEIGHT]" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <style>
      .box-class { fill: #color; stroke: #color; stroke-width: 2; }
      .text-class { fill: #color; font-size: 12px; font-weight: 600; }
    </style>
    <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#6c757d" />
    </marker>
  </defs>

  <text x="[CENTER_X]" y="30" text-anchor="middle" class="title-class">Diagram Title</text>

  <!-- Content groups -->

  <!-- Legend -->
</svg>
```

### Key SVG Guidelines
- **ViewBox**: typically 800-1100 width
- **Comments**: XML comments separate logical sections
- **Grouping**: `<g>` tags for related elements
- **Markers**: define arrow markers once in `<defs>`, reuse via `marker-end="url(#arrowhead)"`
- **No inline styles**: keep all styles in the `<style>` block

---

## Arrow Conventions

| Arrow Type | Usage | Style |
|---|---|---|
| **Dependency** | Layer/library dependency (downward or inward) | `stroke-width: 2` |
| **Binding** | Kotlin surface → native implementation (horizontal) | `stroke-width: 2` |
| **Delegation** | Major architectural delegation | `stroke-width: 3` |

Arrows are straight lines, `marker-end="url(#arrowhead)"`, no manually drawn arrowheads. Vertical arrows pointing down mean "depends on." Horizontal arrows mean "binds to" or "implements."

---

## Naming Conventions

### File Naming
```
NN_name.svg
NN_name.md

Examples:
01_layer_overview.svg
02_module_map.svg
03_optional_libraries.svg
04_native_bridge.svg
```

Numbers 01-04 are fixed for a release record's four core diagrams. A module record adds a file under `<version>/modules/<module>.svg` with its own `.md`, named by module, not by sequence.

---

## Fixed Diagram Set (per release record)

1. **`01_layer_overview`**: vertical layer stack, Calling app to UI libraries to Core libraries to Native bindings to Bound Mapbox SDKs, with Optional libraries as a side branch off Core.
2. **`02_module_map`**: every in-scope library from both trees (public `mapbox-navigation-android/` and the internal repository root), grouped into Core / UI / Optional, one box per module with name and one-line purpose.
3. **`03_optional_libraries`**: reference table, optional library name, purpose, the in-scope modules it depends on and the bound SDKs it declares directly, each read from its `build.gradle`.
4. **`04_native_bridge`**: every module that declares a bound native SDK directly on the left, grouped by tree, the bound native SDKs with their versions on the right, one arrow per declared binding.

---

## Quality Checklist

- [ ] Each SVG renders correctly when opened directly in a browser
- [ ] All text readable at 100% zoom, descriptions under ~40 characters per line
- [ ] Colors match the palette table above, consistent across all four diagrams
- [ ] Arrows use `marker-end="url(#arrowhead)"`, no manually drawn arrowheads
- [ ] Every diagram has a legend if it uses more than one color
- [ ] File names follow `NN_name.svg` / `NN_name.md`, numbers 01-04 fixed for a release record
- [ ] No tracker IDs, customer names, or internal-only URLs in any file
- [ ] No `Status`, `Approver`, `Decision`, `Date`, or `Author` field, and no "Approval" section, anywhere in any file. These records are public and reviewed through the normal PR process, not through a field inside the record.
- [ ] No sentence flags an open question, asks a reader to confirm or verify something, or otherwise reads as an internal note. State every fact as settled, at the precision that is actually true, or leave it out.
- [ ] This SDK's own version is labeled with its `3.` prefix (from `CHANGELOG.md`), not the git tag's `0.` prefix; bound SDK versions (Maps, Navigation Native, Core Common) keep their own real prefixes

---

## Maintenance

### When to Update Records
- New release or release-candidate tag in scope
- A library's public surface or module dependencies change

### When to Update This Manifest
- A new layout pattern is needed beyond the fixed four-diagram set
- The color palette or typography scale changes

This manifest is shared across all `docs/architecture/<version>/` records. Update it in place; do not fork a copy per version.