# JagFX - Jagex Additive Synthesizer

<p align="left">
  <img
    src="assets/370_cow_death.png"
    alt="Cow Death"
    onerror="this.onerror=null;this.src='https://raw.githubusercontent.com/xsyetopz/jagfx-scala/main/assets/370_cow_death.png';"
  >
</p>

Powerful tool for visualizing, playing, and converting Jagex Audio Synthesis (`.synth`) files.

## Features

- **Audio Engine**: High-performance additive synthesis with FM modulation, gating, and custom IIR filters.
- **Visualizations**: Real-time waveform rendering, pole-zero plots, and envelope editors.
- **Cross-Platform**: Runs on macOS, Windows, and Linux.
- **Batch Processing**: CLI tools for rapid `.synth` to `.wav` conversion.

---

## Prerequisites

- **JDK 21** or higher.
- **sbt 1.11+** (Scala Build Tool).
- **Node.js** (or `bun`) for compiling SCSS styles.

---

## Getting Started

### 1. Build & Run

```bash
# compile and run GUI
sbt compile && sbt run

# run unit tests
sbt test
```

### 2. CLI Usage

Batch convert files without opening GUI:

```bash
# sbt "cli <input.synth> <output.wav> [loopCount]"

# convert single file
sbt "cli input.synth output.wav"

# convert and loop 4 times
sbt "cli input.synth output_looped.wav 4"
```

---

## Building for Distribution

You can package JagFX as standalone application that includes scripts for macOS, Linux, and Windows.

1. **Create Universal Distribution:**

   ```bash
   sbt dist
   ```

   This creates ZIP file at:
   `target/universal/jagfx-0.2.0-SNAPSHOT.zip`

2. **Usage:**
   Unzip file on any operating system.
   - **Mac/Linux:** Run `bin/jagfx`
   - **Windows:** Run `bin/jagfx.bat`

   _Note: Distribution includes native JavaFX libraries for Windows, Linux, and macOS (Intel & Silicon), so it should be portable._

---

## Project Structure

```text
src/main/
├── scala/jagfx/
│   ├── io/                 # Binary .synth reader/writer
│   ├── model/              # Pure data models (Tones, Envelopes)
│   ├── synth/              # Core DSP engine (Oscillators, Filters)
│   ├── ui/                 # JavaFX views & controllers
│   ├── utils/              # Utility functions
│   ├── JagFX.scala         # GUI Entry point
│   └── JagFXCli.scala      # CLI Entry point
└── scss/                   # UI Styling
```

## Community Examples

### `ice_cast.synth` and `ice_barrage_impact.synth`

<https://github.com/user-attachments/assets/fff9dba4-0acb-4ca9-949e-b593fdbbc0fc>

## License

This project is licensed under MIT License. See `LICENSE` file for more details.
