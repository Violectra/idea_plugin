package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.listeners.ReloadTreeListener
import com.github.violectra.ideaplugin.services.MyProjectService
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.RowsDnDSupport.RefinedDropSupport.Position
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.EditableModel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

class MyToolWindow(private val project: Project) : JPanel(BorderLayout()), Disposable {

    internal val tree: Tree
    val treeModel: MyDndTreeModel = MyDndTreeModel(null)

    init {
        tree = Tree(treeModel)
        tree.cellRenderer = MyCellRenderer { node: DefaultMutableTreeNode ->
            MyNodeUtils.objectToString(node.userObject)
        }

        val treeDecorator = ToolbarDecorator.createDecorator(tree).setForcedDnD()
        val treeScrollPane = ScrollPaneFactory.createScrollPane(tree)
        treeScrollPane.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
        treeScrollPane.preferredSize = Dimension(250, -1)

        add(treeDecorator.createPanel())

        val messageBusConnection = project.messageBus.connect(this)

        messageBusConnection.subscribe(
            ReloadTreeListener.RELOAD_MY_TREE_TOPIC,
            object : ReloadTreeListener {
                override fun handleTreeReloading(root: TreeNode?) {
                    treeModel.setRoot(root)
                }

                override fun substituteTreeNode(oldNode: MutableTreeNode, newNode: MutableTreeNode) {
                    val parent = oldNode.parent as MutableTreeNode
                    val index = oldNode.parent.getIndex(oldNode)
                    treeModel.removeNodeFromParent(oldNode)
                    treeModel.insertNodeInto(newNode, parent, index)
                }

                override fun addTreeNode(parent: MutableTreeNode, newNode: MutableTreeNode, index: Int) {
                    treeModel.insertNodeInto(newNode, parent, index)
                }

                override fun reloadTree() {
                    treeModel.reload()
                }
            })
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
            val currentNode = getTreeNode(currentTreeIndex)
            val targetNode = getTreeNode(targetTreeIndex)
            project.service<MyProjectService>().insertPsiElement(currentNode.userObject, targetNode.userObject)
        }

        private fun getTreeNode(row: Int) = tree.getPathForRow(row).lastPathComponent as DefaultMutableTreeNode

    }

    inner class MyCellRenderer(val converterFunction: (DefaultMutableTreeNode) -> String?) : NodeRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ) {
            val newValue = if (value is DefaultMutableTreeNode) {
                try {
                    converterFunction(value) ?: value
                } catch (e: Throwable) {
                    "DELETED"
                }
            } else value
            super.customizeCellRenderer(tree, newValue, selected, expanded, leaf, row, hasFocus)
        }
    }

    override fun dispose() {
    }

}
