package com.github.violectra.ideaplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.violectra.ideaplugin.MyBundle
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    internal var window : MyToolWindow? = null

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }


    fun updateText(text: String) {
        window?.updateText(text)
    }
}
