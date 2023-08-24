package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.model.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import javax.swing.tree.DefaultMutableTreeNode

class MyPsiTreeChangeListener(private val project: Project, private val window: MyToolWindow) : PsiTreeChangeAdapter() {

    override fun childReplaced(event: PsiTreeChangeEvent) {
        window.handleTreeChanges()
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        val newChildElement = event.child
        val parentElement = event.parent
        if (newChildElement is XmlTag && parentElement is XmlTag) {
            val domManager = DomManager.getDomManager(project)
            val child = domManager.getDomElement(newChildElement)
            val parent = domManager.getDomElement(parentElement)
//            if (child?.exists() == true) {
//                return
//            }
            if (child is MyNode && parent is MyNodeWithChildren) {
                val indexOfNewElement = parent.getSubNodes().indexOf(child)
                val newChild = DefaultMutableTreeNode(child, child !is NodeRef)
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