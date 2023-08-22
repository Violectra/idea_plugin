package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.services.MyProjectService
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.github.violectra.ideaplugin.utils.TreeNodeUtils
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.RowsDnDSupport.RefinedDropSupport.Position
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

class MyToolWindow(private val project: Project) : JPanel(BorderLayout()) {

    internal val tree: Tree
    val treeModel: DefaultTreeModel

    init {
        treeModel = MyDndTreeModel(null)
        tree = Tree(treeModel)
        tree.cellRenderer = MyCellRenderer { node: DefaultMutableTreeNode ->
            MyNodeUtils.objectToString(node.userObject)
        }

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

        override fun canDrop(oldIndex: Int, newIndex: Int, position: Position): Boolean {
            if (oldIndex == newIndex) return false
            val current = getTreeNode(oldIndex)
            val target = getTreeNode(newIndex)
            return !target.isNodeAncestor(current) && target.allowsChildren
        }

        override fun drop(currentTreeIndex: Int, targetTreeIndex: Int, position: Position) {
            val expandedPaths = TreeUtil.collectExpandedPaths(tree)

            val currentNode = getTreeNode(currentTreeIndex)
            val targetNode = getTreeNode(targetTreeIndex)

            insertTreeNode(currentNode, targetNode, position)
            handleInserting(currentNode, targetNode, position)

            this.reload()
            TreeUtil.restoreExpandedPaths(tree, expandedPaths)
        }

        private fun insertTreeNode(
            current: DefaultMutableTreeNode,
            target: DefaultMutableTreeNode,
            position: Position
        ) {
            if (position == Position.INTO) {
                val index = if (target.isNodeChild(current)) target.childCount - 1 else target.childCount
                insertNodeInto(current, target, index)
            } else {
                val index = TreeNodeUtils.calculateIndexWithPosition(target, current,
                    position == Position.BELOW)
                insertNodeInto(current, target.parent as MutableTreeNode, index)
            }
        }

        private fun handleInserting(
            currentTreeNode: DefaultMutableTreeNode,
            targetTreeNode: DefaultMutableTreeNode,
            position: Position
        ) {
            val current = currentTreeNode.userObject
            val target = targetTreeNode.userObject
            val isInto = position == Position.INTO
            val isAfter = position == Position.BELOW

            val service = project.service<MyProjectService>()
            service.handleTreeNodeInserting(current, target, isInto, isAfter)
        }

        private fun getTreeNode(row: Int) = tree.getPathForRow(row).lastPathComponent as DefaultMutableTreeNode

    }

    inner class MyCellRenderer(val converterFunction: (DefaultMutableTreeNode) -> String?) : NodeRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ) {
            val newValue = if (value is DefaultMutableTreeNode) {
                converterFunction(value) ?: value
            } else value
            super.customizeCellRenderer(tree, newValue, selected, expanded, leaf, row, hasFocus)
        }
    }

}
