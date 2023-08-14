package com.github.violectra.ideaplugin

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

private const val GROUP_NAME = "Custom Notification Group"

class MyNotifier {
    companion object {
        fun notifyError(project: Project, content: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(GROUP_NAME)
                .createNotification(content, NotificationType.ERROR)
                .notify(project)
        }
    }
}