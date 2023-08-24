package com.github.violectra.ideaplugin.toolWindow

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(project)
        val content = ContentFactory.getInstance().createContent(myToolWindow, null, false)
        toolWindow.contentManager.addContent(content)
        project.service<MyProjectService>().startListener(myToolWindow)
    }

    override fun shouldBeAvailable(project: Project) = true
}
