package com.github.violectra.ideaplugin

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service


class MyShowBytecodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: throw RuntimeException("No project found")
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: throw RuntimeException("No file found")


        val myProjectService = project.service<MyProjectService>()
        myProjectService.showBytecode(project, file)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
//        val file = e.getData(CommonDataKeys.PSI_FILE)
//        e.presentation.isEnabled = e.project != null && file?.fileType == JavaFileType.INSTANCE
    }
}