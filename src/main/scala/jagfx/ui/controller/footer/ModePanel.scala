package jagfx.ui.controller.footer

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.{SynthViewModel, RackMode}
import jagfx.ui.components._

/** Mode selection panel (`MAIN`/`FILTER`/`BOTH`). */
object ModePanel:
  def create(viewModel: SynthViewModel): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(70)
    panel.setPrefWidth(70)
    panel.setMaxWidth(70)
    HBox.setHgrow(panel, Priority.NEVER)

    val head = Label("MODE")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val btnMain = JagButton("MAIN")
    val btnFilter = JagButton("FILTER")
    val btnBoth = JagButton("BOTH")

    btnMain.setMaxWidth(Double.MaxValue)
    btnFilter.setMaxWidth(Double.MaxValue)
    btnBoth.setMaxWidth(Double.MaxValue)

    btnMain.setOnAction(_ => viewModel.rackMode.set(RackMode.Main))
    btnFilter.setOnAction(_ => viewModel.rackMode.set(RackMode.Filter))
    btnBoth.setOnAction(_ => viewModel.rackMode.set(RackMode.Both))

    viewModel.rackMode.addListener((_, _, mode) =>
      btnMain.setActive(mode == RackMode.Main)
      btnFilter.setActive(mode == RackMode.Filter)
      btnBoth.setActive(mode == RackMode.Both)
    )
    btnMain.setActive(true)

    val container = VBox(2)
    container.setId("mode-container")
    VBox.setVgrow(container, Priority.ALWAYS)
    container.getChildren.addAll(btnMain, btnFilter, btnBoth)

    panel.getChildren.addAll(head, container)
    panel
