# FreeGram

**FreeGram** is a diagram document authoring tool built with Kotlin and JavaFX.  
It uses **FDML** (FreeDiagram Markup Language) — an XML-based open format — and supports **Mermaid** diagrams with full three-way conversion between FDML, Mermaid, and PDF.

## Features

- **FDML Format** — Custom XML-based open format with full position/coordinate information
- **Mermaid Support** — Native Mermaid diagram blocks with bidirectional conversion
- **PDF Conversion** — Three-way: FDML ↔ Mermaid ↔ PDF
- **Mermaid Compatibility Metadata** — Each document declares `full`/`partial`/`none` compatibility with incompatible reasons
- **Resource Embedding** — Fonts, images, and files can be base64-embedded directly into the XML
- **GUI Editor** — JavaFX-based canvas editor with drag, drop, property editing, and Mermaid WebView preview
- **Cross-Platform** — Runs on any OS with Java 17+ (Linux, Windows, macOS)

## Quick Start

```bash
# Prerequisites: Java 17+ and JavaFX 17+
java --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.web -jar freegram.jar
```

Or use the launcher script:

```bash
# Linux
./freegram-linux.sh

# Windows (double-click or cmd)
freegram-windows.bat
```

## Build from Source

```bash
git clone https://github.com/hslcrb/freegram.git
cd freegram
./gradlew build
```

The JAR will be at `build/libs/freegram-1.0.0.jar`.

## FDML Format (`.fdml`)

FDML is an XML-based diagram format defined at `https://freegram.dev/fdml/1.0`.

Key features:
- `<node>` — diagram nodes with x, y, width, height, shape type
- `<edge>` — connections with source/target, waypoints, labels
- `<mermaid>` — native Mermaid code blocks
- `<pdf>` — embedded PDF elements
- `<group>` — nested element groups
- `<resources>` — base64-embedded fonts, images, and raw files
- `<mermaidCompatibility>` — declares how well the document converts to Mermaid

Example:

```xml
<fdml version="1.0" xmlns="https://freegram.dev/fdml/1.0">
  <metadata>
    <title>My Diagram</title>
    <mermaidCompatibility level="full"/>
  </metadata>
  <diagram width="1200" height="900">
    <node id="n1" x="100" y="100" width="160" height="60">
      <content>Start</content>
    </node>
    <mermaid id="m1" x="500" y="100" native="true">
      <code><![CDATA[graph TD; A-->B;]]></code>
    </mermaid>
  </diagram>
</fdml>
```

## Three-Way Conversion

| From \\ To | FDML | Mermaid | PDF |
|-----------|------|---------|-----|
| **FDML** | — | ✅ | ✅ |
| **Mermaid** | ✅ | — | ✅ |
| **PDF** | ✅ | ✅ (via FDML) | — |

## License

Apache 2.0 — see [LICENSE](LICENSE)
