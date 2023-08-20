package com.github.violectra.ideaplugin.toolWindow

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
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MyToolWindow(private val myProject: Project) : JPanel(BorderLayout()), Disposable {
    val myComponentTree: Tree
    val treeModel: DefaultTreeModel

    init {
        val rootNode = DefaultMutableTreeNode("Hi")
        treeModel = MyDnDTreeModel(rootNode)


        myComponentTree = Tree(treeModel)
        val decorator = ToolbarDecorator.createDecorator(myComponentTree).setForcedDnD()
        val treeScrollPane = ScrollPaneFactory.createScrollPane(myComponentTree)
        treeScrollPane.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
        treeScrollPane.preferredSize = Dimension(250, -1)
        add(decorator.createPanel())
//        myComponentTree.cellRenderer = DefaultTreeCellRenderer()

    }

    inner class MyDnDTreeModel(private val rootNode: DefaultMutableTreeNode) : DefaultTreeModel(rootNode), EditableModel, RowsDnDSupport.RefinedDropSupport {

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

        private fun getNode(row: Int) = myComponentTree.getPathForRow(row).lastPathComponent as DefaultMutableTreeNode
    }


    override fun dispose() {
        //todo
    }

}