package jagfx.ui.components

import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.image._
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.DrawingUtils._

/** Canvas rendering envelope segments. */
class JagEnvelopeCanvas extends Canvas:
  private var viewModel: Option[EnvelopeViewModel] = None

  getStyleClass.add("jag-envelope-canvas")
  setWidth(200)
  setHeight(100)

  private var image: WritableImage = null
  private var buffer: Array[Int] = Array.empty
  private val pixelFormat = PixelFormat.getIntArgbInstance

  def setViewModel(vm: EnvelopeViewModel): Unit =
    viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => draw())
    )
    draw()

  private def resizeBuffer(w: Int, h: Int): Unit =
    if w > 0 && h > 0 then
      image = new WritableImage(w, h)
      buffer = new Array[Int](w * h)
      draw()

  def draw(): Unit =
    val w = getWidth.toInt
    val h = getHeight.toInt

    if buffer.length != w * h then resizeBuffer(w, h)

    if buffer.isEmpty then return

    import jagfx.utils.DrawingUtils._
    import jagfx.utils.ColorUtils._

    clear(buffer, BgBlack)
    drawGrid(buffer, w, h)
    viewModel.foreach(vm => drawEnvelope(buffer, w, h, vm))

    val pw = image.getPixelWriter
    pw.setPixels(0, 0, w, h, pixelFormat, buffer, 0, w)

    val gc = getGraphicsContext2D
    gc.drawImage(image, 0, 0)

  private def drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    import jagfx.utils.DrawingUtils._
    import jagfx.utils.ColorUtils._

    // 8 cols
    for i <- 1 until 8 do
      val x = i * w / 8
      line(buffer, w, h, x, 0, x, h, GridLineFaint)
    // 4 rows
    for i <- 1 until 4 do
      val y = i * h / 4
      line(buffer, w, h, 0, y, w, y, GridLineFaint)

    // midpoint
    val midY = h / 2
    line(buffer, w, h, 0, midY, w, midY, White)

  private def drawEnvelope(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: EnvelopeViewModel
  ): Unit =
    import jagfx.utils.DrawingUtils._
    import jagfx.utils.ColorUtils._

    val segments = vm.getSegments
    if segments.nonEmpty then
      val step = w.toDouble / math.max(1, segments.length - 1)

      // draw 1st point
      var prevX = 0
      var prevY = h - (segments(0) / 65535.0 * h).toInt
      fillRect(buffer, w, h, prevX - 1, prevY - 1, 3, 3, Graph)

      for i <- 1 until segments.length do
        val x = (i * step).toInt
        val y = h - (segments(i) / 65535.0 * h).toInt
        line(buffer, w, h, prevX, prevY, x, y, Graph)
        fillRect(buffer, w, h, x - 1, y - 1, 3, 3, Graph)
        prevX = x
        prevY = y

  widthProperty.addListener((_, _, _) => draw())
  heightProperty.addListener((_, _, _) => draw())

object JagEnvelopeCanvas:
  def apply(): JagEnvelopeCanvas = new JagEnvelopeCanvas()
