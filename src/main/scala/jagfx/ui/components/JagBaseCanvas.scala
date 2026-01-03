package jagfx.ui.components

import javafx.scene.canvas.Canvas
import javafx.scene.image._
import javafx.scene.input._
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._

/** Base canvas with shared buffer management and drawing utilities. */
abstract class JagBaseCanvas extends Canvas:
  protected var image: WritableImage = null
  protected var buffer: Array[Int] = Array.empty
  protected val pixelFormat: PixelFormat[java.nio.IntBuffer] =
    PixelFormat.getIntArgbInstance
  protected var zoomLevel: Int = 1
  protected var panOffset: Int = 0 // X offset in pixels for panning

  setWidth(200)
  setHeight(100)

  def setZoom(level: Int): Unit =
    zoomLevel = level
    panOffset = 0
    draw()

  def getPanOffset: Int = panOffset

  protected def resizeBuffer(w: Int, h: Int): Unit =
    if w > 0 && h > 0 then
      image = new WritableImage(w, h)
      buffer = new Array[Int](w * h)
      draw()

  def draw(): Unit =
    val w = getWidth.toInt
    val h = getHeight.toInt

    if buffer.length != w * h then resizeBuffer(w, h)
    if buffer.isEmpty then return

    clear(buffer, BgBlack)
    drawContent(buffer, w, h)

    val pw = image.getPixelWriter
    pw.setPixels(0, 0, w, h, pixelFormat, buffer, 0, w)

    val gc = getGraphicsContext2D
    gc.drawImage(image, 0, 0)

  /** Draw specific content for this canvas type. */
  protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit

  /** Draw center line (zero crossing / midpoint). */
  protected def drawCenterLine(buffer: Array[Int], w: Int, h: Int): Unit =
    val midY = h / 2
    line(buffer, w, h, 0, midY, w, midY, White)

  /** Max pan offset based on zoom level and width. */
  protected def maxPanOffset: Int =
    val w = getWidth.toInt
    math.max(0, (w * zoomLevel) - w)

  /** Update pan offset, clamping to valid range. */
  protected def setPan(offset: Int): Unit =
    val clamped = math.max(0, math.min(maxPanOffset, offset))
    if panOffset != clamped then
      panOffset = clamped
      draw()

  // scroll up = right, scroll down = left
  setOnScroll((e: ScrollEvent) =>
    if zoomLevel > 1 then
      val delta = if e.getDeltaY > 0 then 20 else -20
      setPan(panOffset + delta)
      e.consume()
  )

  private var dragStartX: Double = 0
  private var dragStartPan: Int = 0

  setOnMousePressed((e: MouseEvent) =>
    if zoomLevel > 1 then
      dragStartX = e.getX
      dragStartPan = panOffset
  )

  setOnMouseDragged((e: MouseEvent) =>
    if zoomLevel > 1 then
      val delta = (dragStartX - e.getX).toInt
      setPan(dragStartPan + delta)
  )

  widthProperty.addListener((_, _, _) => draw())
  heightProperty.addListener((_, _, _) => draw())
