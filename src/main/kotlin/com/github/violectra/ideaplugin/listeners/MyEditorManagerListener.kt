package com.github.violectra.ideaplugin.listeners

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class MyEditorManagerListener : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (event.newFile != null) {
            event.manager.project.service<MyProjectService>().reloadTreeForNewFile(event.newFile)
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        source.project.service<MyProjectService>().clearTree()
    }
}
