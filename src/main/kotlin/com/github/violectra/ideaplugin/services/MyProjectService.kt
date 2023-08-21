package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.github.violectra.ideaplugin.utils.XmlUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
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
class MyProjectService(private val project: Project) : Disposable {
    lateinit var window: MyToolWindow

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))

        val messageBusConnection = project.messageBus.connect(this)
        val fileEditorManager = FileEditorManager.getInstance(project)
        val psiDocumentManager = PsiDocumentManager.getInstance(project)

        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            fileEditorManager.selectedTextEditor?.document?.let {
                psiDocumentManager.getPsiFile(it)?.let { psi ->
                    showTree(psi)
                }
            }
        })
    }

    fun showTree(file: PsiFile) {
        try {
            window.treeModel.setRoot(readFileToTree(file))
        } catch (e: Exception) {
            MyNotifier.notifyError(project, e.message ?: "")
        }
    }

    fun clearTree() {
        try {
            window.treeModel.setRoot(null)
        } catch (e: Exception) {
            MyNotifier.notifyError(project, e.message ?: "")
        }
    }

    fun handleTreeNodeInserting(current: Any, target: Any, isInto: Boolean, isAfter: Boolean) {
        val movableNode = MyNodeUtils.getMovableNode(current as MyNode)
        val targetNode = target as MyNode
        WriteCommandAction.runWriteCommandAction(project) {
            if (isInto) {
                XmlUtils.xmlInsertInto(movableNode.xmlElement, targetNode.xmlElement)
            } else {
                XmlUtils.xmlInsertIntoPosition(movableNode.xmlElement, targetNode.xmlElement, isAfter)
            }
        }
    }

    private fun readFileToTree(file: PsiFile): DefaultMutableTreeNode? {
        val parentFilePath = file.virtualFile.toNioPath().parent
        return getXmlRoot(file)
            ?.let { convertToTreeNode(it, parentFilePath, setOf(file.name)) }
    }

    private fun findPsiFileByPath(path: Path): PsiFile? {
        return VirtualFileManager.getInstance().findFileByNioPath(path)
            ?.let { PsiManager.getInstance(project).findFile(it) }
    }

    private fun getXmlRoot(
        file: PsiFile,
    ) = if (file is XmlFile) {
        DomManager.getDomManager(project).getFileElement(file, Root::class.java)?.rootElement
    } else null

    private fun convertToTreeNode(
        node: MyNode,
        parentFilePath: Path,
        usedSrc: Set<String>,
    ): DefaultMutableTreeNode {
        val newNode = if (node is NodeRef) {
            node.getSrc().value
                ?.let { parentFilePath.resolve(it) }
                ?.let { findPsiFileByPath(it) }
                ?.takeIf { it.name !in usedSrc }
                ?.let { getXmlRoot(it) }
                ?.let { root -> RootWithExternalRef(root, node) } ?: node
        } else {
            node
        }
        val allowsChildren = newNode !is NodeRef
        val treeNode = DefaultMutableTreeNode(newNode, allowsChildren)
        val updatedUsedSrc = if (newNode is RootWithExternalRef) {
            newNode.nodeRef.getSrc().value?.let { usedSrc + it } ?: usedSrc
        } else usedSrc
        if (newNode is MyNodeWithChildren) {
            for (child in newNode.getSubNodes()) {
                treeNode.add(convertToTreeNode(child, parentFilePath, updatedUsedSrc))
            }
        }
        return treeNode
    }

    override fun dispose() {
        window.dispose()
    }

}
