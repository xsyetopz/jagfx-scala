package jagfx.ui.controller.footer

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components._
import jagfx.ui.BindingManager

/** Reverb controls panel (`MIX`/`DAMP`). */
object ReverbPanel:
  def create(viewModel: SynthViewModel, bindings: BindingManager): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(120)
    panel.setPrefWidth(120)
    panel.setMaxWidth(120)
    HBox.setHgrow(panel, Priority.NEVER)
    val head = Label("REVERB")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)
    head.setMaxWidth(Double.MaxValue)

    val mix = JagBarSlider(0, 100, 0, "MIX")
    val damp = JagBarSlider(0, 100, 0, "DAMP")

    viewModel.activeToneIndexProperty.addListener((_, _, _) =>
      bindings.unbindAll()
      val tone = viewModel.getActiveTone
      bindings.bindBidirectional(mix.valueProperty, tone.reverbVolume)
      bindings.bindBidirectional(damp.valueProperty, tone.reverbDelay)
    )

    panel.getChildren.addAll(head, mix, damp)
    panel
