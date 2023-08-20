package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
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
class MyProjectService(project: Project): Disposable {
    lateinit var window: MyToolWindow

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))

        val messageBusConnection = project.messageBus.connect(this)

        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            FileEditorManager.getInstance(project).getSelectedTextEditor()?.getDocument()?.let {
                PsiDocumentManager.getInstance(project).getPsiFile(it)?.let { showTree(project, it) }
            }
        })
    }

    fun showTree(project: Project, file: PsiFile) {
        try {
            readDomXmlFile(project, file) ?: clearTree(project)
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

        val root: Root? = getXmlRoot(file, project)
        if (root == null) {
            clearTree(project)
            return
        }
        val rootPath = file.parent?.virtualFile?.toNioPath() ?: throw RuntimeException("Parent folder not found")
        val name = file.name

        val newRoot = convertNode(root, 1, rootPath, project, setOf(name))
        window.treeModel.setRoot(newRoot)
    }

    private fun findFile(path: Path, project: Project): PsiFile {
        val file =
            VirtualFileManager.getInstance().findFileByNioPath(path) ?: throw RuntimeException("No internal file found")
        return PsiManager.getInstance(project).findFile(file) ?: throw RuntimeException("No internal file found")
    }

    private fun getXmlRoot(
        file: PsiFile,
        project: Project
    ) = if (file is XmlFile) {
        DomManager.getDomManager(project).getFileElement(file, Root::class.java)?.rootElement
            ?: throw RuntimeException("Root not found")
    } else null


    private fun convertNode(
        root: MyNode,
        indentLevel: Int,
        rootPath: Path,
        project: Project,
        usedSrc: Set<String>
    ): DefaultMutableTreeNode {
        val newNode: DefaultMutableTreeNode
        if (root is MyNodeWithChildren) {
            newNode = DefaultMutableTreeNode(root)
            for (child in root.getSubNodes()) {
                newNode.add(convertNode(child, indentLevel + 1, rootPath, project, usedSrc))
            }
        } else if (root is NodeRef) {
            val srcFileName = root.getSrc().value ?: throw RuntimeException("No src for ref")
            val path = rootPath.resolve(srcFileName)
            val file = findFile(path, project)
            if (file.name !in usedSrc) {
                val externalRoot: Root? = getXmlRoot(file, project)
                if (externalRoot == null) {
                    newNode = DefaultMutableTreeNode(root)
                    MyNotifier.notifyError(project, "Ref file is not XML")
                } else {
                    newNode = DefaultMutableTreeNode(NodeRefWithExternalRoot(externalRoot, root))
                    for (child in externalRoot.getSubNodes()) {
                        newNode.add(convertNode(child, indentLevel + 1, rootPath, project, usedSrc + file.name))
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
        TODO("Not yet implemented")
    }

}
