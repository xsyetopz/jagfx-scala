package jagfx.ui.components

import javafx.beans.property._
import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.scene.control.{ContextMenu, MenuItem, SeparatorMenuItem}
import javafx.geometry.Pos
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.IconUtils

class JagCellPane(title: String) extends StackPane:
  private val collapsed = SimpleBooleanProperty(false)
  private val muted = SimpleBooleanProperty(false)
  private val selected = SimpleBooleanProperty(false)
  private val solo = SimpleBooleanProperty(false)

  def collapsedProperty: BooleanProperty = collapsed
  def mutedProperty: BooleanProperty = muted
  def selectedProperty: BooleanProperty = selected
  def soloProperty: BooleanProperty = solo

  getStyleClass.add("jag-cell")
  setMinWidth(0)
  setMinHeight(0)

  private val container = VBox()
  container.getStyleClass.add("cell-container")

  private val header = HBox()
  header.getStyleClass.add("cell-head")
  header.setSpacing(4)

  private val titleLabel = Label(title)
  titleLabel.getStyleClass.add("cell-title")
  titleLabel.setMaxWidth(Double.MaxValue)
  titleLabel.setAlignment(Pos.CENTER_LEFT)
  HBox.setHgrow(titleLabel, Priority.ALWAYS)

  private val toolbar = HBox()
  toolbar.setSpacing(1)

  private val btnX1 = createToolButton("X1")
  private val btnX2 = createToolButton("X2")
  private val btnX4 = createToolButton("X4")
  private val btnS = createToolButton("")
  btnS.setGraphic(IconUtils.icon("mdi2s-star"))

  private val btnM = createToolButton("")
  btnM.setGraphic(IconUtils.icon("mdi2v-volume-off"))

  private val btnCollapse = createToolButton("")
  btnCollapse.setGraphic(IconUtils.icon("mdi2c-chevron-up"))

  private val btnMenu = createToolButton("")
  btnMenu.setGraphic(IconUtils.icon("mdi2d-dots-horizontal"))

  private val zooms = Seq(btnX1, btnX2, btnX4)
  zooms.foreach(b =>
    b.setOnAction(_ =>
      zooms.foreach(_.setActive(false))
      b.setActive(true)
    )
  )
  btnX1.setActive(true)

  btnS.setOnAction(_ =>
    val s = !solo.get
    solo.set(s)
    if s then btnS.setActive(true) else btnS.setActive(false)
  )

  btnM.setOnAction(_ =>
    muted.set(!muted.get)
    btnM.setActive(muted.get)
  )

  private val contextMenu = new ContextMenu()

  def updateMenu(): Unit =
    contextMenu.getItems.clear()
    val iX1 = new MenuItem("x1"); iX1.setOnAction(_ => btnX1.fire())
    val iX2 = new MenuItem("x2"); iX2.setOnAction(_ => btnX2.fire())
    val iX4 = new MenuItem("x4"); iX4.setOnAction(_ => btnX4.fire())
    val iS = new MenuItem(if solo.get then "Un-Solo" else "Solo");
    iS.setOnAction(_ => btnS.fire())
    val iM = new MenuItem(if muted.get then "Un-Mute" else "Mute");
    iM.setOnAction(_ => btnM.fire())

    contextMenu.getItems.addAll(iX1, iX2, iX4, new SeparatorMenuItem(), iS, iM)

  btnMenu.setOnAction(e =>
    updateMenu()
    contextMenu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0)
  )

  btnCollapse.setOnAction(_ => collapsed.set(!collapsed.get))

  private var showMute = true
  private var showCollapse = true

  def setFeatures(showMute: Boolean, showCollapse: Boolean): Unit =
    this.showMute = showMute
    this.showCollapse = showCollapse
    updateToolbar()

  private def updateToolbar(): Unit =
    toolbar.getChildren.clear()
    val w = getWidth

    val titleWidth = titleLabel.prefWidth(-1)
    var toolsCount = 4 // X1, X2, X4, S
    if showMute then toolsCount += 1
    if showCollapse then toolsCount += 1

    val toolsWidth = toolsCount * 25
    val padding = 5

    val isNarrow = w > 0 && w < (titleWidth + toolsWidth + padding)
    if isNarrow then
      toolbar.getChildren.add(btnMenu)
      if showCollapse then toolbar.getChildren.add(btnCollapse)
    else
      toolbar.getChildren.addAll(btnX1, btnX2, btnX4, btnS)
      if showMute then toolbar.getChildren.add(btnM)
      if showCollapse then toolbar.getChildren.add(btnCollapse)

  widthProperty.addListener((_, _, _) => updateToolbar())
  updateToolbar()
  header.getChildren.addAll(titleLabel, toolbar)

  private val canvasWrapper = new Pane()
  VBox.setVgrow(canvasWrapper, Priority.ALWAYS)

  private val canvas = JagEnvelopeCanvas()
  canvas.widthProperty.bind(canvasWrapper.widthProperty)
  canvas.heightProperty.bind(canvasWrapper.heightProperty)
  canvasWrapper.getChildren.add(canvas)

  container.getChildren.addAll(header, canvasWrapper)
  getChildren.add(container)

  collapsed.addListener((_, _, isCollapsed) =>
    if isCollapsed then
      getStyleClass.add("collapsed")
      btnCollapse.setGraphic(IconUtils.icon("mdi2c-chevron-down"))
      canvas.setVisible(false)
    else
      getStyleClass.remove("collapsed")
      btnCollapse.setGraphic(IconUtils.icon("mdi2c-chevron-up"))
      canvas.setVisible(true)
  )

  muted.addListener((_, _, isMuted) =>
    if isMuted then getStyleClass.add("muted")
    else getStyleClass.remove("muted")
  )

  selected.addListener((_, _, isSelected) =>
    if isSelected then getStyleClass.add("selected")
    else getStyleClass.remove("selected")
  )

  def setViewModel(vm: EnvelopeViewModel): Unit =
    canvas.setViewModel(vm)

  def getCanvas: JagEnvelopeCanvas = canvas

  private def createToolButton(text: String): JagButton =
    val b = JagButton(text)
    b.getStyleClass.add("t-btn")
    b
