package com.github.violectra.ideaplugin.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MyToolWindow(private val myProject: Project) : JPanel(BorderLayout()), Disposable {
    val myComponentTree: Tree
    val treeModel: DefaultTreeModel

    init {
        val rootNode = DefaultMutableTreeNode("Hi")
        treeModel = DefaultTreeModel(rootNode)

        myComponentTree = Tree(treeModel)
        val treeScrollPane = ScrollPaneFactory.createScrollPane(myComponentTree)
        treeScrollPane.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
        treeScrollPane.preferredSize = Dimension(250, -1)
        add(myComponentTree)
//        myComponentTree.cellRenderer = DefaultTreeCellRenderer()

    }



    override fun dispose() {
        //todo
    }

}