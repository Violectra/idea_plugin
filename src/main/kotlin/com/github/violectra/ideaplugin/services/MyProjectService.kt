package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomManager
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode


@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) : Disposable {
    lateinit var window: MyToolWindow

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))

        val messageBusConnection = project.messageBus.connect(this)
        val fileEditorManager = FileEditorManager.getInstance(project)
        val psiDocumentManager = PsiDocumentManager.getInstance(project)

        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            fileEditorManager.selectedTextEditor?.document?.let {
                psiDocumentManager.getPsiFile(it)?.let { psi ->
                    showTree(project, psi)
                }
            }
        })
    }

    fun showTree(project: Project, file: PsiFile) {
        try {
            window.treeModel.setRoot(readFileToTree(project, file))
        } catch (e: Exception) {
            MyNotifier.notifyError(project, e.message ?: "")
        }
    }

    fun clearTree(project: Project) {
        try {
            window.treeModel.setRoot(null)
        } catch (e: Exception) {
            MyNotifier.notifyError(project, e.message ?: "")
        }
    }

    private fun readFileToTree(project: Project, file: PsiFile): DefaultMutableTreeNode? {
        val parentFilePath = file.virtualFile.toNioPath().parent
        return getXmlRoot(file, project)
            ?.let { convertToTreeNode(it, parentFilePath, project, setOf(file.name)) }
    }

    private fun findPsiFileByPath(path: Path, project: Project): PsiFile? {
        return VirtualFileManager.getInstance().findFileByNioPath(path)
            ?.let { PsiManager.getInstance(project).findFile(it) }
    }

    private fun getXmlRoot(
        file: PsiFile,
        project: Project
    ) = if (file is XmlFile) {
        DomManager.getDomManager(project).getFileElement(file, Root::class.java)?.rootElement
    } else null

    private fun convertToTreeNode(
        node: MyNode,
        parentFilePath: Path,
        project: Project,
        usedSrc: Set<String>,
    ): DefaultMutableTreeNode {
        val newNode = if (node is NodeRef) {
            node.getSrc().value
                ?.let { parentFilePath.resolve(it) }
                ?.let { findPsiFileByPath(it, project) }
                ?.takeIf { it.name !in usedSrc }
                ?.let { getXmlRoot(it, project) }
                ?.let { root -> NodeRefWithExternalRoot(root, node) } ?: node
        } else {
            node
        }
        val treeNode = DefaultMutableTreeNode(newNode)
        val updatedUsedSrc = if (newNode is NodeRefWithExternalRoot) {
            newNode.ref.getSrc().value?.let { usedSrc + it } ?: usedSrc
        } else usedSrc
        if (newNode is MyNodeWithChildren) {
            for (child in newNode.getSubNodes()) {
                treeNode.add(convertToTreeNode(child, parentFilePath, project, updatedUsedSrc))
            }
        }
        return treeNode
    }

    override fun dispose() {
        window.dispose()
    }

}
