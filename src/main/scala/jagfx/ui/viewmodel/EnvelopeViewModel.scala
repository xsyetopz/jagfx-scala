package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._

/** `ViewModel` for `Envelope` data. */
class EnvelopeViewModel extends ViewModelLike:
  val waveform = new SimpleObjectProperty[Waveform](Waveform.Square)
  val start = new SimpleIntegerProperty(0)
  val end = new SimpleIntegerProperty(0)
  val notes = new SimpleIntegerProperty(0)

  private var _segments: Vector[EnvelopeSegment] = Vector.empty

  def getSegments: Vector[Int] = _segments.map(_.peak)

  def getFullSegments: Vector[EnvelopeSegment] = _segments

  def addSegment(duration: Int, peak: Int): Unit =
    _segments = _segments :+ EnvelopeSegment(duration, peak)
    notifyListeners()

  def removeSegment(index: Int): Unit =
    if index >= 0 && index < _segments.length then
      _segments = _segments.patch(index, Nil, 1)
      notifyListeners()

  def updateSegment(index: Int, duration: Int, peak: Int): Unit =
    if index >= 0 && index < _segments.length then
      _segments = _segments.updated(index, EnvelopeSegment(duration, peak))
      notifyListeners()

  def updateSegments(updates: Seq[(Int, EnvelopeSegment)]): Unit =
    var changed = false
    var newSegments = _segments
    updates.foreach { case (index, newSeg) =>
      if index >= 0 && index < newSegments.length then
        newSegments = newSegments.updated(index, newSeg)
        changed = true
    }
    if changed then
      _segments = newSegments
      notifyListeners()

  def isEmpty: Boolean = _segments.isEmpty && waveform.get == Waveform.Off
  def isZero: Boolean =
    start.get == 0 && end.get == 0 && _segments.forall(_.peak == 0)

  def load(env: Envelope): Unit =
    waveform.set(env.waveform)
    start.set(env.start)
    end.set(env.end)
    _segments = env.segments
    notifyListeners()

  def clear(): Unit =
    waveform.set(Waveform.Off)
    start.set(0)
    end.set(0)
    _segments = Vector.empty
    notifyListeners()

  def toModel(): Envelope =
    Envelope(waveform.get, start.get, end.get, _segments)

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    waveform.addListener((_, _, _) => cb())
    start.addListener((_, _, _) => cb())
    end.addListener((_, _, _) => cb())
