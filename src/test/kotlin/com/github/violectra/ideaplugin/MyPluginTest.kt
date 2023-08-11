package com.github.violectra.ideaplugin

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import junit.framework.TestCase
import java.nio.file.Files
import java.nio.file.Path

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testUtilsWrite() {
        val bytes = Files.readAllBytes(Path.of("src/test/testData/Main.class"))
        val write = VirtualFileUtils.write(bytes)
        TestCase.assertTrue(write.contains("// class version"))
    }

    fun testUtilsRead() {
        val bytes = Files.readAllBytes(Path.of("src/test/testData/Main.class"))
        val mockFile = BinaryLightVirtualFile("", bytes)
        val read = VirtualFileUtils.read(mockFile)
        TestCase.assertTrue(read.contains("// class version"))
    }

//    fun testProjectService() {
//        val projectService = project.service<MyProjectService>()
//        val mockPsiManager = MockPsiManager(project)
//        project.modules[0] = MockModule(project)
//        val bytes = Files.readAllBytes(Path.of("src/test/testData/Main.class"))
//        val mockFile = BinaryLightVirtualFile("", bytes)
//        val file = MockPsiFile(mockFile, mockPsiManager)
//        try {
//            projectService.showBytecode(project, file)
//        } catch (e: Exception) {
//            TestCase.fail("Exception occurred: " + e.message);
//        }
//    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
