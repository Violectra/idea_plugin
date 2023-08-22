package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.github.violectra.ideaplugin.utils.XmlUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))

        val messageBusConnection = project.messageBus.connect(this)
        val fileEditorManager = FileEditorManager.getInstance(project)
        val psiDocumentManager = PsiDocumentManager.getInstance(project)

        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            fileEditorManager.selectedTextEditor?.document?.let {
                psiDocumentManager.getPsiFile(it)?.let { psiFile ->
                    reloadTree(psiFile, true)
                }
            }
        })
        messageBusConnection.subscribe(ChangeTreeNotifier.CHANGE_MY_TREE_TOPIC,
            object : ChangeTreeNotifier {
                override fun handleTreeNodeInserting(current: Any, target: Any, isInto: Boolean, isAfter: Boolean) {
                    handleNodeInserting(current, target, isInto, isAfter)
                }
            })
    }

    fun handleEditorFileSelectionChanged(file: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        reloadTree(psiFile, false)
    }

    fun handleNodeInserting(current: Any, target: Any, isInto: Boolean, isAfter: Boolean) {
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

    fun clearTree() {
        reloadTreeWithNewRoot(null, false)
    }

    private fun reloadTree(file: PsiFile, isSameTree: Boolean) {
        reloadTreeWithNewRoot(readFileToTree(file), isSameTree)
    }

    private fun reloadTreeWithNewRoot(root: DefaultMutableTreeNode?, isSameTree: Boolean) {
        val publisher = project.messageBus.syncPublisher(ReloadTreeNotifier.RELOAD_MY_TREE_TOPIC)
        publisher.handleTreeReloading(root, isSameTree)
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
    }

}
