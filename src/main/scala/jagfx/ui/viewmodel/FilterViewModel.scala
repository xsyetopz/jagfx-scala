package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._

/** `ViewModel` for `Filter` data (poles/zeros). */
class FilterViewModel extends ViewModelLike:
  // Pair counts (`0-4` each)
  val pairCount0 = new SimpleIntegerProperty(0)
  val pairCount1 = new SimpleIntegerProperty(0)

  // Unity gain (start/end)
  val unity0 = new SimpleIntegerProperty(0)
  val unity1 = new SimpleIntegerProperty(0)

  // Phase and magnitude for each pole (2 directions * 4 poles * 2 interpolation points)
  private val _phaseArrays = Array.fill(2, 4, 2)(new SimpleIntegerProperty(0))
  private val _magnitudeArrays =
    Array.fill(2, 4, 2)(new SimpleIntegerProperty(0))

  def pairPhase(dir: Int)(slot: Int)(point: Int): IntegerProperty =
    _phaseArrays(dir)(slot)(point)
  def pairMagnitude(dir: Int)(slot: Int)(point: Int): IntegerProperty =
    _magnitudeArrays(dir)(slot)(point)

  def isEmpty: Boolean = pairCount0.get == 0 && pairCount1.get == 0

  def load(filterOpt: Option[Filter]): Unit =
    filterOpt match
      case Some(f) =>
        pairCount0.set(f.pairCounts(0))
        pairCount1.set(f.pairCounts(1))
        unity0.set(f.unity(0))
        unity1.set(f.unity(1))

        for dir <- 0 until 2 do
          val maxPoles =
            if f.pairPhase.length > dir then f.pairPhase(dir)(0).length else 0
          for slot <- 0 until math.min(4, maxPoles) do
            for point <- 0 until 2 do
              if f.pairPhase.length > dir &&
                f.pairPhase(dir).length > point &&
                f.pairPhase(dir)(point).length > slot
              then
                _phaseArrays(dir)(slot)(point)
                  .set(f.pairPhase(dir)(point)(slot))
                _magnitudeArrays(dir)(slot)(point)
                  .set(f.pairMagnitude(dir)(point)(slot))

        notifyListeners()
      case None =>
        clear()

  def clear(): Unit =
    pairCount0.set(0)
    pairCount1.set(0)
    unity0.set(0)
    unity1.set(0)
    for dir <- 0 until 2; slot <- 0 until 4; point <- 0 until 2 do
      _phaseArrays(dir)(slot)(point).set(0)
      _magnitudeArrays(dir)(slot)(point).set(0)
    notifyListeners()

  def toModel(): Option[Filter] =
    if isEmpty then None
    else
      val pairPhase = Array.ofDim[Int](2, 2, 4)
      val pairMagnitude = Array.ofDim[Int](2, 2, 4)

      for dir <- 0 until 2; slot <- 0 until 4; point <- 0 until 2 do
        pairPhase(dir)(point)(slot) = _phaseArrays(dir)(slot)(point).get
        pairMagnitude(dir)(point)(slot) = _magnitudeArrays(dir)(slot)(point).get

      val phaseIArray = IArray.tabulate(2)(d =>
        IArray.tabulate(2)(p => IArray.tabulate(4)(i => pairPhase(d)(p)(i)))
      )
      val magIArray = IArray.tabulate(2)(d =>
        IArray.tabulate(2)(p => IArray.tabulate(4)(i => pairMagnitude(d)(p)(i)))
      )

      Some(
        Filter(
          IArray(pairCount0.get, pairCount1.get),
          IArray(unity0.get, unity1.get),
          phaseIArray,
          magIArray,
          None // envelope handled separately
        )
      )

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    pairCount0.addListener((_, _, _) => cb())
    pairCount1.addListener((_, _, _) => cb())
    unity0.addListener((_, _, _) => cb())
    unity1.addListener((_, _, _) => cb())
