package jagfx.utils

import java.util.Arrays

object DrawingUtils:
  /** Draws line into raw ARGB pixel buffer using Bresenham's algorithm. */
  def line(
      buffer: Array[Int],
      w: Int,
      h: Int,
      x0: Int,
      y0: Int,
      x1: Int,
      y1: Int,
      color: Int
  ): Unit =
    var (x, y) = (x0, y0)
    val dx = math.abs(x1 - x0)
    val dy = math.abs(y1 - y0)
    val sx = if x0 < x1 then 1 else -1
    val sy = if y0 < y1 then 1 else -1
    var err = dx - dy

    while x != x1 || y != y1 do
      if x >= 0 && x < w && y >= 0 && y < h then buffer(y * w + x) = color

      val e2 = 2 * err
      if e2 > -dy then
        err -= dy
        x += sx
      if e2 < dx then
        err += dx
        y += sy

    if x >= 0 && x < w && y >= 0 && y < h then buffer(y * w + x) = color

  def fillRect(
      buffer: Array[Int],
      w: Int,
      h: Int,
      rx: Int,
      ry: Int,
      rw: Int,
      rh: Int,
      color: Int
  ): Unit =
    val updateMinX = math.max(0, rx)
    val updateMinY = math.max(0, ry)
    val updateMaxX = math.min(w, rx + rw)
    val updateMaxY = math.min(h, ry + rh)
    if updateMinX < updateMaxX && updateMinY < updateMaxY then
      for y <- updateMinY until updateMaxY do
        var rowOffset = y * w
        for x <- updateMinX until updateMaxX do buffer(rowOffset + x) = color

  def clear(buffer: Array[Int], color: Int): Unit =
    Arrays.fill(buffer, color)
