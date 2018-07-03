package visualizer.models



import javax.swing._
import scalaswingcontrib.tree._

import scala.collection.SortedMap
import scala.collection.mutable.ArrayBuffer
import scala.swing._
import scala.swing.event.ActionEvent

class InspectionDisplayModel extends Publisher {
  val inspectedWaves = new ArrayBuffer[Waveform]()

  val temporaryNode = TreeNode("temp", -2)
  val displayTreeModel: InternalTreeModel[TreeNode] = InternalTreeModel(temporaryNode)(_ => Seq.empty[TreeNode])
  val RootPath = Tree.Path.empty[TreeNode]
  val tree = new Tree[TreeNode] {
    model = displayTreeModel
    renderer = Tree.Renderer(_.name) // TODO: use custom renderer to adjust height of row and include value at cursor
    showsRootHandles = true

    // Make it rearrangeable
    peer.setDragEnabled(true)
    peer.setDropMode(DropMode.ON_OR_INSERT)
    peer.setTransferHandler(new TreeTransferHandler)
  }







  var scale: Double = 2
  var minorTickInterval: Long = 0
  val MinMinorTickHSpace = 5

  // initial/constructor
  setScale(10)


  //
  // Signals
  //
  def addSignal(node: TreeNode, source: Component) = {
    displayTreeModel.insertUnder(RootPath, node, displayTreeModel.getChildrenOf(RootPath).size)
    publish(SignalsAdded(source))
  }

  def addModule(moduleNode: TreeNode, source: Component): Unit = {
    displayTreeModel.insertUnder(RootPath, moduleNode, displayTreeModel.getChildrenOf(RootPath).size)

    publish(SignalsAdded(source))
  }

  def setScale(newScale: Double): Unit = {
    scale = newScale
    val x = math.pow(10, math.ceil(math.log10(MinMinorTickHSpace / scale))).toLong
    minorTickInterval = if (x <= 0) 1 else x
  }

  def zoomIn(source: Component): Unit = {
    setScale(scale * 1.25)
    publish(ScaleChanged(source))
  }

  def zoomOut(source: Component): Unit = {
    setScale(scale * 0.8)
    publish(ScaleChanged(source))
  }

  //
  // Cursor
  //
  var cursorPosition: Long = 0
  def setCursorPosition(timestamp: Long) = {
    cursorPosition = timestamp
    publish(CursorSet(null))
  }

  var selectionStart: Long = 0

  //
  // Markers
  //
  var markers = ArrayBuffer[Marker]()
  var nextMarkerId = 1

  def removeAllMarkers: Unit = {
    markers.clear
    publish(MarkerChanged(-1, null))
    nextMarkerId = 1
  }

  def addMarker(description: String, timestamp: Long): Unit = {
    // Adding to markers could be more efficient bc inserting into sorted sequence
    markers  += Marker(nextMarkerId, description, timestamp)
    markers.sortBy(_.timestamp)

    nextMarkerId += 1
    publish(MarkerChanged(timestamp, null))
  }

  // Returns index of element that matches
  def getMarkerAtTime(timestamp: Long): Int = {
    markers.reverse.indexWhere(timestamp >= _.timestamp) match {
      case i if i >= 0 => markers.size - 1 - i
      case _ => 0
    }
  }

  // Returns -1 if no marker was found near this timestamp.
  // Otherwise returns the index of the marker
  def findMarkerNear(timestamp: Long): Int = {
    val MarkerRemoveSlack: Long = (5.0 / scale).toLong

    if (markers.size == 0) {
      -1
    } else {
      val index = getMarkerAtTime(timestamp)
      var targetTimeStamp = markers(index).timestamp
      if (math.abs(timestamp - targetTimeStamp) <= MarkerRemoveSlack) {
        index
      } else if (index < markers.size - 1) {
        targetTimeStamp = markers(index + 1).timestamp
        if (math.abs(timestamp - targetTimeStamp) <= MarkerRemoveSlack) {
          index + 1
        } else {
          -1
        }
      } else {
        -1
      }
    }
  }

  def removeMarkerAtTime(timestamp: Long): Unit = {
    val index = findMarkerNear(timestamp)
    if (index != -1) {
      val actualTimestamp = markers(index).timestamp
      markers.remove(index)
      publish(MarkerChanged(actualTimestamp, null))
    }
  }

  def prevMarker(extendSelection: Boolean): Unit = {
    val index = getMarkerAtTime(cursorPosition)
    var timestamp = markers(index).timestamp
    if (timestamp >= cursorPosition && index > 0) {
      timestamp = markers(index - 1).timestamp
    }

    setCursorPosition(timestamp)
    if (!extendSelection)
      selectionStart = timestamp
  }

  def nextMarker(extendSelection: Boolean): Unit = {
    val index = getMarkerAtTime(cursorPosition)
    if (index < markers.size - 1) {
      var timestamp = markers(index).timestamp
      if (timestamp <= cursorPosition) {
        timestamp = markers(index + 1).timestamp
      }

      setCursorPosition(timestamp)
      if (!extendSelection)
        selectionStart = timestamp
    }
  }
}

case class Marker(id: Int, var description: String, timestamp: Long)

//
// Events
//
case class SignalsAdded(override val source: Component) extends ActionEvent(source)
case class ScaleChanged(override val source: Component) extends ActionEvent(source)
case class CursorSet(override val source: Component) extends ActionEvent(source)
case class MarkerChanged(timestamp: Long, override val source: Component) extends ActionEvent(source)