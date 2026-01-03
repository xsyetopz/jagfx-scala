package jagfx.ui.controller.footer

import javafx.scene.layout._
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.controller.IController
import jagfx.ui.BindingManager

/** Footer controller containing tones, harmonics, reverb, and mode panels. */
class FooterController(viewModel: SynthViewModel) extends IController[HBox]:
  protected val view = HBox()
  view.getStyleClass.add("footer")
  private val reverbBindings = BindingManager()

  private val tonesPanel = TonesPanel.create(viewModel)
  private val harmonicsPanel = HarmonicsPanel.create(viewModel)
  private val reverbPanel = ReverbPanel.create(viewModel, reverbBindings)
  private val modePanel = ModePanel.create(viewModel)

  view.getChildren.addAll(tonesPanel, harmonicsPanel, reverbPanel, modePanel)
