package jagfx.synth

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Arrays

private val MaxPoolSize = 20
private val MaxBufferSize = 1048576 // 1024 * 1024

/** Thread-safe pool for reusable integer arrays. */
object BufferPool:
  private val pool = new ConcurrentLinkedQueue[(Int, Array[Int])]()

  /** Acquires buffer of at least `minSize` elements, zero-filled. */
  def acquire(minSize: Int): Array[Int] =
    if minSize > MaxBufferSize then return new Array[Int](minSize)

    var best: Array[Int] = null
    var bestSize = Int.MaxValue

    val iter = pool.iterator()
    while iter.hasNext do
      val (size, buf) = iter.next()
      if size >= minSize && size < bestSize then
        if pool.remove((size, buf)) then
          best = buf
          bestSize = size

    val result = if best != null then best else new Array[Int](minSize)
    Arrays.fill(result, 0, minSize, 0)
    result

  /** Returns buffer to pool for reuse. */
  def release(buffer: Array[Int]): Unit =
    if buffer.length <= MaxBufferSize && pool.size() < MaxPoolSize then
      pool.offer((buffer.length, buffer))

  /** Clears all pooled buffers. */
  def clear(): Unit = pool.clear()
