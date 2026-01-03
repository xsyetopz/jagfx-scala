package jagfx.io

import jagfx.model._
import jagfx.Constants
import java.nio.file._

/** Serializer for `SynthFile` domain model to `.synth` binary format. */
object SynthWriter:
  /** Serializes `SynthFile` to `.synth` binary data. */
  def write(file: SynthFile): Array[Byte] =
    val buf = BinaryBuffer(4096)
    writeTones(buf, file.tones)
    buf.writeU16BE(file.loop.begin)
    buf.writeU16BE(file.loop.end)
    scribe.debug(s"Serialized ${buf.pos} byte(s)")
    buf.data.take(buf.pos)

  /** Writes `SynthFile` to filesystem path. */
  def writeToPath(file: SynthFile, path: Path): Unit =
    val bytes = write(file)
    Files.write(path, bytes)
    scribe.info(s"Wrote ${bytes.length} byte(s) to $path")

  private def writeTones(buf: BinaryBuffer, tones: Vector[Option[Tone]]): Unit =
    for tone <- tones.take(Constants.MaxTones) do
      tone match
        case Some(t) => writeTone(buf, t)
        case None    => buf.writeU8(0)

  private def writeTone(buf: BinaryBuffer, tone: Tone): Unit =
    writeEnvelope(buf, tone.pitchEnvelope)
    writeEnvelope(buf, tone.volumeEnvelope)
    writeOptionalEnvelopePair(buf, tone.vibratoRate, tone.vibratoDepth)
    writeOptionalEnvelopePair(buf, tone.tremoloRate, tone.tremoloDepth)
    writeOptionalEnvelopePair(buf, tone.gateSilence, tone.gateDuration)
    writeHarmonics(buf, tone.harmonics)
    buf.writeSmartUnsigned(tone.reverbDelay)
    buf.writeSmartUnsigned(tone.reverbVolume)
    buf.writeU16BE(tone.duration)
    buf.writeU16BE(tone.start)
    writeFilter(buf, tone.filter)

  private def writeEnvelope(buf: BinaryBuffer, env: Envelope): Unit =
    buf.writeU8(env.form.id)
    buf.writeS32BE(env.start)
    buf.writeS32BE(env.end)
    buf.writeU8(env.segments.length)
    for seg <- env.segments do
      buf.writeU16BE(seg.duration)
      buf.writeU16BE(seg.peak)

  private def writeFilter(buf: BinaryBuffer, filter: Option[Filter]): Unit =
    filter match
      case None    => buf.writeU8(0)
      case Some(f) =>
        val p0 = f.pairCounts(0)
        val p1 = f.pairCounts(1)
        buf.writeU8((p0 << 4) | p1)

        buf.writeU16BE(f.unity(0))
        buf.writeU16BE(f.unity(1))

        var migrated = 0
        for dir <- 0 until 2 do
          val count = f.pairCounts(dir)
          for p <- 0 until count do
            val diffPhase = f.pairPhase(dir)(0)(p) != f.pairPhase(dir)(1)(p)
            val diffMag =
              f.pairMagnitude(dir)(0)(p) != f.pairMagnitude(dir)(1)(p)
            if diffPhase || diffMag then migrated |= (1 << (dir * 4 + p))

        if f.envelope.isDefined && migrated == 0 && f.unity(0) == f.unity(1)
        then
          if p0 > 0 then migrated |= 1
          else if p1 > 0 then migrated |= (1 << 4)

        buf.writeU8(migrated)

        for p <- 0 until p0 do
          buf.writeU16BE(f.pairPhase(0)(0)(p))
          buf.writeU16BE(f.pairMagnitude(0)(0)(p))
        for p <- 0 until p1 do
          buf.writeU16BE(f.pairPhase(1)(0)(p))
          buf.writeU16BE(f.pairMagnitude(1)(0)(p))

        for dir <- 0 until 2 do
          val count = f.pairCounts(dir)
          for p <- 0 until count do
            if (migrated & (1 << (dir * 4 + p))) != 0 then
              buf.writeU16BE(f.pairPhase(dir)(1)(p))
              buf.writeU16BE(f.pairMagnitude(dir)(1)(p))

        f.envelope match
          case Some(env) => writeEnvelopeSegments(buf, env)
          case None      => // do nothing

  private def writeEnvelopeSegments(buf: BinaryBuffer, env: Envelope): Unit =
    buf.writeU8(env.segments.length)
    for seg <- env.segments do
      buf.writeU16BE(seg.duration)
      buf.writeU16BE(seg.peak)

  private def writeOptionalEnvelopePair(
      buf: BinaryBuffer,
      env1: Option[Envelope],
      env2: Option[Envelope]
  ): Unit =
    (env1, env2) match
      case (Some(e1), Some(e2)) =>
        writeEnvelope(buf, e1)
        writeEnvelope(buf, e2)
      case _ =>
        buf.writeU8(0)

  private def writeHarmonics(
      buf: BinaryBuffer,
      harmonics: Vector[Harmonic]
  ): Unit =
    val activeHarmonics =
      harmonics.filter(_.volume > 0).take(Constants.MaxHarmonics)
    for h <- activeHarmonics do
      buf.writeSmartUnsigned(h.volume)
      buf.writeSmart(h.semitone)
      buf.writeSmartUnsigned(h.delay)
    buf.writeSmartUnsigned(0)
