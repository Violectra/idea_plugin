package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.github.violectra.ideaplugin.utils.NodeUtils
import com.github.violectra.ideaplugin.utils.XmlUtils
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
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
import javax.swing.tree.MutableTreeNode
import com.intellij.ui.RowsDnDSupport.RefinedDropSupport.Position as DNDPosition

class MyToolWindow(private val project: Project) : JPanel(BorderLayout()), Disposable {

    internal val tree: Tree
    val treeModel: DefaultTreeModel

    init {
        treeModel = MyDndTreeModel(null)
        tree = Tree(treeModel)
        tree.cellRenderer = CustomCellRenderer()

        val treeDecorator = ToolbarDecorator.createDecorator(tree).setForcedDnD()
        val treeScrollPane = ScrollPaneFactory.createScrollPane(tree)
        treeScrollPane.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
        treeScrollPane.preferredSize = Dimension(250, -1)

        add(treeDecorator.createPanel())
    }


    inner class MyDndTreeModel(rootNode: DefaultMutableTreeNode?) : DefaultTreeModel(rootNode), EditableModel,
        RowsDnDSupport.RefinedDropSupport {

        override fun removeRow(idx: Int) {
            // not used, but EditableModel is required for DnD enabling
        }

        override fun addRow() {
            // not used, but EditableModel is required for DnD enabling
        }

        override fun exchangeRows(oldIndex: Int, newIndex: Int) {
            // not used, but EditableModel is required for DnD enabling
        }

        override fun canExchangeRows(oldIndex: Int, newIndex: Int): Boolean {
            // not used, but EditableModel is required for DnD enabling
            return false
        }

        override fun isDropInto(component: JComponent?, oldIndex: Int, newIndex: Int): Boolean {
            return true
        }

        override fun canDrop(
            oldIndex: Int, newIndex: Int, position: DNDPosition
        ): Boolean {
            if (oldIndex == newIndex) return false

            val oldNode = getNode(oldIndex)
            val targetNode = getNode(newIndex)
            return !targetNode.isNodeAncestor(oldNode) && (targetNode.userObject !is NodeRef)
        }

        override fun drop(currentTreeIndex: Int, targetTreeIndex: Int, position: DNDPosition) {
            val expandedPaths = TreeUtil.collectExpandedPaths(tree)

            val curTreeNode = getNode(currentTreeIndex)
            val targetTreeNode = getNode(targetTreeIndex)

            insertTreeNode(curTreeNode, targetTreeNode, position)
            insertXmlElement(curTreeNode, targetTreeNode, position)

            this.reload()
            TreeUtil.restoreExpandedPaths(tree, expandedPaths)
        }

        private fun insertTreeNode(
            curTreeNode: DefaultMutableTreeNode,
            targetTreeNode: DefaultMutableTreeNode,
            position: RowsDnDSupport.RefinedDropSupport.Position
        ) {
            if (position == DNDPosition.INTO) {
                val intoIndex = if (targetTreeNode.childCount == 0) 0 else targetTreeNode.childCount
                insertNodeInto(curTreeNode, targetTreeNode, intoIndex)
            } else {
                val intoIndex = NodeUtils.calculateIndexWithPosition(targetTreeNode, curTreeNode, position)
                insertNodeInto(curTreeNode, targetTreeNode.parent as MutableTreeNode, intoIndex)
            }
        }

        private fun insertXmlElement(
            currentTreeNode: DefaultMutableTreeNode,
            targetTreeNode: DefaultMutableTreeNode,
            position: RowsDnDSupport.RefinedDropSupport.Position
        ) {
            val current = getMovableNode(currentTreeNode)
            val target = targetTreeNode.userObject as MyNode
            WriteCommandAction.runWriteCommandAction(project) {
                if (position == DNDPosition.INTO) {
                    XmlUtils.xmlInsertInto(current.xmlElement, target.xmlElement)
                } else {
                    XmlUtils.xmlInsertIntoPosition(current.xmlElement, target.xmlElement, position)
                }
            }
        }

        private fun getMovableNode(treeNode: DefaultMutableTreeNode): MyNode {
            val node = treeNode.userObject as MyNode
            return if (node is RootWithExternalRef) node.nodeRef else node
        }

        private fun getNode(row: Int) = tree.getPathForRow(row).lastPathComponent as DefaultMutableTreeNode

    }

    inner class CustomCellRenderer : NodeRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ) {
            val newValue = if (value is DefaultMutableTreeNode) {
                val userObject = value.userObject
                if (userObject is MyNode) MyNodeUtils.nodeToString(userObject) else value
            } else value
            super.customizeCellRenderer(tree, newValue, selected, expanded, leaf, row, hasFocus)
        }
    }

    override fun dispose() {
        //todo
    }

}
