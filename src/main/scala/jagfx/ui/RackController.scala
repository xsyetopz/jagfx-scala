package jagfx.ui

import javafx.scene.layout._
import jagfx.ui.viewmodel._
import jagfx.ui.components.JagCellPane

class RackController(viewModel: SynthViewModel, inspector: InspectorController):
  private val view = GridPane()
  view.getStyleClass.add("rack")
  view.setHgap(1)
  view.setVgap(1)

  private val cells = new Array[JagCellPane](12)

  private val definitions = Vector(
    ("PITCH", true), // 0
    ("VIB RATE", true), // 1
    ("VIB DEPTH", true), // 2
    ("FILT POLE", false), // 3
    ("VOLUME", true), // 4
    ("TREM RATE", true), // 5
    ("TREM DEPTH", true), // 6
    ("TRANSITION", false), // 7
    (
      "OUTPUT",
      false
    ), // 8 (TODO: waveform visualisation)
    ("SILENCE", true), // 9
    ("DURATION", true), // 10
    ("RESPONSE", false) // 11
  )

  definitions.zipWithIndex.foreach { case ((title, enabled), idx) =>
    val cell = JagCellPane(title)
    if !enabled then cell.setDisable(true)
    else cell.setOnMouseClicked(_ => selectCell(idx))

    if Set(3, 11).contains(idx) then cell.setFeatures(false, false)

    GridPane.setHgrow(cell, Priority.ALWAYS)
    GridPane.setVgrow(cell, Priority.ALWAYS)
    cells(idx) = cell
  }

  viewModel.rackMode.addListener((_, _, _) => buildGrid())
  viewModel.selectedCellIndex.addListener((_, _, _) => updateSelection())
  buildGrid()

  bindSoloLogic()
  bindActiveTone()

  viewModel.activeToneIndexProperty.addListener((_, _, _) => bindActiveTone())

  def getView: GridPane = view

  private def buildGrid(): Unit =
    view.getChildren.clear()
    view.getColumnConstraints.clear()

    val mode = viewModel.rackMode.get
    val indices = mode match
      case RackMode.Main   => Vector(0, 1, 2, 4, 5, 6, 8, 9, 10)
      case RackMode.Filter => Vector(3, 7, 11)
      case RackMode.Both   => (0 to 11).toVector

    // Main/Filter use 3-col grid, Both uses 4-col
    val cols = if mode == RackMode.Both then 4 else 3

    val constraint = new ColumnConstraints()
    constraint.setPercentWidth(100.0 / cols)
    for _ <- 0 until cols do view.getColumnConstraints.add(constraint)

    indices.zipWithIndex.foreach { case (cellIdx, i) =>
      val cell = cells(cellIdx)
      val col = i % cols
      val row = i / cols

      view.add(cell, col, row)
    }
    updateSelection()

  private def updateSelection(): Unit =
    val selectedIdx = viewModel.selectedCellIndex.get
    cells.zipWithIndex.foreach { case (cell, idx) =>
      if cell != null then
        val isSel = idx == selectedIdx
        if cell.selectedProperty.get != isSel then
          cell.selectedProperty.set(isSel)
    }
    if selectedIdx >= 0 && selectedIdx < cells.length then
      bindInspector(selectedIdx)

  private def selectCell(idx: Int): Unit =
    viewModel.selectedCellIndex.set(idx)

  /** Returns envelope for cell index, or `None` for disabled cells. */
  private def envelopeForCell(
      tone: ToneViewModel,
      idx: Int
  ): Option[EnvelopeViewModel] =
    idx match
      case 0  => Some(tone.pitch)
      case 1  => Some(tone.vibratoRate)
      case 2  => Some(tone.vibratoDepth)
      case 4  => Some(tone.volume)
      case 5  => Some(tone.tremoloRate)
      case 6  => Some(tone.tremoloDepth)
      case 9  => Some(tone.gateSilence)
      case 10 => Some(tone.gateDuration)
      case _  => None

  private def bindInspector(idx: Int): Unit =
    envelopeForCell(viewModel.getActiveTone, idx).foreach(inspector.bind)

  private def bindActiveTone(): Unit =
    val tone = viewModel.getActiveTone
    for idx <- cells.indices if cells(idx) != null do
      envelopeForCell(tone, idx).foreach(cells(idx).setViewModel)

  private var preSoloMuteState: Map[JagCellPane, Boolean] = Map.empty

  private def bindSoloLogic(): Unit =
    cells.foreach { cell =>
      if cell != null then
        cell.soloProperty.addListener((_, _, _) => updateSoloState())
    }

  private def updateSoloState(): Unit =
    val activeSolos = cells.filter(c => c != null && c.soloProperty.get)
    if activeSolos.nonEmpty then
      if preSoloMuteState.isEmpty then
        preSoloMuteState = cells.collect {
          case c if c != null => c -> c.mutedProperty.get
        }.toMap

      cells.foreach { c =>
        if c != null then
          if activeSolos.contains(c) then
            if c.mutedProperty.get then c.mutedProperty.set(false)
          else if !c.mutedProperty.get then c.mutedProperty.set(true)
      }
    else if preSoloMuteState.nonEmpty then
      preSoloMuteState.foreach { case (c, wasMuted) =>
        c.mutedProperty.set(wasMuted)
      }
      preSoloMuteState = Map.empty
