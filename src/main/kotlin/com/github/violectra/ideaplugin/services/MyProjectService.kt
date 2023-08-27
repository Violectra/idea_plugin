package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
import com.github.violectra.ideaplugin.listeners.ReloadTreeListener
import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.listeners.MyPsiTreeChangeListener
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomManager
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode


@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : Disposable {

    lateinit var rootFile: PsiFile
    private val treeNodesByDomNodes = HashMap<MyNode, DefaultMutableTreeNode>()

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun startListener(window: MyToolWindow) {
        PsiManager.getInstance(project).addPsiTreeChangeListener(MyPsiTreeChangeListener(project, window), this)
    }

    fun reloadTreeForNewFile(file: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        reloadTreeForFile(psiFile)
    }

    fun clearTree() {
        treeNodesByDomNodes.clear()
        reloadTreeWithNewRoot(null)
    }

    fun reloadTreeForCurrentFile() {
        reloadTreeForFile(rootFile)
    }

    private fun reloadTreeForFile(file: PsiFile) {
        treeNodesByDomNodes.clear()
        rootFile = file
        reloadTreeWithNewRoot(readDomStructureTreeNode(file))
    }

    private fun reloadTreeWithNewRoot(root: DefaultMutableTreeNode?) {
        project.messageBus.syncPublisher(ReloadTreeListener.RELOAD_MY_TREE_TOPIC).handleTreeReloading(root)
    }

    private fun readDomStructureTreeNode(file: PsiFile): DefaultMutableTreeNode? {
        val parentFilePath = file.virtualFile.toNioPath().parent
        return getDomRoot(file)
            ?.let { convertToTreeNode(it, parentFilePath, setOf(file.name)) }
    }

    private fun findPsiFileByPath(path: Path): PsiFile? {
        return VirtualFileManager.getInstance().findFileByNioPath(path)
            ?.let { PsiManager.getInstance(project).findFile(it) }
    }

    private fun getDomRoot(
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
                ?.let { getDomRoot(it) }
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
        treeNodesByDomNodes[node] = treeNode
        treeNodesByDomNodes[newNode] = treeNode
        return treeNode
    }

    fun insertPsiElement(currentNode: Any, targetNode: Any) {
        val movableNode: DomElement = MyNodeUtils.getMovableNode(currentNode as MyNode)
        val target = targetNode as DomElement
        WriteCommandAction.runWriteCommandAction(project) {
            val copy = movableNode.xmlElement?.copy() ?: return@runWriteCommandAction
            movableNode.xmlElement?.delete()
            target.xmlElement?.add(copy)
        }
    }

    fun convertToNodes(child: MyNode): DefaultMutableTreeNode {
        val parentPath = rootFile.virtualFile.toNioPath().parent
        val userSrc = calculateUsedSrcForNode(child)
        return convertToTreeNode(child, parentPath!!, userSrc)
    }

    private fun calculateUsedSrcForNode(child: MyNode): Set<String> {
        val userSrc = mutableSetOf(rootFile.virtualFile.name)
        var cur: MyNode? = child
        while (cur != null) {
            if (cur is RootWithExternalRef) {
                cur.nodeRef.getSrc().value?.let { userSrc.add(it) }
            }
            cur = cur.parent as? MyNode
        }
        return userSrc
    }

    fun getTreeNode(p: MyNode): DefaultMutableTreeNode? {
        return treeNodesByDomNodes[p]
    }

    override fun dispose() {
    }
}
