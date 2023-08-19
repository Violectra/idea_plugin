package com.github.violectra.ideaplugin.listeners

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.psi.PsiManager

class MyEditorManagerListener : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)
        thisLogger().warn("!!!!! selectionChanged ${event.oldFile?.name} -> ${event.newFile?.name}")

        val project = event.manager.project
        val myProjectService = project.service<MyProjectService>()
        val psiFile = PsiManager.getInstance(project).findFile(event.newFile) ?: throw RuntimeException("Can't find psi")
        myProjectService.showTree(project, psiFile)
    }
}
