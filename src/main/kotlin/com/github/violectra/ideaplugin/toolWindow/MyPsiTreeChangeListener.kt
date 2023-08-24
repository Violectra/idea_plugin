package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getTreePath
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType.XML_NAME
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

class MyPsiTreeChangeListener(private val project: Project, private val window: MyToolWindow) : PsiTreeChangeAdapter() {

    override fun childReplaced(event: PsiTreeChangeEvent) {
        val eventParent = event.parent
        if (eventParent is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val element = domManager.getDomElement(eventParent)
            if (element is MyNode) {
                val treePath = window.tree.model.getTreePath(element)
                if (treePath != null) {
                    window.treeModel.reload()
                } else {
                    val child = event.child
                    if (child is XmlToken) {
                        if (child.tokenType == XML_NAME) {
                            val curNode: MyNode = element
                            val parent = curNode.parent as? MyNodeWithChildren ?: return
                            val indexOfNewElement = parent.getSubNodes().indexOf(curNode)
                            val newChild = project.service<MyProjectService>().convertToNodes(curNode)
                            window.handleChildAdded(parent, newChild, indexOfNewElement)
                            return
                        }
                    }
                }
            }
        } else if (eventParent is XmlDocument) {
            val oldChild1 = event.oldChild
            val newChild1 = event.newChild
            if (oldChild1 is XmlTag && newChild1 is XmlTag) {
                val domManager = DomManager.getDomManager(project)
                val newChild = domManager.getDomElement(newChild1)
                if ((newChild is Root && oldChild1.name != "root") || (oldChild1.name == "root" && newChild1.name != "root")) {
                    project.service<MyProjectService>().reloadTree(eventParent.containingFile, true)
                }
            }
        } else if (eventParent is XmlAttributeValue) {
            val xmlAttribute = event.parent.parent as XmlAttribute
            val attrName = xmlAttribute.name
            if (attrName == "src") {
                val domManager = DomManager.getDomManager(project)
                val parent = domManager.getDomElement(xmlAttribute.parent)
                if (parent is NodeRef) {
                    val newChild = project.service<MyProjectService>().convertToNodes(parent)
                    val oldNodeParent =
                        window.treeModel.getTreePath(parent.parent)?.lastPathComponent as? MutableTreeNode ?: return
                    val oldNode = findOldNode(parent, oldNodeParent) ?: return

                    val indexOfChild = window.treeModel.getIndexOfChild(oldNodeParent, oldNode)

                    window.treeModel.removeNodeFromParent(oldNode)
                    window.treeModel.insertNodeInto(newChild, oldNodeParent, indexOfChild)
                }
            }

        } else if (eventParent is XmlAttribute) {
            if (event.newChild.text == "src" || event.oldChild.text == "src") {
                val domManager = DomManager.getDomManager(project)
                val parent = domManager.getDomElement(eventParent.parent)
                if (parent is NodeRef) {
                    val newChild = project.service<MyProjectService>().convertToNodes(parent)
                    val oldNodeParent =
                        window.treeModel.getTreePath(parent.parent)?.lastPathComponent as? MutableTreeNode ?: return
                    val oldNode = findOldNode(parent, oldNodeParent) ?: return
                    val indexOfChild = window.treeModel.getIndexOfChild(oldNodeParent, oldNode)
                    window.treeModel.removeNodeFromParent(oldNode)
                    window.treeModel.insertNodeInto(newChild, oldNodeParent, indexOfChild)
                }
            }
        } else {
            window.treeModel.reload()
        }
    }

    private fun findOldNode(
        parent: DomElement?,
        oldNodeParent: MutableTreeNode
    ) = (window.treeModel.getTreePath(parent)?.lastPathComponent as? MutableTreeNode
        ?: oldNodeParent.children().toList().filterIsInstance<DefaultMutableTreeNode>()
            .find {
                val userObject = it.userObject
                return@find (userObject is RootWithExternalRef && userObject.nodeRef == parent)
            })

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
        val eventParent = event.parent
        if (eventParent is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val par = domManager.getDomElement(eventParent)
            if (par is MyNode) {
                if (window.tree.model.getTreePath(par) != null) {
                    val child = event.newChild
                    if (child is XmlToken) {
                        if (child.tokenType == XML_NAME) {
                            if (child.text !in setOf("nodeA", "nodeB", "nodeRef", "root")) {
                                window.handleChildRemoving(par)
                            }
                        }
                    }
                }
            }
        } else if (eventParent is XmlDocument) {
            val oldChild1 = event.oldChild
            val newChild1 = event.newChild
            if (oldChild1 is XmlTag && newChild1 is XmlTag) {
                val domManager = DomManager.getDomManager(project)
                val oldChild = domManager.getDomElement(oldChild1)
                if (oldChild is Root && newChild1.name != "root") {
                    window.treeModel.setRoot(null)

                }
            }
        }
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        val newChildElement = event.child
        val parentElement = event.parent
        if (newChildElement is XmlTag && parentElement is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val child = domManager.getDomElement(newChildElement)
            val parent = domManager.getDomElement(parentElement)

            if (child is MyNode && parent is MyNodeWithChildren) {
                val indexOfNewElement = parent.getSubNodes().indexOf(child)
                val newChild = project.service<MyProjectService>().convertToNodes(child)
                window.handleChildAdded(parent, newChild, indexOfNewElement)
            }
        } else if (parentElement is XmlDocument && newChildElement is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(parentElement.containingFile, true)
        }
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
        val newChildElement = event.child
        val parentElement = event.parent
        if (parentElement is XmlDocument && newChildElement is XmlTag) {
            window.treeModel.setRoot(null)
            project.service<MyProjectService>().reloadTree(parentElement.containingFile, true)
        } else if (newChildElement is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val child = domManager.getDomElement(newChildElement)
            if (child is MyNode) {
                window.handleChildRemoving(child)
            }
        }
    }
}