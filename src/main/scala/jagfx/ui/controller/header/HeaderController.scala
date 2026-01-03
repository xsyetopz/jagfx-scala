package jagfx.ui.controller.header

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry._
import javafx.scene.text._
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.controller.IController
import jagfx.ui.components._
import jagfx.Constants
import jagfx.utils._

/** Header controller containing transport, file, and settings controls. */
class HeaderController(viewModel: SynthViewModel) extends IController[GridPane]:
  import Constants._

  private val audioPlayer = AudioPlayer(viewModel)
  private val fileOps = FileOperations(viewModel, () => view.getScene.getWindow)

  /** Callback for playhead position updates. */
  def onPlayheadUpdate: Double => Unit = audioPlayer.onPlayheadUpdate
  def onPlayheadUpdate_=(f: Double => Unit): Unit =
    audioPlayer.onPlayheadUpdate = f

  protected val view = GridPane()
  view.getStyleClass.add("header")

  private val col1 = ColumnConstraints()
  col1.setPercentWidth(25)
  col1.setHalignment(HPos.LEFT)

  private val col2 = ColumnConstraints()
  col2.setPercentWidth(50)
  col2.setHalignment(HPos.CENTER)

  private val col3 = ColumnConstraints()
  col3.setPercentWidth(25)
  col3.setHalignment(HPos.CENTER)

  view.getColumnConstraints.addAll(col1, col2, col3)

  private val transportGroup = createTransportGroup()
  private val tgtGroup = createTargetGroup()
  private val lenPosGroup = createLenPosGroup()
  private val loopGroup = createLoopGroup()
  private val fileGroup = createFileGroup()
  private val btn16 = create16BitButton()
  private val btnInit = createInitButton()

  private val leftGroup = HBox(2)
  leftGroup.setAlignment(Pos.CENTER_LEFT)
  leftGroup.setPickOnBounds(false)

  private val title = new TextFlow()
  title.setPadding(new Insets(0, 6, 0, 0))

  val txtJag = new Text("JAG")
  txtJag.setStyle(
    "-fx-fill: #f0f0f0; -fx-font-weight: 900; -fx-font-size: 14px;"
  )
  val txtFx = new Text("FX")
  txtFx.setStyle(
    "-fx-fill: #33bbee; -fx-font-weight: 900; -fx-font-size: 14px;"
  )

  title.getChildren.addAll(txtJag, txtFx)

  val separator = new Separator(Orientation.VERTICAL)
  separator.setPadding(new Insets(0, 4, 0, 4))

  leftGroup.getChildren.addAll(title, separator, transportGroup)

  private val centerGroup = HBox(2)
  centerGroup.setAlignment(Pos.CENTER)
  centerGroup.setPickOnBounds(false)
  centerGroup.getChildren.addAll(tgtGroup, lenPosGroup, loopGroup)

  private val rightGroup = HBox(2)
  rightGroup.setAlignment(Pos.CENTER_RIGHT)
  rightGroup.setPickOnBounds(false)
  rightGroup.setMaxWidth(Double.MaxValue)

  val rightSpacer = new Region()
  HBox.setHgrow(rightSpacer, Priority.ALWAYS)

  rightGroup.getChildren.addAll(btn16, btnInit, rightSpacer, fileGroup)

  view.add(leftGroup, 0, 0)
  view.add(centerGroup, 1, 0)
  view.add(rightGroup, 2, 0)

  private def createTransportGroup(): HBox =
    val btnPlay = JagButton()
    btnPlay.setGraphic(IconUtils.icon("mdi2p-play"))
    val btnStop = JagButton()
    btnStop.setGraphic(IconUtils.icon("mdi2s-stop"))
    val btnLoop = JagButton()
    btnLoop.setGraphic(IconUtils.icon("mdi2r-repeat"))
    btnLoop.setId("btn-loop")

    btnPlay.setOnAction(_ => audioPlayer.play())
    btnStop.setOnAction(_ => audioPlayer.stop())
    btnLoop.setOnAction(_ =>
      viewModel.loopEnabledProperty.set(!viewModel.isLoopEnabled)
    )

    viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
      btnLoop.setActive(enabled)
    )

    val group = HBox(2, btnPlay, btnStop, btnLoop)
    group.getStyleClass.add("h-grp")
    group

  private def createTargetGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")

    val btnTone = JagButton("TONE")
    val btnAll = JagButton("ALL")
    btnTone.setOnAction(_ => viewModel.targetModeProperty.set(false))
    btnAll.setOnAction(_ => viewModel.targetModeProperty.set(true))

    viewModel.targetModeProperty.addListener((_, _, isAll) =>
      btnTone.setActive(!isAll)
      btnAll.setActive(isAll)
    )
    btnTone.setActive(true)

    group.getChildren.addAll(Label("TGT"), btnTone, btnAll)
    group

  private def createLenPosGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")
    val lenField = JagNumericField(0, Int16.Range, 1200)
    lenField.valueProperty.bindBidirectional(viewModel.totalDurationProperty)
    val posField = JagNumericField(0, Int16.Range, 0)
    group.getChildren.addAll(Label("LEN"), lenField, Label("POS"), posField)
    group

  private def createLoopGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")

    val l1 = JagNumericField(0, Int16.Range, 0)
    l1.setPrefWidth(34)
    l1.valueProperty.bindBidirectional(viewModel.loopStartProperty)

    val l2 = JagNumericField(0, Int16.Range, 0)
    l2.setPrefWidth(34)
    l2.valueProperty.bindBidirectional(viewModel.loopEndProperty)

    val cnt = JagNumericField(0, 100, 0)
    cnt.setPrefWidth(24)
    cnt.valueProperty.bindBidirectional(viewModel.loopCountProperty)

    group.getChildren.addAll(
      Label("L1"),
      l1,
      Label("L2"),
      l2,
      Label("CNT"),
      cnt
    )

    viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
      group.setDisable(!enabled)
    )
    group.setDisable(true)
    group

  private def createFileGroup(): HBox =
    val group = HBox(2)
    group.setStyle("-fx-border-color: transparent;")
    group.setAlignment(Pos.CENTER)

    val btnOpen = JagButton()
    btnOpen.setGraphic(IconUtils.icon("mdi2f-folder-open"))
    val btnSave = JagButton()
    btnSave.setGraphic(IconUtils.icon("mdi2c-content-save"))
    val btnExport = JagButton()
    btnExport.setGraphic(IconUtils.icon("mdi2e-export-variant"))

    btnOpen.setOnAction(_ => fileOps.open())
    btnSave.setOnAction(_ => fileOps.save())
    btnExport.setOnAction(_ => fileOps.saveAs())

    group.getChildren.addAll(btnOpen, btnSave, btnExport)
    group

  private def createInitButton(): JagButton =
    val btn = JagButton()
    btn.setGraphic(IconUtils.icon("mdi2f-file-plus"))
    btn.setOnAction(_ => viewModel.reset())
    btn

  private def create16BitButton(): JagButton =
    val btn16 = JagButton("16-BIT")
    btn16.setOnAction(_ =>
      UserPreferences.export16Bit.set(!UserPreferences.export16Bit.get)
    )
    UserPreferences.export16Bit.addListener((_, _, enabled) =>
      btn16.setActive(enabled)
    )
    btn16.setActive(UserPreferences.export16Bit.get)
    btn16
