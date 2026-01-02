package jagfx.ui.viewmodel

import javafx.beans.property._
import javafx.collections.{FXCollections, ObservableList}
import jagfx.model._

enum RackMode:
  case Main, Filter, Both

/** Root `ViewModel` encapsulating entire `.synth` file state. */
class SynthViewModel:
  private val activeToneIndex = new SimpleIntegerProperty(0)
  private val tones = FXCollections.observableArrayList[ToneViewModel]()
  private val loopStart = new SimpleIntegerProperty(0)
  private val loopEnd = new SimpleIntegerProperty(0)
  private val loopCount = new SimpleIntegerProperty(0)
  private val loopEnabled = new SimpleBooleanProperty(false)

  val rackMode = new SimpleObjectProperty[RackMode](RackMode.Main)
  val selectedCellIndex = new SimpleIntegerProperty(-1)

  // TGT: false = TONE, true = ALL
  private val targetMode = new SimpleBooleanProperty(false)

  for _ <- 0 until 10 do tones.add(new ToneViewModel())

  def activeToneIndexProperty: IntegerProperty = activeToneIndex
  def getActiveToneIndex: Int = activeToneIndex.get
  def setActiveToneIndex(idx: Int): Unit = activeToneIndex.set(idx)

  def getTones: ObservableList[ToneViewModel] = tones
  def getActiveTone: ToneViewModel = tones.get(activeToneIndex.get)

  def loopStartProperty: IntegerProperty = loopStart
  def loopEndProperty: IntegerProperty = loopEnd
  def loopCountProperty: IntegerProperty = loopCount

  def loopEnabledProperty: BooleanProperty = loopEnabled
  def isLoopEnabled: Boolean = loopEnabled.get

  def targetModeProperty: BooleanProperty = targetMode
  def isTargetAll: Boolean = targetMode.get

  def load(file: SynthFile): Unit =
    loopStart.set(file.loop.begin)
    loopEnd.set(file.loop.end)
    for i <- 0 until 10 do
      val tone = file.tones.lift(i).flatten
      tones.get(i).load(tone)

  def toModel(): SynthFile =
    val toneModels = tones
      .stream()
      .map(_.toModel())
      .toArray(size => new Array[Option[Tone]](size))
      .toVector
    val loop = LoopParams(loopStart.get, loopEnd.get)
    SynthFile(toneModels, loop)
