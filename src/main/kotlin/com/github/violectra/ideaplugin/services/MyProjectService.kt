package com.github.violectra.ideaplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.violectra.ideaplugin.MyBundle
import com.github.violectra.ideaplugin.VirtualFileUtils
import com.github.violectra.ideaplugin.toolWindow.MyToolWindow
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.IOException
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
        updateText(getTextFromCorrespondingFile(project, file))
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
