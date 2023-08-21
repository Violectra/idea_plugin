package com.github.violectra.ideaplugin.model

import com.intellij.util.xml.SubTagsList

interface MyNodeWithChildren : MyNode {
    fun getNodeAs(): List<NodeA>
    fun getNodeBs(): List<NodeB>
    fun getNodeRefs(): List<NodeRef>

    @SubTagsList("nodeA", "nodeB", "nodeRef")
    fun getSubNodes(): List<MyNodeWithIdAttribute>

    @SubTagsList("nodeA", "nodeB", "nodeRef", tagName = "nodeA")
    fun addNodeA(): NodeA

    @SubTagsList("nodeA", "nodeB", "nodeRef", tagName = "nodeB")
    fun addNodeB(): NodeB

    @SubTagsList("nodeA", "nodeB", "nodeRef", tagName = "nodeRef")
    fun addNodeRef(): NodeRef
}