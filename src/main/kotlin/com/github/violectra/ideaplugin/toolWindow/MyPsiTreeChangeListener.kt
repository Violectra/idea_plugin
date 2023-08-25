package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getTreePath
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import javax.swing.tree.DefaultMutableTreeNode

class MyPsiTreeChangeListener(private val project: Project, private val window: MyToolWindow) : PsiTreeChangeAdapter() {

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
        if (event.newChild is PsiWhiteSpace && event.newChild is PsiWhiteSpace) return
        val eventParent = event.parent
        if (eventParent is XmlDocument) {
            val oldChild = event.oldChild
            if (oldChild is XmlTag && event.newChild is XmlTag) {
                val oldChildDom = DomManager.getDomManager(project).getDomElement(oldChild)
                if (oldChildDom is Root) {
                    window.treeModel.setRoot(null)
                }
            }
        } else {
            val nodeToReload = getAffectedNode(event.parent) ?: return
            val affected: DefaultMutableTreeNode =
                project.service<MyProjectService>().getTreeNode(nodeToReload) ?: return
            if (!affected.isRoot) {
                window.treeModel.removeNodeFromParent(affected)
            } else {
                window.treeModel.setRoot(null)
            }
        }
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        if (event.newChild == null || event.newChild is PsiWhiteSpace) return
        reloadAffectedSubTree(event.parent)
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        reloadAffectedSubTree(event.parent)
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        val newChildElement = event.child
        val parentElement = event.parent
        if (parentElement is XmlDocument && newChildElement is XmlTag) {
            window.treeModel.setRoot(null)
        } else if (newChildElement is XmlTag) {
            val nodeToReload = getAffectedNode(event.child) ?: return
            val affected: DefaultMutableTreeNode =
                project.service<MyProjectService>().getTreeNode(nodeToReload) ?: return
            if (!affected.isRoot) {
                window.treeModel.removeNodeFromParent(affected)
            } else {
                window.treeModel.setRoot(null)
            }
        }
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        if (event.child is XmlTag) {
            reloadAffectedSubTree(event.parent)
        }
    }

    private fun reloadAffectedSubTree(element: PsiElement) {

        if (element is XmlDocument) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(element.containingFile, true)
            return
        }
        val rootContainingFile = project.service<MyProjectService>().rootFile

        if (rootContainingFile != element.containingFile) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(rootContainingFile, true)
            return
        }

        val affectedNode = getAffectedNode(element) ?: return
        if (affectedNode is Root) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(rootContainingFile, true)
            return
        }
        val affectedParentTreeNode: DefaultMutableTreeNode =
            project.service<MyProjectService>().getTreeNode(affectedNode.parent as MyNode)
                ?: throw RuntimeException("Node is broken")
        val indexOf =
            (affectedNode.parent as MyNodeWithChildren).getSubNodes().indexOf(affectedNode as MyNodeWithIdAttribute)
        val affectedTreeNode: DefaultMutableTreeNode? =
            project.service<MyProjectService>().getTreeNode(affectedNode as MyNode)
        affectedTreeNode?.let { if (affectedTreeNode.parent != null) window.treeModel.removeNodeFromParent(it) }
        val newChild = project.service<MyProjectService>().convertToNodes(affectedNode)
        window.treeModel.insertNodeInto(newChild, affectedParentTreeNode, indexOf)
        window.treeModel.getTreePath(newChild).let { window.tree.expandPath(it) }
    }

    private fun getAffectedNode(element: PsiElement?): MyNode? {
        var cur: PsiElement? = element ?: return null
        while (cur != null && cur !is XmlTag) {
            cur = cur.parent
        }
        if (cur == null) return null
        val domManager = DomManager.getDomManager(project)
        val curElement = domManager.getDomElement(cur as XmlTag)
        return if (curElement is MyNode) curElement else null
    }
}