package jagfx.utils

import jagfx.Constants

object AudioUtils:
  import Constants._

  def msToSamples(ms: Int): Int =
    (ms * SampleRate / 1000.0).toInt

  def msToSamples(ms: Double): Int =
    (ms * SampleRate / 1000.0).toInt
