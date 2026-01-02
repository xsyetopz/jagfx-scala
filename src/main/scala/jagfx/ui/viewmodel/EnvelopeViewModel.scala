package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._

/** `ViewModel` for `Envelope` data. */
class EnvelopeViewModel:
  val form = new SimpleObjectProperty[WaveForm](WaveForm.Square)
  val start = new SimpleIntegerProperty(0)
  val end = new SimpleIntegerProperty(0)
  val notes = new SimpleIntegerProperty(
    0
  ) // FIXME: segment count but half-arsed -- shall fix maybe?

  private var segments: Vector[Int] = Vector.empty
  private var listeners: List[() => Unit] = Nil

  def getSegments: Vector[Int] = segments
  def isEmpty: Boolean = form.get == WaveForm.Off

  def load(env: Envelope): Unit =
    form.set(env.form)
    start.set(env.start)
    end.set(env.end)
    segments = env.segments.map(_.peak)
    notifyListeners()

  def clear(): Unit =
    form.set(WaveForm.Off)
    start.set(0)
    end.set(0)
    segments = Vector.empty
    notifyListeners()

  def toModel(): Envelope =
    val segs =
      segments.map(p =>
        EnvelopeSegment(0, p)
      ) // NOTE: duration calc needed later
    Envelope(form.get, start.get, end.get, segs)

  def addChangeListener(listener: () => Unit): Unit =
    listeners = listener :: listeners

  private def notifyListeners(): Unit =
    listeners.foreach(_())
