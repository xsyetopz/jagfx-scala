package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel._
import jagfx.ui.controller.IController

/** Inspector panel for editing envelope or filter parameters. */
class InspectorController(viewModel: SynthViewModel) extends IController[VBox]:
  protected val view = VBox()
  view.getStyleClass.add("inspector")
  view.setSpacing(8)

  private val envInspector = EnvelopeInspector()
  private val fltInspector = FilterInspector()
  private val timingInspector = TimingInspector()

  private val helpHeader = Label("INFO")
  helpHeader.getStyleClass.add("help-header")

  private val helpDesc = Label("")
  helpDesc.getStyleClass.add("help-text")
  helpDesc.setWrapText(true)

  private val helpControls = Label("")
  helpControls.getStyleClass.add("help-text")
  helpControls.setWrapText(true)

  private val topPane = VBox(8)
  topPane.setAlignment(Pos.TOP_LEFT)

  private val bottomPane = VBox(2)
  bottomPane.setAlignment(Pos.BOTTOM_LEFT)
  VBox.setVgrow(bottomPane, Priority.ALWAYS)

  // add inspectors to top pane, but hide them initially
  topPane.getChildren.addAll(envInspector, fltInspector, timingInspector)
  envInspector.setVisible(false)
  envInspector.setManaged(false)
  fltInspector.setVisible(false)
  fltInspector.setManaged(false)
  timingInspector.setVisible(false)
  timingInspector.setManaged(false)

  bottomPane.getChildren.addAll(helpHeader, helpDesc, helpControls)
  view.getChildren.addAll(topPane, bottomPane)

  hide()

  def bind(envelope: EnvelopeViewModel, title: String, desc: String): Unit =
    show(envInspector)
    envInspector.bind(envelope)
    timingInspector.bind(viewModel.getActiveTone)
    timingInspector.setVisible(true)
    timingInspector.setManaged(true)
    updateHelp(title, desc, envInspector.getHelpText)

  def bindFilter(filter: FilterViewModel, title: String, desc: String): Unit =
    show(fltInspector)
    fltInspector.bind(filter)
    updateHelp(title, desc, fltInspector.getHelpText)

  def hide(): Unit =
    envInspector.setVisible(false)
    envInspector.setManaged(false)
    fltInspector.setVisible(false)
    fltInspector.setManaged(false)
    timingInspector.setVisible(false)
    timingInspector.setManaged(false)
    helpHeader.setVisible(false)
    helpDesc.setText("")
    helpControls.setText("")

  private def show(node: javafx.scene.Node): Unit =
    hide()
    node.setVisible(true)
    node.setManaged(true)
    helpHeader.setVisible(true)

  private def updateHelp(title: String, desc: String, controls: String): Unit =
    helpDesc.setText(desc)
    helpControls.setText(s"\n$controls")
