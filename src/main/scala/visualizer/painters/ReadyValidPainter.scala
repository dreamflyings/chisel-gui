package visualizer.painters

import java.awt.{Color, Rectangle}

import visualizer.DrawMetrics
import visualizer.models._

import scala.swing.Graphics2D

class ReadyValidPainter(dataModel: DataModel, displayModel: DisplayModel) extends Painter(displayModel) {
  val FireColor = new Color(152, 251, 152)
  val ReadySetColor = new Color(255, 240, 106)
  val ValidSetColor = new Color(255, 208, 98)

  def paintWaveform(g: Graphics2D, visibleRect: Rectangle, top: Int, node: InspectedNode): Unit = {
    require(node.signal.isDefined)
    val signal = node.signal.get
    require(signal.waveform.isDefined)
    require(signal.isInstanceOf[CombinedSignal])

    val combinedSignal = signal.asInstanceOf[CombinedSignal]
    val startTimestamp = displayModel.xCoordinateToTimestamp(visibleRect.x)

    try {
      combinedSignal.waveform.get
        .findTransition(startTimestamp)
        .sliding(2)
        .takeWhile { transitionPair =>
          displayModel.timestampToXCoordinate(transitionPair.head.timestamp) < visibleRect.x + visibleRect.width
        }
        .foreach { transitionPair =>
          // length could be 1 if findTransition(startTimestamp) has length 1
          if (transitionPair.length == 2) {
            val left:  Int = displayModel.timestampToXCoordinate(transitionPair.head.timestamp)
            val right: Int = displayModel.timestampToXCoordinate(transitionPair.last.timestamp)

            assert(transitionPair.head.value.length == 2)
            drawSegment(g, left, right, top, transitionPair.head.value(0) == 1, transitionPair.head.value(1) == 1)
          }
        }
    } catch {
      // If there's only 1 transition in the iterator returned by findTransition,
      // sliding will throw IndexOutOfBoundsException
      case _: IndexOutOfBoundsException =>
    }
  }

  def drawSegment(g: Graphics2D, left: Int, right: Int, top: Int, ready: Boolean, valid: Boolean): Unit = {
    (ready, valid) match {
      case (true, true) =>
        g.setColor(FireColor)
        g.fillPolygon(Painter.hexagon(left, right, top))
      case (true, false) =>
        g.setColor(ReadySetColor)
        g.fillPolygon(Painter.hexagon(left, right, top))
      case (false, true) =>
        g.setColor(ValidSetColor)
        g.fillPolygon(Painter.hexagon(left, right, top))
      case (false, false) =>
        g.setColor(Color.gray)
        g.drawLine(left, top + DrawMetrics.WaveformHeight / 2, right, top + DrawMetrics.WaveformHeight / 2)
    }
  }
}
