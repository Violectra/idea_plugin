package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.github.violectra.ideaplugin.utils.TreeNodeUtils
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getTreePath
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.*
import com.intellij.ui.RowsDnDSupport.RefinedDropSupport.Position
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.EditableModel
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.util.xml.DomManager
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

class MyToolWindow(private val project: Project) : JPanel(BorderLayout()), Disposable {

    internal val tree: Tree
    val treeModel: MyDndTreeModel

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

        val messageBusConnection = project.messageBus.connect(this)

        //new file opened
        messageBusConnection.subscribe(ReloadTreeNotifier.RELOAD_MY_TREE_TOPIC,
            object : ReloadTreeNotifier {
                override fun handleTreeReloading(root: DefaultMutableTreeNode?, isSameTree: Boolean) {
                    if (!isSameTree) {
                        treeModel.setRoot(root)
                    }
                }
            })

        PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeChangeAdapter() {

            override fun childrenChanged(event: PsiTreeChangeEvent) {
                val expandedPaths = TreeUtil.collectExpandedPaths(tree)
                treeModel.reload()
                TreeUtil.restoreExpandedPaths(tree, expandedPaths)
            }

            override fun childAdded(event: PsiTreeChangeEvent) {
                val newChildElement = event.child
                val parentElement = event.parent
                if (newChildElement is XmlTag && parentElement is XmlTag) {
                    val domManager = DomManager.getDomManager(project)
                    val child = domManager.getDomElement(newChildElement)
                    val parent = domManager.getDomElement(parentElement)
                    if (child?.exists() == true) {
                        return
                    }
                    if (child is MyNode && parent is MyNodeWithChildren) {
                        val treePath = treeModel.getTreePath(parent) ?: return

                        val indexOfNewElement = parent.getSubNodes().indexOf(child)
                        val parentTreeNode =
                            treePath.lastPathComponent as DefaultMutableTreeNode
                        val newChild = DefaultMutableTreeNode(child, child !is NodeRef)
                        treeModel.insertNodeInto(newChild, parentTreeNode, indexOfNewElement)
                    }
                }
            }

            override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
                val newChildElement = event.child
                if (newChildElement is XmlTag) {
                    val domManager = DomManager.getDomManager(project)
                    val child = domManager.getDomElement(newChildElement)
                    if (child is MyNode) {
                        val treePath = treeModel.getTreePath(child) ?: return
                        val childTreeNode =
                            treePath.lastPathComponent as DefaultMutableTreeNode
                        treeModel.removeNodeFromParent(childTreeNode)
                    }
                }
            }
        }, this);


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

            WriteCommandAction.runWriteCommandAction(project) {
                val movableNode = MyNodeUtils.getMovableNode(currentNode.userObject as MyNode)
                val newPsi: PsiElement? = movableNode.xmlElement?.let { cur ->
                    (targetNode.userObject as MyNode).xmlElement?.add(cur.copy())
                }
                val curCopyDomElement = DomManager.getDomManager(project).getDomElement(newPsi as XmlTag)
                insertNodeInto(DefaultMutableTreeNode(curCopyDomElement), targetNode, targetNode.childCount)
                removeNodeFromParent(currentNode)
                movableNode.xmlElement?.delete()
                treeModel.reload(targetNode)
                tree.expandPath(TreePath(targetNode))
            }
        }

        private fun insertTreeNode(
            current: DefaultMutableTreeNode,
            target: DefaultMutableTreeNode,
            position: Position
        ) {
            if (position == Position.INTO) {
                tree.expandPath(TreePath(target))
                val index = if (target.isNodeChild(current)) target.childCount - 1 else target.childCount
                insertNodeInto(current, target, index)
            } else {
                val index = TreeNodeUtils.calculateIndexWithPosition(
                    target, current,
                    position == Position.BELOW
                )
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

            val publisher = project.messageBus.syncPublisher(ChangeTreeNotifier.CHANGE_MY_TREE_TOPIC)
            publisher.handleTreeNodeInserting(current, target, isInto, isAfter)
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

    override fun dispose() {
    }

}
