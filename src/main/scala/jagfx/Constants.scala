package jagfx

/** Public constants for external use (e.g., GUI, plugins). */
object Constants:
  /** Audio sample rate in Hz. */
  final val SampleRate: Int = 22050

  /** Bits per audio sample. */
  final val BitsPerSample: Int = 8

  /** Number of audio channels (mono). */
  final val NumChannels: Int = 1

  /** Maximum tones per synth file. */
  final val MaxTones: Int = 10

  /** Maximum partials per tone. */
  final val MaxPartials: Int = 10

  /** Maximum filter pole/zero pairs per direction. */
  final val MaxFilterPairs: Int = 4

  /** Buffer pool constants. */
  final val MaxBufferSize: Int = 1048576 // 1MB
  final val MaxPoolSize: Int = 20

  /** Filter coefficient update rate (samples per update). */
  final val FilterUpdateRate: Int = Byte.MaxValue + 1

  /** Minimum frame duration for canvas rendering (60 FPS). */
  final val MinFrameNanos: Long = 16_666_666L

  /** Phase accumulator constants for synthesis. */
  final val PhaseScale: Double = 32.768
  final val NoisePhaseDiv: Int = 2607
  final val PhaseMask: Int = 0x7fff

  /** Lookup table sizing constants. */
  final val SemitoneRange: Int = 120
  final val SinTableDivisor: Double = 5215.1903
  final val CircleSegments: Int = 64

  /** 16-bit signed integer range constants. */
  object Int16:
    final val Range: Int = 65536
    final val UnsignedMaxValue: Int = 32768
    final val Quarter: Int = 16384

  /** WAV file format constants. */
  object Wav:
    final val RiffMagic: Int = 0x52494646
    final val WaveMagic: Int = 0x57415645
    final val FmtMagic: Int = 0x666d7420
    final val DataMagic: Int = 0x64617461
    final val HeaderSize: Int = 44
    final val FmtChunkSize: Int = 16
    final val PcmFormat: Int = 1

  /** Smart variable-length integer encoding constants. */
  object Smart:
    final val Threshold: Int = Byte.MaxValue + 1
    final val SignedOffset: Int = 64
    final val SignedBaseOffset: Int = 49152
