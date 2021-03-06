package visualizer.components

import javafx.scene.control.ToggleButton
import javax.swing.{BorderFactory, Icon, ImageIcon}
import javax.swing.tree.TreePath
import scalaswingcontrib.event.TreeNodesInserted
import scalaswingcontrib.tree.Tree
import visualizer.models._

import scala.swing.BorderPanel.Position.North
import scala.swing._
import scala.swing.event.{ButtonClicked, MouseClicked}

/**
  * Offers all signals in the design to be selected for viewing in
  * wave form viewer
  * @param dataModel    underlying data model
  * @param displayModel underlying displayModel
  */
class SignalSelector(
  dataModel:    DataModel,
  displayModel: DisplayModel
) extends BoxPanel(Orientation.Vertical) {

  val me: SignalSelector = this

  ///////////////////////////////////////////////////////////////////////////
  // View
  ///////////////////////////////////////////////////////////////////////////
  val tree: Tree[DirectoryNode] = new Tree[DirectoryNode] {
    model = dataModel.directoryTreeModel
    renderer = Tree.Renderer(_.name)
    showsRootHandles = true

    listenTo(mouse.clicks)
    reactions += {
      case m: MouseClicked =>
        if (m.clicks == 1) {
          println(s"Got mouse click in tree ${m.clicks}")
        } else if (m.clicks == 2) {
          println(s"mouse double clicked in tree ${m.clicks}")
          selection.cellValues.foreach { node =>
            displayModel.addFromDirectoryToInspected(node.toInspected, this)
          }
        }
    }
  }

  private val toolBar = new ToolBar() {
    peer.setFloatable(false)

    val r = me.getClass.getResource("/images/ShowTemps.png")
    val icon = new ImageIcon(r)
    val r2 = me.getClass.getResource("/images/ShowTemps.png")
    val icon2 = new ImageIcon(r)
//    val tb = new ToggleButton
//    tb.
//    tb.icon = icon

    val toggleButton1 = new Button("Hide _T") {
      if (text.startsWith("Hide")) { text = "Show _T" } else { text = "Hide _T" }
    }
    val toggleButton2 = new Button("Hide _GEN") {
      if (text.startsWith("Hide")) { text = "Show _GEN" } else { text = "Hide _GEN" }
    }

    contents += toggleButton1
    contents += toggleButton2
  }

  contents += toolBar

  val addSymbolsButton = new Button("Add")
  val symbolList: ScrollPane = new ScrollPane(tree) {
    border = BorderFactory.createEmptyBorder()
  }
  contents += symbolList
  contents += addSymbolsButton

  ///////////////////////////////////////////////////////////////////////////
  // Controller
  ///////////////////////////////////////////////////////////////////////////
  listenTo(addSymbolsButton)
  listenTo(tree)
  listenTo(mouse.clicks)
  reactions += {
    case m: MouseClicked =>
      if (m.clicks == 1) {
        println(s"Got mouse click in DirectoryComponent ${m.clicks}")
      } else if (m.clicks == 2) {
        println(s"mouse double clicked in DirectoryComponent ${m.clicks}")
        tree.selection.cellValues.foreach { node =>
          displayModel.addFromDirectoryToInspected(node.toInspected, this)
        }
      }
    case ButtonClicked(`addSymbolsButton`) =>
      tree.selection.cellValues.foreach { node =>
        displayModel.addFromDirectoryToInspected(node.toInspected, this)
      }
    case e: TreeNodesInserted[_] =>
      if (dataModel.directoryTreeModel.size == e.childIndices.length) {
        tree.peer.expandPath(new TreePath(dataModel.directoryTreeModel.peer.getRoot))
      }
  }
}
