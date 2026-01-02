# JagFX

Jagex Audio Synthesis Engine & Tools.

---

## Commands

### Dependencies

- `JDK 21+`
- `sbt 1.11+`

### Build & Run

- `sbt compile`: Compile source
- `sbt "run -- <input.synth> <output.wav> [loopCount]"`: Run CLI from source
- `sbt test`: Execute unit tests (munit)

### Deployment

- `sbt stage`: Generate native scripts in `target/universal/stage/bin/`
- `./target/universal/stage/bin/jagfx-cli <input.synth> <output.wav> [loopCount]`: Execute binary

---

## CLI Usage

`jagfx-cli` converts Jagex `.synth` binary files to 8-bit mono PCM `.wav` files.

**Arguments**:

- `input.synth`: Path to source file
- `output.wav`: Path for output file
- `loopCount`: (Optional) Repetitions for loop region (Default: `1`)

---

## Project Structure

```text
src/main/scala/jagfx/
├── io/                 # Binary serialization & WAV generation
├── model/              # Pure data models (Tone, Envelope, etc.)
├── synth/              # DSP & Waveform generation logic
└── Constants.scala     # Shared constants
└── JagFXCli.scala      # Main CLI entry point
```

## Implementation Layers

- **Model**: Immutable representation of `.synth` file structure.
- **IO**: Bitstream parsing and 8-bit WAV encoding.
- **Synth**: Additive synthesis engine with FM/AM modulation and reverb.
- **CLI**: Batch processing interface.

---

## Documentation

- `docs/synth_format_spec.md`: Detailed binary specification.

## Logging

Uses `scribe` logger. Output to `stdout`.

- `INFO`: Conversion progress and file stats.
- `DEBUG`: Harmonic/Envelope processing details.
- `WARN`: Invalid loop ranges or empty synth files.
- `ERROR`: IO or Parse failures.

---

## License

This project is licensed under MIT License - see [LICENSE](LICENSE) file for details.
