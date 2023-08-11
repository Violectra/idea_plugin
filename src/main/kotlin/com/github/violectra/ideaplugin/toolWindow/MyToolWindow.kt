package com.github.violectra.ideaplugin.toolWindow

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import java.awt.BorderLayout
import java.util.*
import javax.swing.*

class MyToolWindow(private val myProject: Project) : JPanel(BorderLayout()), Disposable {
    @Suppress("JoinDeclarationAndAssignment")
    private val myEditor: Editor

    init {
        myEditor = EditorFactory.getInstance().createEditor(
            EditorFactory.getInstance().createDocument(""), myProject, JavaFileType.INSTANCE, true
        )
        myEditor.setBorder(null)
        add(myEditor.component)


        setText("Nothing to show, open java file and use an action")
    }


    private fun setText(resultText: String) {
        ApplicationManager.getApplication().runWriteAction {
            myEditor.document.setText(StringUtil.convertLineSeparators(resultText))
        }
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(myEditor)
    }

    fun updateText(text: String) {
        setText(text)
    }

}