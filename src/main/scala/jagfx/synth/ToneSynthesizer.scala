package jagfx.synth

import jagfx.model._
import jagfx.Constants
import jagfx.Constants.{Int16, PhaseScale, NoisePhaseDiv, PhaseMask}
import jagfx.utils.MathUtils.{clamp, clipInt16}

/** Synthesizes single `Tone` into audio samples using FM/AM modulation and
  * additive synthesis.
  */
object ToneSynthesizer:
  /** Generates audio samples from `Tone` definition.
    *
    * Returns `AudioBuffer` with rendered samples.
    */
  def synthesize(tone: Tone): AudioBuffer =
    val sampleCount = tone.duration * Constants.SampleRate / 1000
    if sampleCount <= 0 || tone.duration < 10 then return AudioBuffer.empty(0)

    val samplesPerStep = sampleCount.toDouble / tone.duration.toDouble
    val buffer = BufferPool.acquire(sampleCount)

    val state = _initSynthState(tone, samplesPerStep, sampleCount)
    _renderSamples(buffer, tone, state, sampleCount)

    _applyGating(buffer, tone, sampleCount)
    _applyReverb(buffer, tone, samplesPerStep, sampleCount)

    tone.filter.foreach { f =>
      FilterSynthesizer.apply(buffer, f, sampleCount)
    }

    clipInt16(buffer, sampleCount)

    val output = new Array[Int](sampleCount)
    System.arraycopy(buffer, 0, output, 0, sampleCount)
    BufferPool.release(buffer)
    AudioBuffer(output, Constants.SampleRate)

  private case class SynthState(
      freqBaseEval: EnvelopeEvaluator,
      ampBaseEval: EnvelopeEvaluator,
      freqModRateEval: Option[EnvelopeEvaluator],
      freqModRangeEval: Option[EnvelopeEvaluator],
      ampModRateEval: Option[EnvelopeEvaluator],
      ampModRangeEval: Option[EnvelopeEvaluator],
      frequencyStart: Int,
      frequencyDuration: Int,
      amplitudeStart: Int,
      amplitudeDuration: Int,
      harmonicDelays: Array[Int],
      harmonicVolumes: Array[Int],
      harmonicSemitones: Array[Int],
      harmonicStarts: Array[Int]
  )

  private def _initSynthState(
      tone: Tone,
      samplesPerStep: Double,
      sampleCount: Int
  ): SynthState =
    val freqBaseEval = EnvelopeEvaluator(tone.pitchEnvelope)
    val ampBaseEval = EnvelopeEvaluator(tone.volumeEnvelope)
    freqBaseEval.reset()
    ampBaseEval.reset()

    val (freqModRateEval, freqModRangeEval, frequencyStart, frequencyDuration) =
      _initFrequencyModulation(tone, samplesPerStep)

    val (ampModRateEval, ampModRangeEval, amplitudeStart, amplitudeDuration) =
      _initAmplitudeModulation(tone, samplesPerStep)

    val (delays, volumes, semitones, starts) =
      _initHarmonics(tone, samplesPerStep)

    SynthState(
      freqBaseEval,
      ampBaseEval,
      freqModRateEval,
      freqModRangeEval,
      ampModRateEval,
      ampModRangeEval,
      frequencyStart,
      frequencyDuration,
      amplitudeStart,
      amplitudeDuration,
      delays,
      volumes,
      semitones,
      starts
    )

  private def _initFrequencyModulation(
      tone: Tone,
      samplesPerStep: Double
  ): (Option[EnvelopeEvaluator], Option[EnvelopeEvaluator], Int, Int) =
    tone.vibratoRate match
      case Some(env) =>
        val rateEval = EnvelopeEvaluator(env)
        val rangeEval = tone.vibratoDepth.map(EnvelopeEvaluator(_))
        rateEval.reset()
        rangeEval.foreach(_.reset())
        val start = ((env.end - env.start) * PhaseScale / samplesPerStep).toInt
        val duration = (env.start * PhaseScale / samplesPerStep).toInt
        (Some(rateEval), rangeEval, start, duration)
      case None =>
        (None, None, 0, 0)

  private def _initAmplitudeModulation(
      tone: Tone,
      samplesPerStep: Double
  ): (Option[EnvelopeEvaluator], Option[EnvelopeEvaluator], Int, Int) =
    tone.tremoloRate match
      case Some(env) =>
        val rateEval = EnvelopeEvaluator(env)
        val rangeEval = tone.tremoloDepth.map(EnvelopeEvaluator(_))
        rateEval.reset()
        rangeEval.foreach(_.reset())
        val start = ((env.end - env.start) * PhaseScale / samplesPerStep).toInt
        val duration = (env.start * PhaseScale / samplesPerStep).toInt
        (Some(rateEval), rangeEval, start, duration)
      case None =>
        (None, None, 0, 0)

  private def _initHarmonics(
      tone: Tone,
      samplesPerStep: Double
  ): (Array[Int], Array[Int], Array[Int], Array[Int]) =
    val delays = new Array[Int](Constants.MaxHarmonics)
    val volumes = new Array[Int](Constants.MaxHarmonics)
    val semitones = new Array[Int](Constants.MaxHarmonics)
    val starts = new Array[Int](Constants.MaxHarmonics)

    for harmonic <- 0 until math.min(
        Constants.MaxHarmonics,
        tone.harmonics.length
      )
    do
      val h = tone.harmonics(harmonic)
      if h.volume != 0 then
        delays(harmonic) = (h.delay * samplesPerStep).toInt
        volumes(harmonic) = (h.volume << 14) / 100
        semitones(harmonic) =
          ((tone.pitchEnvelope.end - tone.pitchEnvelope.start) * PhaseScale *
            LookupTables.getSemitoneMultiplier(
              h.semitone
            ) / samplesPerStep).toInt
        starts(harmonic) =
          (tone.pitchEnvelope.start * PhaseScale / samplesPerStep).toInt

    (delays, volumes, semitones, starts)

  private def _renderSamples(
      buffer: Array[Int],
      tone: Tone,
      state: SynthState,
      sampleCount: Int
  ): Unit =
    val phases = new Array[Int](Constants.MaxHarmonics)
    var frequencyPhase = 0
    var amplitudePhase = 0

    for sample <- 0 until sampleCount do
      var frequency = state.freqBaseEval.evaluate(sampleCount)
      var amplitude = state.ampBaseEval.evaluate(sampleCount)

      val (newFreq, newFreqPhase) = _applyVibrato(
        frequency,
        frequencyPhase,
        sampleCount,
        state,
        tone
      )
      frequency = newFreq
      frequencyPhase = newFreqPhase
      val (newAmp, newAmpPhase) = _applyTremolo(
        amplitude,
        amplitudePhase,
        sampleCount,
        state,
        tone
      )
      amplitude = newAmp
      amplitudePhase = newAmpPhase

      _renderHarmonics(
        buffer,
        tone,
        state,
        sample,
        sampleCount,
        frequency,
        amplitude,
        phases
      )

  private def _applyVibrato(
      frequency: Int,
      phase: Int,
      sampleCount: Int,
      state: SynthState,
      tone: Tone
  ): (Int, Int) =
    (state.freqModRateEval, state.freqModRangeEval) match
      case (Some(rateEval), Some(rangeEval)) =>
        val rate = rateEval.evaluate(sampleCount)
        val range = rangeEval.evaluate(sampleCount)
        val mod = _generateSample(
          range,
          phase,
          tone.vibratoRate.get.form
        ) >> 1
        val nextPhase =
          phase + (rate * state.frequencyStart >> 16) + state.frequencyDuration
        (frequency + mod, nextPhase)
      case _ => (frequency, phase)

  private def _applyTremolo(
      amplitude: Int,
      phase: Int,
      sampleCount: Int,
      state: SynthState,
      tone: Tone
  ): (Int, Int) =
    (state.ampModRateEval, state.ampModRangeEval) match
      case (Some(rateEval), Some(rangeEval)) =>
        val rate = rateEval.evaluate(sampleCount)
        val range = rangeEval.evaluate(sampleCount)
        val mod = _generateSample(
          range,
          phase,
          tone.tremoloRate.get.form
        ) >> 1
        val newAmp =
          amplitude * (mod + Constants.Int16.UnsignedMaxValue) >> 15
        val nextPhase =
          phase + (rate * state.amplitudeStart >> 16) + state.amplitudeDuration
        (newAmp, nextPhase)
      case _ => (amplitude, phase)

  private def _renderHarmonics(
      buffer: Array[Int],
      tone: Tone,
      state: SynthState,
      sample: Int,
      sampleCount: Int,
      frequency: Int,
      amplitude: Int,
      phases: Array[Int]
  ): Unit =
    for harmonic <- 0 until math.min(
        Constants.MaxHarmonics,
        tone.harmonics.length
      )
    do
      if tone.harmonics(harmonic).volume != 0 then
        val position = sample + state.harmonicDelays(harmonic)
        if position >= 0 && position < sampleCount then
          buffer(position) += _generateSample(
            amplitude * state.harmonicVolumes(harmonic) >> 15,
            phases(harmonic),
            tone.pitchEnvelope.form
          )
          phases(harmonic) += (frequency * state.harmonicSemitones(
            harmonic
          ) >> 16) + state.harmonicStarts(harmonic)

  private def _generateSample(amplitude: Int, phase: Int, form: WaveForm): Int =
    form match
      case WaveForm.Square =>
        if (phase & PhaseMask) < Int16.Quarter then amplitude else -amplitude
      case WaveForm.Sine =>
        (LookupTables.sin(phase & PhaseMask) * amplitude) >> 14
      case WaveForm.Saw => (((phase & PhaseMask) * amplitude) >> 14) - amplitude
      case WaveForm.Noise =>
        LookupTables.noise((phase / NoisePhaseDiv) & PhaseMask) * amplitude
      case WaveForm.Off => 0

  private def _applyGating(
      buffer: Array[Int],
      tone: Tone,
      sampleCount: Int
  ): Unit =
    (tone.gateSilence, tone.gateDuration) match
      case (Some(release), Some(attack)) =>
        val releaseEval = EnvelopeEvaluator(release)
        val attackEval = EnvelopeEvaluator(attack)
        releaseEval.reset()
        attackEval.reset()
        var counter = 0
        var muted = true
        for sample <- 0 until sampleCount do
          val stepOn = releaseEval.evaluate(sampleCount)
          val stepOff = attackEval.evaluate(sampleCount)
          val threshold =
            if muted then
              release.start + ((release.end - release.start) * stepOn >> 8)
            else release.start + ((release.end - release.start) * stepOff >> 8)
          counter += 256
          if counter >= threshold then
            counter = 0
            muted = !muted
          if muted then buffer(sample) = 0
      case _ => ()

  private def _applyReverb(
      buffer: Array[Int],
      tone: Tone,
      samplesPerStep: Double,
      sampleCount: Int
  ): Unit =
    if tone.reverbDelay > 0 && tone.reverbVolume > 0 then
      val start = (tone.reverbDelay * samplesPerStep).toInt
      for sample <- start until sampleCount do
        buffer(sample) += buffer(sample - start) * tone.reverbVolume / 100
