package jagfx.ui.components

import javafx.beans.property._
import javafx.scene.layout._
import javafx.scene.shape.Rectangle
import javafx.geometry.Pos
import javafx.scene.control.Label

/** Horizontal bar slider with numeric input. */
class JagBarSlider(min: Int, max: Int, initial: Int, labelText: String = "")
    extends VBox:
  private val value = SimpleIntegerProperty(initial)

  def valueProperty: IntegerProperty = value
  def getValue: Int = value.get
  def setValue(v: Int): Unit = value.set(math.max(min, math.min(max, v)))

  getStyleClass.add("jag-bar-slider")
  setSpacing(2)

  private val inputRow = HBox()
  inputRow.setSpacing(4)
  inputRow.setAlignment(Pos.CENTER_LEFT)

  if labelText.nonEmpty then
    val lbl = new Label(labelText)
    lbl.getStyleClass.add("label")
    lbl.setStyle(
      "-fx-text-fill: #888; -fx-font-size: 9px; -fx-font-weight: bold;"
    )

    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.ALWAYS)
    inputRow.getChildren.addAll(lbl, spacer)

  private val input = JagNumericField(min, max, initial)
  value.bindBidirectional(input.valueProperty)
  inputRow.getChildren.add(input)

  private val barBox = VBox()
  barBox.getStyleClass.add("bar-box")
  barBox.setPrefHeight(4)
  barBox.setMaxHeight(4)

  private val barFill = Region()
  barFill.getStyleClass.add("bar-fill")
  barFill.setPrefHeight(4)
  barFill.setMaxHeight(4)

  barBox.widthProperty.addListener((_, _, newWidth) =>
    val range = max - min
    val ratio = if range > 0 then (value.get - min).toDouble / range else 0
    barFill.setPrefWidth(newWidth.doubleValue * ratio)
  )

  value.addListener((_, _, newVal) =>
    val range = max - min
    val ratio =
      if range > 0 then (newVal.intValue - min).toDouble / range else 0
    barFill.setPrefWidth(barBox.getWidth * ratio)
  )

  getChildren.addAll(inputRow, barBox)
  barBox.getChildren.add(barFill)

object JagBarSlider:
  def apply(
      min: Int,
      max: Int,
      initial: Int,
      label: String = ""
  ): JagBarSlider =
    new JagBarSlider(min, max, initial, label)
