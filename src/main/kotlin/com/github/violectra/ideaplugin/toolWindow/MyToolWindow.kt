package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.*
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.EditableModel
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MyToolWindow(private val myProject: Project) : JPanel(BorderLayout()), Disposable {
    val myComponentTree: Tree
    val treeModel: DefaultTreeModel

    init {
        val rootNode = DefaultMutableTreeNode("Hi")
        treeModel = MyDnDTreeModel(rootNode)


        myComponentTree = Tree(treeModel)
        myComponentTree.cellRenderer = object : NodeRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                if (value is DefaultMutableTreeNode) {
                    val userObject = value.userObject
                    if (userObject is MyNode) {

                        val newValue = nodeString(userObject)
                        super.customizeCellRenderer(tree, newValue, selected, expanded, leaf, row, hasFocus)
                        return
                    }

                    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
                }
            }
        }

        val decorator = ToolbarDecorator.createDecorator(myComponentTree).setForcedDnD()
        val treeScrollPane = ScrollPaneFactory.createScrollPane(myComponentTree)
        treeScrollPane.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
        treeScrollPane.preferredSize = Dimension(250, -1)
        add(decorator.createPanel())
//        myComponentTree.cellRenderer = DefaultTreeCellRenderer()

    }

    private fun nodeString(
        root: MyNode
    ): String {
        val tag = when (root) {
            is NodeA -> "A"
            is NodeB -> "B"
            is NodeRef -> "Ref"
            else -> "root"
        }

        return when (root) {
            is Root -> tag
            is NodeRef -> {
                val srcFileName = root.getSrc().value
                val id = root.getId().value
                "$tag[$id, $srcFileName] ${getTitle(root)}"
            }

            is MyNodeWithIdAttribute -> {
                val id = root.getId().value
                "$tag[$id] ${getTitle(root)}"
            }

            else -> ""
        }
    }

    private fun getTitle(root: MyNodeWithIdAttribute) = root.getTitle().value ?: root.getValue()


    inner class MyDnDTreeModel(private val rootNode: DefaultMutableTreeNode) : DefaultTreeModel(rootNode),
        EditableModel, RowsDnDSupport.RefinedDropSupport {

        override fun removeRow(idx: Int) {
        }

        override fun addRow() {
        }

        override fun exchangeRows(oldIndex: Int, newIndex: Int) {
        }

        override fun canExchangeRows(oldIndex: Int, newIndex: Int): Boolean {
            return false
        }

        override fun isDropInto(component: JComponent?, oldIndex: Int, newIndex: Int): Boolean {
            return true
        }

        override fun canDrop(
            oldIndex: Int,
            newIndex: Int,
            position: RowsDnDSupport.RefinedDropSupport.Position
        ): Boolean {
            return true
        }

        override fun drop(oldIndex: Int, newIndex: Int, position: RowsDnDSupport.RefinedDropSupport.Position) {
            val expandedPaths = TreeUtil.collectExpandedPaths(myComponentTree)

            val oldNode = getNode(oldIndex)
            val newNode = getNode(newIndex)
            if (oldNode.parent == newNode) {
                insertNodeInto(oldNode, newNode, newNode.childCount - 1)
            } else {
                insertNodeInto(oldNode, newNode, if (newNode.childCount == 0) 0 else newNode.childCount)
            }
            this.reload()
            TreeUtil.restoreExpandedPaths(myComponentTree, expandedPaths)
        }

        private fun getNode(row: Int) =
            myComponentTree.getPathForRow(row).lastPathComponent as DefaultMutableTreeNode
    }


    override fun dispose() {
        //todo
    }

}