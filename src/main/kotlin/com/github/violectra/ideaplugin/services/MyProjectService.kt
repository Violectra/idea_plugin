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
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomManager
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path


@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    internal var window : MyToolWindow? = null

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }


    private fun updateText(text: String) {
        window?.updateText(text)
    }

    fun showBytecode(project: Project, file: PsiFile) {
        try {
            updateText(readDomXmlFile(project, file))
        } catch (e: Exception) {
            MyNotifier.notifyError(project, e.message ?: "")
        }
    }

    private fun readDomXmlFile(project: Project, file: PsiFile): String {
        val manager = DomManager.getDomManager(project)
        val virtualFile = file
        if (virtualFile is XmlFile) {
            val root: Root = manager.getFileElement(virtualFile, Root::class.java)?.rootElement ?: throw RuntimeException("Root not found")
            val writer = StringWriter()
            PrintWriter(writer).use { printWriter ->
                printNode(root, 0, printWriter)
            }
            return writer.toString()
        }
        throw RuntimeException("File is not correct")
    }

    private fun printNode(root: MyNode, indentLevel: Int, writer: PrintWriter) {
        val tag = root.xmlTag?.name
        val indent = " ".repeat(indentLevel)
        writer.println(
            when (root) {
                is Root -> "$indent$tag"
                is NodeRef -> {
                    val title = root.getTitle().value ?: root.getValue()
                    val srcFileName = root.getSrc().value
                    val info = "${root.getId().value}, $srcFileName"
                    "$indent$tag[$info] $title"
                }
                is MyNodeWithIdAttribute -> {
                    val title = root.getTitle().value ?: root.getValue()
                    val info = root.getId().value
                    "$indent$tag[$info] $title"
                }
                else -> ""
            }
        )
        if (root is MyNodeWithChildren) {
            for (child in root.getSubNodes()) {
                printNode(child, indentLevel + 1, writer)
            }
        }
    }

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
