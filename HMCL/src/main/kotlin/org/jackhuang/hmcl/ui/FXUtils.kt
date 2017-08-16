/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2017  huangyuhui <huanghongxun2008@126.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hmcl.ui

import com.jfoenix.concurrency.JFXUtilities
import com.jfoenix.controls.*
import javafx.animation.Animation
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import org.jackhuang.hmcl.Main

fun Node.loadFXML(absolutePath: String) {
    val fxmlLoader = FXMLLoader(this.javaClass.getResource(absolutePath), Main.RESOURCE_BUNDLE)
    fxmlLoader.setRoot(this)
    fxmlLoader.setController(this)
    fxmlLoader.load<Any>()
}

fun ListView<*>.smoothScrolling() {
    skinProperty().addListener { _ ->
        val bar = lookup(".scroll-bar") as ScrollBar
        val virtualFlow = lookup(".virtual-flow")
        val frictions = doubleArrayOf(0.99, 0.1, 0.05, 0.04, 0.03, 0.02, 0.01, 0.04, 0.01, 0.008, 0.008, 0.008, 0.008, 0.0006, 0.0005, 0.00003, 0.00001)
        val pushes = doubleArrayOf(1.0)
        val derivatives = DoubleArray(frictions.size)

        val timeline = Timeline()
        bar.addEventHandler(MouseEvent.DRAG_DETECTED) { timeline.stop() }

        val scrollEventHandler = EventHandler<ScrollEvent> { event ->
            if (event.eventType == ScrollEvent.SCROLL) {
                val direction = if (event.deltaY > 0) -1 else 1
                for (i in pushes.indices) {
                    derivatives[i] += direction * pushes[i]
                }
                if (timeline.status == Animation.Status.STOPPED) {
                    timeline.play()
                }
                event.consume()
            }
        }

        bar.addEventHandler(ScrollEvent.ANY, scrollEventHandler)
        virtualFlow.onScroll = scrollEventHandler

        timeline.keyFrames.add(KeyFrame(Duration.millis(3.0), EventHandler<ActionEvent> {
            for (i in derivatives.indices) {
                derivatives[i] *= frictions[i]
            }
            for (i in 1..derivatives.size - 1) {
                derivatives[i] += derivatives[i - 1]
            }
            val dy = derivatives[derivatives.size - 1]
            val height = layoutBounds.height
            bar.value = Math.min(Math.max(bar.value + dy / height, 0.0), 1.0)
            if (Math.abs(dy) < 0.001) {
                timeline.stop()
            }
            requestLayout()
        }))
        timeline.cycleCount = Animation.INDEFINITE
    }
}

fun ScrollPane.smoothScrolling() = JFXScrollPane.smoothScrolling(this)

fun runOnUiThread(runnable: () -> Unit) = {
    JFXUtilities.runInFX(runnable)
}

fun takeSnapshot(node: Parent, width: Double, height: Double): WritableImage {
    val scene = Scene(node, width, height)
    scene.stylesheets.addAll(*stylesheets)
    return scene.snapshot(null)
}

fun setOverflowHidden(node: Region) {
    val rectangle = Rectangle()
    rectangle.widthProperty().bind(node.widthProperty())
    rectangle.heightProperty().bind(node.heightProperty())
    node.clip = rectangle
}

val stylesheets = arrayOf(
        Controllers::class.java.getResource("/css/jfoenix-fonts.css").toExternalForm(),
        Controllers::class.java.getResource("/css/jfoenix-design.css").toExternalForm(),
        Controllers::class.java.getResource("/assets/css/jfoenix-main-demo.css").toExternalForm())

fun Region.limitWidth(width: Double) {
    maxWidth = width
    minWidth = width
    prefWidth = width
}

fun Region.limitHeight(height: Double) {
    maxHeight = height
    minHeight = height
    prefHeight = height
}

fun bindInt(textField: JFXTextField, property: Property<*>) {
    textField.textProperty().unbind()
    @Suppress("UNCHECKED_CAST")
    textField.textProperty().bindBidirectional(property as Property<Int>, SafeIntStringConverter())
}

fun bindString(textField: JFXTextField, property: Property<String>) {
    textField.textProperty().unbind()
    textField.textProperty().bindBidirectional(property)
}

fun bindBoolean(toggleButton: JFXToggleButton, property: Property<Boolean>) {
    toggleButton.selectedProperty().unbind()
    toggleButton.selectedProperty().bindBidirectional(property)
}

fun bindBoolean(checkBox: JFXCheckBox, property: Property<Boolean>) {
    checkBox.selectedProperty().unbind()
    checkBox.selectedProperty().bindBidirectional(property)
}

fun bindEnum(comboBox: JFXComboBox<*>, property: Property<out Enum<*>>) {
    unbindEnum(comboBox)
    val listener = ChangeListener<Number> { _, _, newValue ->
        property.value = property.value.javaClass.enumConstants[newValue.toInt()]
    }
    comboBox.selectionModel.select(property.value.ordinal)
    comboBox.properties["listener"] = listener
    comboBox.selectionModel.selectedIndexProperty().addListener(listener)
}

fun unbindEnum(comboBox: JFXComboBox<*>) {
    @Suppress("UNCHECKED_CAST")
    val listener = comboBox.properties["listener"] as? ChangeListener<Number> ?: return
    comboBox.selectionModel.selectedIndexProperty().removeListener(listener)
}


/**
 * Built-in interpolator that provides discrete time interpolation. The
 * return value of `interpolate()` is `endValue` only when the
 * input `fraction` is 1.0, and `startValue` otherwise.
 */
val SINE: Interpolator = object : Interpolator() {
    override fun curve(t: Double): Double {
        return Math.sin(t * Math.PI / 2)
    }

    override fun toString(): String {
        return "Interpolator.DISCRETE"
    }
}