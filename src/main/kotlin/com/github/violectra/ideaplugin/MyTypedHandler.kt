package com.github.violectra.ideaplugin

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.Alarm
import java.util.*


class MyTypedHandler : TypedHandlerDelegate(), Disposable {

    var myDocumentAlarm: Alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    //Alarm myDocumentAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    val MODIFIED_DOCUMENT_TIMEOUT_MS = 1000L


    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
//        if (!myDocumentAlarm.isDisposed) {
//            myDocumentAlarm.cancelAllRequests()
//
//            myDocumentAlarm.addRequest({
//                project.service<MyProjectService>().handleEditorFileSelectionChanged(file.virtualFile, true)
//            }, MODIFIED_DOCUMENT_TIMEOUT_MS)
//        }

        return Result.CONTINUE
    }

    override fun dispose() {
    }
}