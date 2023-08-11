package com.github.violectra.ideaplugin

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class VirtualFileUtils {
    companion object {
        fun read(virtualFile: VirtualFile): String {
            try {
                val bytes: ByteArray = virtualFile.contentsToByteArray()
                return write(bytes)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        fun write(bytes: ByteArray): String {
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
    }
}