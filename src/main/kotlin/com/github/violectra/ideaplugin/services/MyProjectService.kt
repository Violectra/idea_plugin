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
            readDomXmlFile(project, file)
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

    private fun readDomXmlFile(project: Project, file: PsiFile) {
        val parentFilePath = file.virtualFile.toNioPath().parent
        val usedSrc = setOf(file.name)

        val root: Root? = getXmlRoot(file, project)
        val treeRootNode = if (root != null) {
            convertToTreeNode(root, parentFilePath, project, usedSrc)
        } else null

        window.treeModel.setRoot(treeRootNode)
    }

    private fun findInternalRefFile(path: Path, project: Project): PsiFile {
        return VirtualFileManager.getInstance().findFileByNioPath(path)
            ?.let { PsiManager.getInstance(project).findFile(it) }
            ?: throw RuntimeException("No internal ref file found")
    }

    private fun getXmlRoot(
        file: PsiFile,
        project: Project
    ) = if (file is XmlFile) {
        DomManager.getDomManager(project).getFileElement(file, Root::class.java)?.rootElement
    } else null


    private fun convertToTreeNode(
        root: MyNode,
        parentFilePath: Path,
        project: Project,
        usedSrc: Set<String>
    ): DefaultMutableTreeNode {
        val newNode: DefaultMutableTreeNode
        if (root is MyNodeWithChildren) {
            newNode = DefaultMutableTreeNode(root)
            for (child in root.getSubNodes()) {
                newNode.add(convertToTreeNode(child, parentFilePath, project, usedSrc))
            }
        } else if (root is NodeRef) {
            val srcFileName = root.getSrc().value ?: throw RuntimeException("No src for ref")
            val path = parentFilePath.resolve(srcFileName)
            val file = findInternalRefFile(path, project)
            if (file.name !in usedSrc) {
                val externalRoot: Root? = getXmlRoot(file, project)
                if (externalRoot == null) {
                    newNode = DefaultMutableTreeNode(root)
                    MyNotifier.notifyError(project, "Ref file is not XML or doesn't have root element")
                } else {
                    newNode = DefaultMutableTreeNode(NodeRefWithExternalRoot(externalRoot, root))
                    for (child in externalRoot.getSubNodes()) {
                        newNode.add(convertToTreeNode(child, parentFilePath, project, usedSrc + file.name))
                    }
                }
            } else {
                newNode = DefaultMutableTreeNode(root)
            }
        } else {
            throw RuntimeException("Unknown node")
        }
        return newNode
    }

    override fun dispose() {
        window.dispose()
    }

}
