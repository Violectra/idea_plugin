package com.github.violectra.ideaplugin.listeners

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType

class MyPsiTreeChangeListener(private val project: Project) : PsiTreeChangeAdapter() {

    override fun childReplaced(event: PsiTreeChangeEvent) {
        if (event.newChild == null || event.newChild is PsiWhiteSpace) return
        if (event.child is XmlToken && event.child.elementType == XmlTokenType.XML_DATA_CHARACTERS) {
            reloadTree()
            return
        }
        reloadAffectedSubTree(event.parent)
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        if (event.child == null || event.child is PsiWhiteSpace) return
        if (event.child is XmlToken && event.child.elementType == XmlTokenType.XML_DATA_CHARACTERS) {
            reloadTree()
            return
        }
        reloadAffectedSubTree(event.parent)
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        if (event.child is XmlToken && event.child.elementType == XmlTokenType.XML_DATA_CHARACTERS) {
            reloadTree()
            return
        }
        reloadAffectedSubTree(event.parent)
    }

    private fun reloadAffectedSubTree(element: PsiElement) {
        project.service<MyProjectService>().reloadAffectedSubTree(element)
    }

    private fun reloadTree() {
        project.service<MyProjectService>().reloadTree()
    }

}