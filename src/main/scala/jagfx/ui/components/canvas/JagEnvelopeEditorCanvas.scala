package jagfx.ui.components.canvas

import javafx.scene.input.MouseEvent
import javafx.scene.Cursor
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.utils.MathUtils
import jagfx.constants.Int16
import jagfx.model.EnvelopeSegment
import javafx.application.Platform
import scala.collection.mutable.ListBuffer

private val _PointRadius = 5
private val _HitRadius = 8
private val _SegmentHitThreshold = 6

/** Interactive canvas for envelope editing with draggable control points.
  */
class JagEnvelopeEditorCanvas extends JagBaseCanvas:
  private enum HoverTarget:
    case None
    case Point(index: Int)
    case Segment(index: Int)

  private object DragState:
    var primaryIndex: Int = -1
    var dragging: Boolean = false
    var lockTime: Boolean = false
    var startY: Double = 0
    var startPeaks: Map[Int, Int] = Map.empty
    var startDurations: Map[Int, Int] = Map.empty
    var startAbsTimes: Map[Int, Int] = Map.empty
    var totalTimeScale: Double = 0.0

    def capture(
        viewModel: EnvelopeViewModel,
        selection: Set[Int],
        y: Double,
        width: Double
    ): Unit =
      dragging = true
      startY = y
      val segments = viewModel.getFullSegments

      startPeaks = selection.flatMap { idx =>
        if idx < segments.length then Some(idx -> segments(idx).peak) else None
      }.toMap

      startDurations = segments.zipWithIndex.map { case (s, i) =>
        i -> s.duration
      }.toMap

      var t = 0
      val absTimes = segments.indices.map { i =>
        if i > 0 then t += segments(i).duration
        i -> t
      }.toMap

      startAbsTimes = selection.flatMap { idx =>
        absTimes.get(idx).map(idx -> _)
      }.toMap

      val totalDur =
        (1 until segments.length).map(k => segments(k).duration).sum.toDouble
      totalTimeScale = if totalDur > 0 then width / totalDur else 0.0

  private object MarqueeState:
    var isSelecting: Boolean = false
    var start: (Double, Double) = (0.0, 0.0)
    var end: (Double, Double) = (0.0, 0.0)

  private var _viewModel: Option[EnvelopeViewModel] = None
  private var _hoverTarget: HoverTarget = HoverTarget.None
  private var _selection: Set[Int] = Set.empty

  getStyleClass.add("jag-envelope-editor-canvas")

  def setViewModel(vm: EnvelopeViewModel): Unit =
    _viewModel = Some(vm)
    vm.addChangeListener(() => Platform.runLater(() => requestRedraw()))
    requestRedraw()

  def getViewModel: Option[EnvelopeViewModel] = _viewModel

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    _drawGrid(buffer, w, h)
    drawCenterLine(buffer, w, h)
    _viewModel.foreach { vm =>
      val segments = vm.getFullSegments
      val xs = _computePointXs(w, segments)
      _drawEnvelope(buffer, w, h, segments, xs)
      _drawControlPoints(buffer, w, h, segments, xs)
      _drawSelectionRect(buffer, w, h)
    }

  private def _computePointXs(
      w: Int,
      segments: Vector[EnvelopeSegment]
  ): Vector[Int] =
    if segments.isEmpty then Vector.empty
    else
      var t = 0
      val pointTimes = segments.indices.map { i =>
        if i == 0 then 0
        else
          t += segments(i).duration
          t
      }.toVector

      val totalTime = pointTimes.lastOption.getOrElse(0).toDouble
      if totalTime <= 1e-3 then
        val step = w.toDouble / math.max(1, segments.length - 1)
        segments.indices.map(i => (i * step).toInt).toVector
      else
        val scale = w / totalTime
        pointTimes.map(t => (t * scale).toInt)

  private def _drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    val majorCols = 8
    for i <- 1 until majorCols do
      val x = i * w / majorCols
      line(buffer, w, h, x, 0, x, h, GridLineFaint)
    val rows = 4
    for i <- 1 until rows do
      val y = i * h / rows
      line(buffer, w, h, 0, y, w, y, GridLineFaint)

  private def _drawEnvelope(
      buffer: Array[Int],
      w: Int,
      h: Int,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int]
  ): Unit =
    if segments.nonEmpty && xs.length == segments.length then
      val range = Int16.Range.toDouble
      var prevX = xs(0)
      var prevY = ((1.0 - segments(0).peak / range) * h).toInt

      for i <- 1 until segments.length do
        val x = xs(i)
        val y = ((1.0 - segments(i).peak / range) * h).toInt

        val color = _hoverTarget match
          case HoverTarget.Segment(idx) if idx == i => White
          case _                                    => Graph
        if color == White then
          line(buffer, w, h, prevX, prevY - 1, x, y - 1, color)
          line(buffer, w, h, prevX, prevY + 1, x, y + 1, color)

        line(buffer, w, h, prevX, prevY, x, y, color)
        prevX = x
        prevY = y

  private def _drawControlPoints(
      buffer: Array[Int],
      w: Int,
      h: Int,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int]
  ): Unit =
    if segments.nonEmpty && xs.length == segments.length then
      val range = Int16.Range.toDouble
      for i <- segments.indices do
        val x = xs(i)
        val y = ((1.0 - segments(i).peak / range) * h).toInt

        val color =
          if _selection.contains(i) then PointSelected
          else
            _hoverTarget match
              case HoverTarget.Point(idx) if idx == i => PointHover
              case _                                  => PointNormal

        fillCircle(buffer, w, h, x, y, _PointRadius, color)
        drawCircle(buffer, w, h, x, y, _PointRadius, White)

  private def _drawSelectionRect(buffer: Array[Int], w: Int, h: Int): Unit =
    if MarqueeState.isSelecting then
      val rx =
        math.min(MarqueeState.start._1, MarqueeState.end._1).toInt
      val ry =
        math.min(MarqueeState.start._2, MarqueeState.end._2).toInt
      val rw = math
        .abs(MarqueeState.end._1 - MarqueeState.start._1)
        .toInt
      val rh = math
        .abs(MarqueeState.end._2 - MarqueeState.start._2)
        .toInt

      line(buffer, w, h, rx, ry, rx + rw, ry, White)
      line(buffer, w, h, rx, ry + rh, rx + rw, ry + rh, White)
      line(buffer, w, h, rx, ry, rx, ry + rh, White)
      line(buffer, w, h, rx + rw, ry, rx + rw, ry + rh, White)

  private def _hitTest(mx: Double, my: Double): HoverTarget =
    _viewModel match
      case None     => HoverTarget.None
      case Some(vm) =>
        val segments = vm.getFullSegments
        if segments.isEmpty then HoverTarget.None
        else
          val w = getWidth.toInt
          val h = getHeight.toInt
          val xs = _computePointXs(w, segments)
          val range = Int16.Range.toDouble

          _hitTestPoint(mx, my, segments, xs, h, range)
            .map(HoverTarget.Point(_))
            .orElse(
              _hitTestSegment(mx, my, segments, xs, h, range)
                .map(HoverTarget.Segment(_))
            )
            .getOrElse(HoverTarget.None)

  private def _hitTestPoint(
      mx: Double,
      my: Double,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int],
      h: Int,
      range: Double
  ): Option[Int] =
    val idx = segments.indices.indexWhere { i =>
      val x = xs(i)
      val y = (1.0 - segments(i).peak / range) * h
      val dist = math.sqrt((mx - x) * (mx - x) + (my - y) * (my - y))
      dist <= _HitRadius
    }
    if idx >= 0 then Some(idx) else None

  private def _hitTestSegment(
      mx: Double,
      my: Double,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int],
      h: Int,
      range: Double
  ): Option[Int] =
    val idx = (1 until segments.length).indexWhere { i =>
      val x1 = xs(i - 1)
      val y1 = (1.0 - segments(i - 1).peak / range) * h
      val x2 = xs(i)
      val y2 = (1.0 - segments(i).peak / range) * h
      MathUtils.distanceToSegment(
        mx,
        my,
        x1,
        y1,
        x2,
        y2
      ) <= _SegmentHitThreshold
    }
    if idx >= 0 then Some(idx + 1) else None

  setOnMouseMoved((e: MouseEvent) =>
    val newHover = _hitTest(e.getX, e.getY)
    if newHover != _hoverTarget then
      _hoverTarget = newHover
      setCursor(_hoverTarget match
        case HoverTarget.Segment(_) => Cursor.V_RESIZE
        case HoverTarget.Point(_)   => Cursor.DEFAULT
        case _                      => Cursor.DEFAULT)
      requestRedraw()
  )

  setOnMousePressed((e: MouseEvent) =>
    val target = _hitTest(e.getX, e.getY)
    DragState.dragging = false
    MarqueeState.isSelecting = false

    target match
      case HoverTarget.Point(idx) =>
        if e.isShortcutDown || e.isShiftDown then
          if _selection.contains(idx) then _selection -= idx
          else _selection += idx
        else if !_selection.contains(idx) then _selection = Set(idx)

        DragState.primaryIndex = idx
        DragState.lockTime = false

        _viewModel.foreach { vm =>
          DragState.capture(vm, _selection, e.getY, getWidth)
        }

      case HoverTarget.Segment(idx) =>
        if !e.isShortcutDown && !e.isShiftDown then
          _selection = Set(idx - 1, idx)
        else _selection ++= Set(idx - 1, idx)

        DragState.primaryIndex = idx
        DragState.lockTime = true

        _viewModel.foreach { vm =>
          DragState.capture(vm, _selection, e.getY, getWidth)
        }

      case HoverTarget.None =>
        if !e.isShortcutDown && !e.isShiftDown then _selection = Set.empty
        MarqueeState.isSelecting = true
        MarqueeState.start = (e.getX, e.getY)
        MarqueeState.end = (e.getX, e.getY)

    requestRedraw()
  )

  setOnMouseDragged((e: MouseEvent) =>
    if DragState.dragging then _handleDrag(e)
    else if MarqueeState.isSelecting then
      MarqueeState.end = (e.getX, e.getY)
      requestRedraw()
  )

  private def _handleDrag(e: MouseEvent): Unit =
    if DragState.primaryIndex >= 0 then
      _viewModel.foreach { vm =>
        val segments = vm.getFullSegments
        if DragState.primaryIndex < segments.length then
          val peakDelta = _calculatePeakDelta(e.getY, segments)
          val timeDelta = _calculateTimeDelta(e.getX, segments)

          val updates = _applyUpdates(segments, peakDelta, timeDelta)

          if updates.nonEmpty then vm.updateSegments(updates.toSeq)
          requestRedraw()
          e.consume()
      }

  private def _calculatePeakDelta(
      y: Double,
      segments: Vector[EnvelopeSegment]
  ): Int =
    val newPrimaryPeak = _calculatePeak(y)
    val startPeak = DragState.startPeaks.getOrElse(
      DragState.primaryIndex,
      segments(DragState.primaryIndex).peak
    )
    newPrimaryPeak - startPeak

  private def _calculateTimeDelta(
      x: Double,
      segments: Vector[EnvelopeSegment]
  ): Int =
    if DragState.lockTime || DragState.totalTimeScale <= 0 then 0
    else
      val mouseTime = (x / DragState.totalTimeScale).toInt
      val startAbs =
        DragState.startAbsTimes.getOrElse(DragState.primaryIndex, 0)
      val rawDelta = mouseTime - startAbs

      var minDelta = -Int16.Range * segments.length
      var maxDelta = Int16.Range * segments.length

      _selection.foreach { idx =>
        if idx > 0 && !_selection.contains(idx - 1) then
          val d = DragState.startDurations.getOrElse(idx, 0)
          minDelta = math.max(minDelta, -d)
          maxDelta = math.min(maxDelta, Int16.Range - 1 - d)
        if idx < segments.length - 1 && !_selection.contains(idx + 1) then
          val d = DragState.startDurations.getOrElse(idx + 1, 0)
          maxDelta = math.min(maxDelta, d)
          minDelta = math.max(minDelta, d - (Int16.Range - 1))
      }

      rawDelta.max(minDelta).min(maxDelta)

  private def _applyUpdates(
      segments: Vector[EnvelopeSegment],
      peakDelta: Int,
      timeDelta: Int
  ): ListBuffer[(Int, EnvelopeSegment)] =
    val updates = ListBuffer[(Int, EnvelopeSegment)]()

    _selection.foreach { idx =>
      if idx < segments.length then
        val originPeak =
          DragState.startPeaks.getOrElse(idx, segments(idx).peak)
        val peak = (originPeak + peakDelta).max(0).min(Int16.Range - 1)
        updates += (idx -> EnvelopeSegment(segments(idx).duration, peak))
    }

    val affectedIndices = _selection ++ _selection.map(_ + 1)

    affectedIndices.foreach { segIdx =>
      if segIdx > 0 && segIdx < segments.length then
        val isStartSelected = _selection.contains(segIdx - 1)
        val isEndSelected = _selection.contains(segIdx)

        val oldDur = DragState.startDurations.getOrElse(segIdx, 0)
        var newDur = oldDur

        if isEndSelected && !isStartSelected then newDur = oldDur + timeDelta
        else if isStartSelected && !isEndSelected then
          newDur = oldDur - timeDelta

        val index = updates.indexWhere(_._1 == segIdx)
        if index >= 0 then
          val current = updates(index)._2
          updates(index) = segIdx -> EnvelopeSegment(newDur, current.peak)
        else updates += segIdx -> EnvelopeSegment(newDur, segments(segIdx).peak)
    }
    updates

  private def _calculatePeak(y: Double): Int =
    val h = getHeight
    val range = Int16.Range.toDouble
    val normalizedY = 1.0 - (y / h)
    (normalizedY * range).toInt.max(0).min(Int16.Range - 1)

  setOnMouseReleased((e: MouseEvent) =>
    if MarqueeState.isSelecting then
      _viewModel.foreach { vm =>
        val segments = vm.getFullSegments
        val range = Int16.Range.toDouble
        val h = getHeight.toInt
        val w = getWidth.toInt
        val xs = _computePointXs(w, segments)

        val rx = math.min(MarqueeState.start._1, MarqueeState.end._1)
        val ry = math.min(MarqueeState.start._2, MarqueeState.end._2)
        val rw = math.abs(MarqueeState.end._1 - MarqueeState.start._1)
        val rh = math.abs(MarqueeState.end._2 - MarqueeState.start._2)

        val newSelection = segments.indices.filter { i =>
          val x = xs(i)
          val y = (1.0 - segments(i).peak / range) * h
          x >= rx && x <= rx + rw && y >= ry && y <= ry + rh
        }.toSet

        if e.isShiftDown || e.isShortcutDown then _selection ++= newSelection
        else _selection = newSelection
      }
      MarqueeState.isSelecting = false
      requestRedraw()
    else
      DragState.dragging = false
      setCursor(_hitTest(e.getX, e.getY) match
        case HoverTarget.Segment(_) => Cursor.V_RESIZE
        case HoverTarget.Point(_)   => Cursor.DEFAULT
        case _                      => Cursor.DEFAULT)
  )

  setOnMouseExited((e: MouseEvent) =>
    if !DragState.dragging && !MarqueeState.isSelecting then
      _hoverTarget = HoverTarget.None
      setCursor(Cursor.DEFAULT)
      requestRedraw()
  )

object JagEnvelopeEditorCanvas:
  def apply(): JagEnvelopeEditorCanvas = new JagEnvelopeEditorCanvas()
