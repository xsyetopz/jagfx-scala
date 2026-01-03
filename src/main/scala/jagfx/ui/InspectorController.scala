package jagfx.ui

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel._
import jagfx.ui.components._
import jagfx.model.WaveForm

class InspectorController(viewModel: SynthViewModel):
  private val view = VBox()
  view.getStyleClass.add("inspector")
  view.setSpacing(10)
  view.setPrefWidth(170)

  private var currentEnvelope: Option[EnvelopeViewModel] = None

  private val waveLabel = Label("WAVE")
  waveLabel.getStyleClass.add("label")

  private val waveGrid = JagToggleGroup(
    ("Off", "mdi2v-volume-off"),
    ("Square", "mdi2s-square-wave"),
    ("Sine", "mdi2s-sine-wave"),
    ("Saw", "mdi2s-sawtooth-wave"),
    ("Noise", "mdi2w-waveform")
  )
  waveGrid.setAlignment(Pos.CENTER)

  waveGrid.selectedProperty.addListener((_, _, newVal) =>
    currentEnvelope.foreach { env =>
      val form = newVal match
        case "Off"    => WaveForm.Off
        case "Square" => WaveForm.Square
        case "Sine"   => WaveForm.Sine
        case "Saw"    => WaveForm.Saw
        case "Noise"  => WaveForm.Noise
        case _        => WaveForm.Off
      env.form.set(form)
    }
  )

  private val rangeLabel = Label("RANGE")
  rangeLabel.getStyleClass.add("label")

  private val startField = JagNumericField(-999999, 999999, 0)
  startField.valueProperty.addListener((_, _, newVal) =>
    currentEnvelope.foreach(_.start.set(newVal.intValue))
  )

  private val endField = JagNumericField(-999999, 999999, 0)
  endField.valueProperty.addListener((_, _, newVal) =>
    currentEnvelope.foreach(_.end.set(newVal.intValue))
  )

  private val sp1 = new Region()
  HBox.setHgrow(sp1, Priority.ALWAYS)
  private val startRow = HBox(4, Label("S"), sp1, startField)

  private val sp2 = new Region()
  HBox.setHgrow(sp2, Priority.ALWAYS)
  private val endRow = HBox(4, Label("E"), sp2, endField)

  view.getChildren.addAll(waveLabel, waveGrid, rangeLabel, startRow, endRow)
  view.setVisible(false)

  def getView: VBox = view

  def bind(envelope: EnvelopeViewModel): Unit =
    currentEnvelope = Some(envelope)
    view.setVisible(true)

    val formStr = envelope.form.get match
      case WaveForm.Square => "Square"
      case WaveForm.Sine   => "Sine"
      case WaveForm.Saw    => "Saw"
      case WaveForm.Noise  => "Noise"
      case _               => "Off"

    waveGrid.setSelected(formStr)
    startField.setValue(envelope.start.get)
    endField.setValue(envelope.end.get)

  def hide(): Unit =
    currentEnvelope = None
    view.setVisible(false)
