package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getTreePath
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType.XML_NAME
import com.intellij.util.xml.DomManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

class MyPsiTreeChangeListener(private val project: Project, private val window: MyToolWindow) : PsiTreeChangeAdapter() {

    override fun childReplaced(event: PsiTreeChangeEvent) {
        val eventParent = event.parent
        if (eventParent is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val par = domManager.getDomElement(eventParent)
            if (par is MyNode) {
                if (window.tree.model.getTreePath(par) != null) {
                    window.handleTreeChangesForSubtree(par)
                } else {
                    val child = event.child
                    if (child is XmlToken) {
                        if (child.tokenType == XML_NAME) {
                            // added new node
                            val c: MyNode = par
                            //todo: handle root case
                            val parent = c.parent as? MyNodeWithChildren ?: return
                            val indexOfNewElement = parent.getSubNodes().indexOf(c)
                            val newChild = project.service<MyProjectService>().convertToNodes(c)
                            window.handleChildAdded(parent, newChild, indexOfNewElement)
                        }
                    }
                }
            }
        }
    }

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
                            if (child.text !in setOf("nodeA", "nodeB", "nodeRef")) {
                                window.handleChildRemoving(par)
                            }
                        }
                    }
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
        }
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
        val newChildElement = event.child
        if (newChildElement is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val child = domManager.getDomElement(newChildElement)
            if (child is MyNode) {
                window.handleChildRemoving(child)
            }
        }
    }
}