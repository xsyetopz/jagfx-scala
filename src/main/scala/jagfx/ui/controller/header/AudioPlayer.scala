package jagfx.ui.controller.header

import javax.sound.sampled._
import javafx.animation.AnimationTimer
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.synth.TrackSynthesizer
import jagfx.Constants
import jagfx.utils.AudioUtils

/** Audio playback controller for synth. */
class AudioPlayer(viewModel: SynthViewModel):
  private var currentClip: Option[Clip] = None
  private var playheadTimer: Option[AnimationTimer] = None

  /** Callback for playhead position updates (`0.0` to `1.0`, or `-1` when
    * stopped).
    */
  var onPlayheadUpdate: Double => Unit = _ => ()

  def play(): Unit =
    stop()

    val toneFilter =
      if viewModel.isTargetAll then -1 else viewModel.getActiveToneIndex
    val loopCount =
      if viewModel.isLoopEnabled then viewModel.loopCountProperty.get else 1
    val audio =
      TrackSynthesizer.synthesize(viewModel.toModel(), loopCount, toneFilter)

    val clip = AudioSystem.getClip()
    currentClip = Some(clip)

    val format = new AudioFormat(Constants.SampleRate, 16, 1, true, true)
    val bytes = audio.toBytes16BE
    clip.open(format, bytes, 0, bytes.length)

    if viewModel.isLoopEnabled && configureLoopPoints(clip) then
      val count = viewModel.loopCountProperty.get
      clip.loop(if count == 0 then Clip.LOOP_CONTINUOUSLY else count - 1)
    else clip.start()

    val totalFrames = clip.getFrameLength.toDouble
    val timer = new AnimationTimer:
      def handle(now: Long): Unit =
        currentClip.foreach { c =>
          if c.isRunning then
            val pos = c.getFramePosition.toDouble / totalFrames
            onPlayheadUpdate(pos)
          else
            onPlayheadUpdate(-1)
            this.stop()
        }
    timer.start()
    playheadTimer = Some(timer)

  /** Configures loop points on clip.
    *
    * Returns `true` if loop points are valid.
    */
  private def configureLoopPoints(clip: Clip): Boolean =
    val startMs = math.max(0, viewModel.loopStartProperty.get)
    val endMs = math.max(startMs, viewModel.loopEndProperty.get)
    if endMs <= startMs then return false

    val startFrames = AudioUtils.msToSamples(startMs)
    val endFrames = AudioUtils.msToSamples(endMs)
    val len = clip.getFrameLength

    val validEnd = math.min(endFrames, len - 1).toInt
    val validStart = math.min(startFrames, validEnd).toInt
    if validEnd > validStart then
      clip.setLoopPoints(validStart, validEnd)
      true
    else false

  def stop(): Unit =
    playheadTimer.foreach(_.stop())
    playheadTimer = None
    onPlayheadUpdate(-1)
    currentClip.foreach { clip =>
      if clip.isRunning then clip.stop()
      clip.close()
    }
    currentClip = None
