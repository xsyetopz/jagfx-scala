package jagfx.ui.controller.footer

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import javafx.beans.property.IntegerProperty
import javafx.beans.value.ChangeListener
import javafx.beans.binding.Bindings
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components._

/** Harmonics panel with H1-5/H6-10 bank switcher. */
object HarmonicsPanel:
  def create(viewModel: SynthViewModel): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    HBox.setHgrow(panel, Priority.ALWAYS)

    // 0 = H1-5, 5 = H6-10
    var bankOffset = 0

    val headRow = HBox(4)
    headRow.setAlignment(Pos.CENTER)

    val head = Label("HARMONICS")
    head.getStyleClass.add("panel-head")
    HBox.setHgrow(head, Priority.ALWAYS)

    val bankBtn = JagButton("1-5")
    bankBtn.setPrefWidth(36)

    headRow.getChildren.addAll(head, bankBtn)

    val grid = HBox(4)
    grid.setId("harmonics")
    VBox.setVgrow(grid, Priority.ALWAYS)

    // store strip components for rebinding
    case class HStrip(
        strip: VBox,
        label: Label,
        sRow: HarmonicsRow,
        vRow: HarmonicsRow,
        dRow: HarmonicsRow
    )
    val strips = new Array[HStrip](5)

    for i <- 0 until 5 do
      val strip = VBox()
      strip.getStyleClass.add("h-strip")
      HBox.setHgrow(strip, Priority.ALWAYS)

      val label = Label(s"H${i + 1}")
      label.getStyleClass.add("h-head")

      val sRow = HarmonicsRow("SEMI", -120, 120, 9.0, "%.1f")
      val vRow = HarmonicsRow("VOL", 0, 100)
      val dRow = HarmonicsRow("DEL", 0, 1000)

      strip.getChildren.addAll(label, sRow.view, vRow.view, dRow.view)
      grid.getChildren.add(strip)
      strips(i) = HStrip(strip, label, sRow, vRow, dRow)

    var volListeners =
      Array.fill[Option[(IntegerProperty, ChangeListener[Number])]](5)(None)

    def bindHarmonics(): Unit =
      for i <- 0 until 5 do
        val hIdx = bankOffset + i
        val hs = strips(i)
        hs.label.setText(s"H${hIdx + 1}")

        val h = viewModel.getActiveTone.harmonics(hIdx)

        // remove old listener
        volListeners(i).foreach { case (prop, listener) =>
          prop.removeListener(listener)
        }

        hs.sRow.bind(h.semitone)
        hs.vRow.bind(h.volume)
        hs.dRow.bind(h.delay)

        val volListener: ChangeListener[Number] = (_, _, newVal) =>
          val dim = newVal.intValue == 0
          hs.strip.setOpacity(if dim then 0.5 else 1.0)
          hs.sRow.view.setDisable(dim)
          hs.dRow.view.setDisable(dim)

        h.volume.addListener(volListener)
        volListeners(i) = Some((h.volume, volListener))
        volListener.changed(null, null, h.volume.get)

    bankBtn.setOnAction(_ =>
      bankOffset = if bankOffset == 0 then 5 else 0
      bankBtn.setText(if bankOffset == 0 then "1-5" else "6-10")
      bindHarmonics()
    )

    viewModel.activeToneIndexProperty.addListener((_, _, _) => bindHarmonics())
    bindHarmonics()

    panel.getChildren.addAll(headRow, grid)
    panel

/** Single row in harmonics strip (`SEMI`/`VOL`/`DEL`). */
class HarmonicsRow(
    labelTxt: String,
    min: Int,
    max: Int,
    scale: Double = 1.0,
    format: String = "%.0f"
):
  val view = VBox()
  view.getStyleClass.add("h-row")

  val topRow = HBox()
  topRow.getStyleClass.add("h-sub-row")

  val label = Label(labelTxt)
  label.getStyleClass.add("h-lbl")

  val input = JagNumericField(min, max, 0, scale, format)
  input.setPrefWidth(34)

  val barBox = VBox()
  barBox.getStyleClass.add("bar-box")
  val barFill = Region()
  barFill.getStyleClass.add("bar-fill")
  barBox.getChildren.add(barFill)

  val spacer = new Region()
  HBox.setHgrow(spacer, Priority.ALWAYS)

  topRow.getChildren.addAll(label, spacer, input)
  view.getChildren.addAll(topRow, barBox)

  private var currentProp: Option[IntegerProperty] = None

  def bind(prop: IntegerProperty): Unit =
    currentProp.foreach(input.valueProperty.unbindBidirectional)
    input.valueProperty.bindBidirectional(prop)
    currentProp = Some(prop)

    barFill.prefWidthProperty.bind(
      barBox.widthProperty.multiply(
        Bindings.createDoubleBinding(
          () => (prop.get - min).toDouble / (max - min),
          prop
        )
      )
    )
