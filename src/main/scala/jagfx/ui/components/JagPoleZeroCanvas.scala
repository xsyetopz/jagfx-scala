package jagfx.ui.components

import jagfx.ui.viewmodel.FilterViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.utils.MathUtils
import jagfx.utils.MathUtils.TwoPi

/** Canvas rendering pole-zero diagram on unit circle. */
class JagPoleZeroCanvas extends JagBaseCanvas:
  import JagPoleZeroCanvas._

  private var viewModel: Option[FilterViewModel] = None

  getStyleClass.add("jag-pole-zero-canvas")

  def setViewModel(vm: FilterViewModel): Unit =
    viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => draw())
    )
    draw()

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    drawGrid(buffer, w, h)
    drawUnitCircle(buffer, w, h)
    viewModel.foreach { vm =>
      drawFeedforwardPoles(buffer, w, h, vm)
      drawFeedbackPoles(buffer, w, h, vm)
    }

  private def drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    val (cx, cy) = (w / 2, h / 2)
    line(buffer, w, h, 0, cy, w, cy, GridLineFaint)
    line(buffer, w, h, cx, 0, cx, h, GridLineFaint)

  private def drawUnitCircle(buffer: Array[Int], w: Int, h: Int): Unit =
    val (cx, cy) = (w / 2, h / 2)
    val radius = math.min(w, h) / 2 - CirclePadding
    for i <- 0 until CircleSegments do
      val a1 = i * TwoPi / CircleSegments
      val a2 = (i + 1) * TwoPi / CircleSegments
      val x1 = cx + (radius * math.cos(a1)).toInt
      val y1 = cy + (radius * math.sin(a1)).toInt
      val x2 = cx + (radius * math.cos(a2)).toInt
      val y2 = cy + (radius * math.sin(a2)).toInt
      line(buffer, w, h, x1, y1, x2, y2, BorderDim)

  private def drawFeedforwardPoles(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: FilterViewModel
  ): Unit =
    val (cx, cy) = (w / 2, h / 2)
    val radius = math.min(w, h) / 2 - CirclePadding
    for i <- 0 until vm.pairCount0.get do
      val (x, y) = polePosition(vm, 0, i, cx, cy, radius)
      drawCircleMarker(buffer, w, h, x, y, FilterZero)

  private def drawFeedbackPoles(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: FilterViewModel
  ): Unit =
    val (cx, cy) = (w / 2, h / 2)
    val radius = math.min(w, h) / 2 - CirclePadding
    for i <- 0 until vm.pairCount1.get do
      val (x, y) = polePosition(vm, 1, i, cx, cy, radius)
      drawCrossMarker(buffer, w, h, x, y, FilterPole)

  private def polePosition(
      vm: FilterViewModel,
      dir: Int,
      idx: Int,
      cx: Int,
      cy: Int,
      radius: Int
  ): (Int, Int) =
    val phase = vm.pairPhase(dir)(idx)(0).get / ValueScale * TwoPi
    val mag = vm.pairMagnitude(dir)(idx)(0).get / ValueScale
    val x = cx + (radius * mag * math.cos(phase)).toInt
    val y = cy - (radius * mag * math.sin(phase)).toInt
    (x, y)

  private def drawCrossMarker(
      buffer: Array[Int],
      w: Int,
      h: Int,
      x: Int,
      y: Int,
      color: Int
  ): Unit =
    line(
      buffer,
      w,
      h,
      x - MarkerSize,
      y - MarkerSize,
      x + MarkerSize,
      y + MarkerSize,
      color
    )
    line(
      buffer,
      w,
      h,
      x - MarkerSize,
      y + MarkerSize,
      x + MarkerSize,
      y - MarkerSize,
      color
    )

  private def drawCircleMarker(
      buffer: Array[Int],
      w: Int,
      h: Int,
      x: Int,
      y: Int,
      color: Int
  ): Unit =
    for i <- 0 until MarkerCircleSegments do
      val a1 = i * TwoPi / MarkerCircleSegments
      val a2 = (i + 1) * TwoPi / MarkerCircleSegments
      val x1 = x + (MarkerSize * math.cos(a1)).toInt
      val y1 = y + (MarkerSize * math.sin(a1)).toInt
      val x2 = x + (MarkerSize * math.cos(a2)).toInt
      val y2 = y + (MarkerSize * math.sin(a2)).toInt
      line(buffer, w, h, x1, y1, x2, y2, color)

object JagPoleZeroCanvas:
  private val CircleSegments = 64
  private val MarkerCircleSegments = 8
  private val CirclePadding = 4
  private val MarkerSize = 3
  private val ValueScale = 65535.0

  def apply(): JagPoleZeroCanvas = new JagPoleZeroCanvas()
