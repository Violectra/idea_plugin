package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
import com.github.violectra.ideaplugin.model.*
import com.github.violectra.ideaplugin.toolWindow.MyPsiTreeChangeListener
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow
import com.github.violectra.ideaplugin.utils.MyNodeUtils
import com.github.violectra.ideaplugin.utils.XmlUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomEventListener
import com.intellij.util.xml.DomManager
import com.intellij.util.xml.events.DomEvent
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode


@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : Disposable {

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))


        DomManager.getDomManager(project).addDomEventListener(object : DomEventListener{
            override fun eventOccured(event: DomEvent) {
//                TODO("Not yet implemented")
            }
        } , this)

        val messageBusConnection = project.messageBus.connect(this)


        messageBusConnection.subscribe(ChangeTreeNotifier.CHANGE_MY_TREE_TOPIC,
            object : ChangeTreeNotifier {
                override fun handleTreeNodeInserting(current: Any, target: Any, isInto: Boolean, isAfter: Boolean) {
                    handleNodeInserting(current, target, isInto, isAfter)
                }
            })

    }

    fun startListener(window: MyToolWindow) {
        PsiManager.getInstance(project).addPsiTreeChangeListener(MyPsiTreeChangeListener(project, window), this)
    }

    fun handleEditorFileSelectionChanged(file: VirtualFile, isSameTree: Boolean) {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        reloadTree(psiFile, isSameTree)
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

    fun createNewNodeAndRemoveOld(
        currentNode: DefaultMutableTreeNode,
        targetNode: DefaultMutableTreeNode
    ) {
        val movableNode = MyNodeUtils.getMovableNode(currentNode.userObject as MyNode)
        WriteCommandAction.runWriteCommandAction(project) {
            val copy = movableNode.xmlElement!!.copy()

            val curXmlElement = MyNodeUtils.getMovableNode(currentNode.userObject as MyNode).xmlElement
            curXmlElement?.delete()

            val xmlElement = (targetNode.userObject as MyNode).xmlElement
            xmlElement?.add(copy)
        }
    }

    fun convertToNodes(child: MyNode): MutableTreeNode {
        val containingFile = child.xmlElement?.containingFile!!
        val parentPath = containingFile.virtualFile.toNioPath().parent
        val userSrc = mutableSetOf(containingFile.virtualFile.name)
        var c: MyNode? = child
        while (c != null) {
            if(c is RootWithExternalRef) {
                userSrc.plus(c.nodeRef.getSrc())
            }
            c = c.parent as? MyNode

        }
        return convertToTreeNode(child, parentPath!!, userSrc)
    }
}
