package com.github.violectra.ideaplugin.services

import com.github.violectra.ideaplugin.*
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode


@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    lateinit var window: MyToolWindow

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun showBytecode(project: Project, file: PsiFile) {
        try {
            readDomXmlFile(project, file)
        } catch (e: Exception) {
            MyNotifier.notifyError(project, e.message ?: "")
        }
    }

    private fun readDomXmlFile(project: Project, file: PsiFile) {
        val root: Root = getXmlRoot(file, project)
        val rootPath = file.parent?.virtualFile?.toNioPath() ?: throw RuntimeException("Parent folder not found")
        val name = file.name

        val tag = root.xmlTag?.name
        val nodeString = nodeString(root, "", tag)
        val usedSrc = setOf(name)
        val treeRoot = window.rootNode
        treeRoot.userObject = nodeString
        for (child in root.getSubNodes()) {
            treeRoot.add(convertNode(child, 1, rootPath, project, usedSrc))
        }
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
    } else throw RuntimeException("File type is not XML")


    private fun convertNode(
        root: MyNode,
        indentLevel: Int,
        rootPath: Path,
        project: Project,
        usedSrc: Set<String>
    ): DefaultMutableTreeNode {
        val tag = root.xmlTag?.name
        val indent = " ".repeat(indentLevel)
        val newNode = DefaultMutableTreeNode(nodeString(root, indent, tag))
        if (root is MyNodeWithChildren) {
            for (child in root.getSubNodes()) {
                newNode.add(convertNode(child, indentLevel + 1, rootPath, project, usedSrc))
            }
        } else if (root is NodeRef) {
            val srcFileName = root.getSrc().value ?: throw RuntimeException("No src for ref")
            val path = rootPath.resolve(srcFileName)
            val file = findFile(path, project)
            if (file.name !in usedSrc) {
                val externalRoot: Root = getXmlRoot(file, project)
                for (child in externalRoot.getSubNodes()) {
                    newNode.add(convertNode(child, indentLevel + 1, rootPath, project, usedSrc + file.name))
                }
            }
        }
        return newNode
    }

    private fun nodeString(
        root: MyNode,
        indent: String,
        tag: String?
    ) = when (root) {
        is Root -> "$indent$tag"
        is NodeRef -> {
            val srcFileName = root.getSrc().value
            val id = root.getId().value
            "$indent$tag[$id, $srcFileName] ${getTitle(root)}"
        }

        is MyNodeWithIdAttribute -> {
            val id = root.getId().value
            "$indent$tag[$id] ${getTitle(root)}"
        }

        else -> ""
    }

    private fun getTitle(root: MyNodeWithIdAttribute) = root.getTitle().value ?: root.getValue()

    private fun getTextFromCorrespondingFile(project: Project, file: PsiFile): String {
        val index: ProjectFileIndex = ProjectFileIndex.getInstance(project)
        val virtualFile = file.virtualFile

        val text = if (virtualFile.fileType is JavaClassFileType) {
            VirtualFileUtils.read(virtualFile)
        } else {
            val classFilePath = findCorrespondingClassPath(index, virtualFile)
            try {
                VirtualFileUtils.write(Files.readAllBytes(classFilePath))
            } catch (e: IOException) {
                thisLogger().info("Bytecode file not found", e)
                throw RuntimeException("Bytecode file not found")
            }
        }
        return text
    }

    private fun findCorrespondingClassPath(
        index: ProjectFileIndex,
        virtualFile: VirtualFile
    ): Path {
        val module = index.getModuleForFile(virtualFile)
            ?: throw RuntimeException("No module for virtual file found")
        val sourceRoot = ModuleRootManager.getInstance(module).getSourceRoots(false)[0]
        val nioCompilerOutput = CompilerModuleExtension
            .getInstance(module)?.compilerOutputPath?.toNioPath()
            ?: throw RuntimeException("No compiler output path found")

        val relativePath = sourceRoot.toNioPath().relativize(virtualFile.toNioPath())
        val classFileParentPath = nioCompilerOutput.resolve(relativePath).parent
        return classFileParentPath.resolve(virtualFile.nameWithoutExtension + ".class")
    }


}
