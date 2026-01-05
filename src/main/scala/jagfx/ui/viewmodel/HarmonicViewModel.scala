package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model.Harmonic

/** `ViewModel` for `Harmonic` data. */
class HarmonicViewModel extends ViewModelLike:
  val active = new SimpleBooleanProperty(false)
  val semitone = new SimpleIntegerProperty(0)
  val volume = new SimpleIntegerProperty(0)
  val delay = new SimpleIntegerProperty(0)

  def load(h: Harmonic): Unit =
    active.set(true)
    semitone.set(h.semitone)
    volume.set(h.volume)
    delay.set(h.delay)
    notifyListeners()

  def clear(): Unit =
    active.set(false)
    semitone.set(0)
    volume.set(0)
    delay.set(0)
    notifyListeners()

  def toModel(): Harmonic =
    Harmonic(volume.get, semitone.get, delay.get)

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    active.addListener((_, _, _) => cb())
    semitone.addListener((_, _, _) => cb())
    volume.addListener((_, _, _) => cb())
    delay.addListener((_, _, _) => cb())
