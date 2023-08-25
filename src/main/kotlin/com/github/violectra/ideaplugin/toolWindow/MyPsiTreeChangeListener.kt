package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import javax.swing.tree.DefaultMutableTreeNode

class MyPsiTreeChangeListener(private val project: Project, private val window: MyToolWindow) : PsiTreeChangeAdapter() {

    override fun childReplaced(event: PsiTreeChangeEvent) {
        if (event.newChild == null || event.newChild is PsiWhiteSpace) return
        val eventParent = event.parent
        if (eventParent is XmlDocument) {
            val oldChild1 = event.oldChild
            val newChild1 = event.newChild
            if (oldChild1 is XmlTag && newChild1 is XmlTag) {
                val domManager = DomManager.getDomManager(project)
                val newChild = domManager.getDomElement(newChild1)
                if ((newChild is Root && oldChild1.name != "root") || (oldChild1.name == "root" && newChild1.name != "root")) {
                    project.service<MyProjectService>().reloadTree(eventParent.containingFile, true)
                }
            }
            return
        }
        reloadAffectedSubTree(event.parent)
    }

    private fun reloadAffectedSubTree(element: PsiElement) {
        val rootContainingFile = project.service<MyProjectService>().rootFile

        if (rootContainingFile != element.containingFile) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(rootContainingFile, true)
            return
        }

        val affectedNode = getAffectedNode(element) ?: return

        if (affectedNode is Root && affectedNode !is RootWithExternalRef) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(element.containingFile, true)
            return
        }
        val nodeToReload = affectedNode.parent
        if (nodeToReload == null || nodeToReload !is MyNode) {
            return
        }

        val affected: DefaultMutableTreeNode? = project.service<MyProjectService>().getTreeNode(nodeToReload)
        if (affected == null) {
            window.treeModel.reload()
            return
        }
        if (affected.isRoot) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(element.containingFile, true)
            return
        }
        val parentOfParent = affected.parent
        val index = parentOfParent.getIndex(affected)
        window.treeModel.removeNodeFromParent(affected)
        val newChild = project.service<MyProjectService>().convertToNodes(affected.userObject as MyNode)
        window.treeModel.insertNodeInto(newChild, parentOfParent as DefaultMutableTreeNode, index)
    }

    private fun getAffectedNode(element: PsiElement?): MyNode? {
        var cur: PsiElement? = element ?: return null
        while (cur != null && cur !is XmlTag) {
            cur = cur.parent
        }
        if (cur == null) return null
        val domManager = DomManager.getDomManager(project)
        val curElement = domManager.getDomElement(cur as XmlTag)
        if (curElement is MyNode) {
            return curElement
        }
        return null
    }

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
        if (event.newChild is PsiWhiteSpace && event.newChild is PsiWhiteSpace) return
        val eventParent = event.parent
        if (eventParent is XmlDocument) {
            val oldChild1 = event.oldChild
            val newChild1 = event.newChild
            if (oldChild1 is XmlTag && newChild1 is XmlTag) {
                val domManager = DomManager.getDomManager(project)
                val oldChild = domManager.getDomElement(oldChild1)
                if (oldChild is Root && newChild1.name != "root") {
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

    override fun childAdded(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        val newChildElement = event.child
        val parentElement = event.parent
        if (parentElement is XmlDocument && newChildElement is XmlTag) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(parentElement.containingFile, true)
        } else {
            reloadAffectedSubTree(event.parent)
        }
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
        val newChildElement = event.child
        val parentElement = event.parent
        if (parentElement is XmlDocument && newChildElement is XmlTag) {
            project.service<MyProjectService>().reloadTree(parentElement.containingFile, true)
        } else if (newChildElement is XmlTag) {
            reloadAffectedSubTree(parentElement)
        }
    }
}