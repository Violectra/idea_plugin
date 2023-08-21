package com.github.violectra.ideaplugin.listeners

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class MyEditorManagerListener : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (event.newFile != null) {
            super.selectionChanged(event)

            val project = event.manager.project
            val myProjectService = project.service<MyProjectService>()
            val psiFile = PsiManager.getInstance(project).findFile(event.newFile) ?: return
            myProjectService.showTree(psiFile)
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        super.fileClosed(source, file)

        val myProjectService = source.project.service<MyProjectService>()
        myProjectService.clearTree()
    }
}
