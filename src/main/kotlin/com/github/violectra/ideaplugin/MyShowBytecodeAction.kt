package com.github.violectra.ideaplugin

import com.github.violectra.ideaplugin.services.MyProjectService
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path


class MyShowBytecodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: throw RuntimeException("No project found")
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: throw RuntimeException("No file found")

        val index: ProjectFileIndex = ProjectFileIndex.getInstance(project)
        val virtualFile = file.virtualFile

        val text = if (virtualFile.fileType is JavaClassFileType) {
            read(virtualFile)
        } else {
            val classFilePath = findCorrespondingClassPath(index, virtualFile)
            try {
                write(Files.readAllBytes(classFilePath))
            } catch (e: IOException) {
                thisLogger().info("Bytecode file not found", e)
                return
            }
        }

        val myProjectService = project.service<MyProjectService>()
        myProjectService.updateText(text)
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
        val resolved = classFileParentPath.resolve(virtualFile.nameWithoutExtension + ".class")
        return resolved
    }

    private fun read(virtualFile: VirtualFile): String {
        try {
            val bytes: ByteArray = virtualFile.contentsToByteArray(true)
            return write(bytes)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun write(bytes: ByteArray): String {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            ClassReader(bytes).accept(
                TraceClassVisitor(
                    null,
                    Textifier(),
                    printWriter
                ), 0
            )
        }
        return writer.toString()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
//        val file = e.getData(CommonDataKeys.PSI_FILE)
//        e.presentation.isEnabled = e.project != null && file?.fileType == JavaFileType.INSTANCE
    }
}