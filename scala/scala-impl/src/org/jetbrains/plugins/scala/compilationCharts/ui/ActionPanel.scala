package org.jetbrains.plugins.scala.compilationCharts.ui

import java.awt._
import java.awt.geom.Rectangle2D

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.border.LineBorder
import javax.swing.{Icon, JComponent}
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.ScalaBundle

import scala.concurrent.duration.DurationInt

class ActionPanel(setZoom: Zoom => Unit)
  extends BorderLayoutPanel {

  import ActionPanel._
  import Common._

  private var currentZoomIndex: Int = DefaultZoomIndex

  private val debugBorder = false
  private val debugBorderThickness = 2
  private def maybeSetDebugBorder(c: JComponent, color: Color): Unit = {
    if (debugBorder) {
      c.setBorder(new LineBorder(color, debugBorderThickness))
    }
  }

  setBorder(JBUI.Borders.empty)
  maybeSetDebugBorder(this, Color.RED)
  addToRight(createActionToolbar())
  withMinimumHeight((2.5 * NormalFont.getSize).toInt)

  private def createActionToolbar(): JComponent = {
    val actionGroup = new DefaultActionGroup
    val actions = Seq(LegendAction, new Separator, ResetZoomAction, ZoomOutAction, ZoomInAction)
    actions.foreach(actionGroup.add)
    val place = classOf[CompilationChartsComponent].getSimpleName
    val toolbar = ActionManager.getInstance.createActionToolbar(place, actionGroup, true)
    val component = toolbar.getComponent
    component
  }

  private object LegendAction
    extends DumbAwareAction
      with CustomComponentAction {

    override def createCustomComponent(presentation: Presentation, place: String): JComponent = {
      val panel = new JBPanel(new FlowLayout(FlowLayout.RIGHT))
      val items = Seq(
        new LegendItem("Production", ProdModuleColor),
        new LegendItem("Test", TestModuleColor),
        new LegendItem("Memory", MemoryLineColor, isLine = true)
      )
      items.foreach(panel.add)
      maybeSetDebugBorder(panel, Color.BLUE)
      panel
    }

    override def actionPerformed(e: AnActionEvent): Unit = ()

    private class LegendItem(name: String,
                             color: Color,
                             isLine: Boolean = false)
      extends JBPanel {

      maybeSetDebugBorder(this, Color.RED)

      private var initialized = false

      override def paintComponent(g: Graphics): Unit = {
        val graphics = g.asInstanceOf[Graphics2D]

        val clipBounds = g.getClipBounds()
        // HACK: I don't know why, but sometimes clipBounds is larger by 1 then component width and this causes flickering
        // Looks like something is wrong with parents layout, but I didn't manage to find out what exactly.
        // So this is probably not the best solution, but looks like it works fine.
        clipBounds.width = clipBounds.width.min(this.getWidth)
        clipBounds.height = clipBounds.height.min(this.getHeight)

        val rendering = graphics.getTextRendering(clipBounds, name, NormalFont, HAlign.Right, VAlign.Center)
        val textRect = rendering.rect.getBounds
        val colorHeight = if (isLine) MemoryLineStroke.thickness else ColorWidth
        val colorY = textRect.y + (textRect.height - colorHeight) / 2.0
        val colorRect = new Rectangle2D.Double(BeforeColorMargin, colorY, ColorWidth, colorHeight)

        graphics.printText(rendering, TextColor)
        graphics.printRect(colorRect, DiagramBackgroundColor)
        graphics.printRect(colorRect, color)

        // we don't have dynamic text, so we can set calculated preferred size just once
        if (!initialized) {
          val width = textRect.width + ColorWidth + BeforeColorMargin + AfterColorMargin
          val height = graphics.getFontMetrics(NormalFont).getHeight
          setPreferredSize(new Dimension(width, height))
          revalidate()
          initialized = true
        }
      }
    }

    private final val ColorWidth = 8

    private final val BeforeColorMargin = 12
    private final val AfterColorMargin = 4
  }

  private abstract class BasicZoomAction(@NotNull @NlsActions.ActionText text: String,
                                         @NotNull @NlsActions.ActionDescription description: String,
                                         @NotNull icon: Icon)
    extends DumbAwareAction(text, description, icon) {

    final override def update(e: AnActionEvent): Unit =
      e.getPresentation.setEnabled(isEnabled)

    final override def actionPerformed(e: AnActionEvent): Unit = {
      currentZoomIndex = newZoomIndex
      val newZoom = AvailableZooms(currentZoomIndex)
      setZoom(newZoom)
    }

    protected def isEnabled: Boolean

    protected def newZoomIndex: Int
  }

  private object ResetZoomAction
    extends BasicZoomAction(
      ScalaBundle.message("compilation.charts.reset.zoom.action.text"),
      ScalaBundle.message("compilation.charts.reset.zoom.action.description"),
      AllIcons.General.ActualZoom) {

    override protected def isEnabled: Boolean = currentZoomIndex != DefaultZoomIndex
    override protected def newZoomIndex: Int = DefaultZoomIndex
  }

  private object ZoomOutAction
    extends BasicZoomAction(
      ScalaBundle.message("compilation.charts.zoom.out.action.text"),
      ScalaBundle.message("compilation.charts.zoom.out.action.description"),
      AllIcons.General.ZoomOut) {

    override protected def isEnabled: Boolean = currentZoomIndex > 0
    override protected def newZoomIndex: Int = currentZoomIndex - 1
  }

  private object ZoomInAction
    extends BasicZoomAction(
      ScalaBundle.message("compilation.charts.zoom.in.action.text"),
      ScalaBundle.message("compilation.charts.zoom.in.action.description"),
      AllIcons.General.ZoomIn) {

    override protected def isEnabled: Boolean = currentZoomIndex < AvailableZooms.size - 1
    override protected def newZoomIndex: Int = currentZoomIndex + 1
  }
}

object ActionPanel {

  private final val AvailableZooms = Seq(
    Zoom(5.minute, 6),
    Zoom(3.minute, 5),
    Zoom(2.minute, 5),
    Zoom(1.minute + 30.seconds, 4),
    Zoom(1.minute, 6),
    Zoom(45.seconds, 4),
    Zoom(30.seconds, 4),
    Zoom(25.seconds, 4),
    Zoom(20.seconds, 3),
    Zoom(15.seconds, 4),
    Zoom(12.seconds, 5),
    Zoom(10.seconds, 6),
    Zoom(8.seconds, 4),
    Zoom(6.seconds, 5),
    Zoom(5.seconds, 6),
    Zoom(4.seconds, 5),
    Zoom(3.seconds, 5),
    Zoom(2.seconds, 5),
    Zoom(1500.millis, 4),
    Zoom(1000.millis, 6),
    Zoom(750.millis, 4),
    Zoom(500.millis, 6),
    Zoom(250.millis, 4),
  ).sortBy(_.durationStep).reverse

  private final val DefaultZoomIndex = AvailableZooms.size / 2

  def defaultZoom: Zoom = AvailableZooms(DefaultZoomIndex)
}
