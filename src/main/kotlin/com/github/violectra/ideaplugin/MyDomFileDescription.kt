package com.github.violectra.ideaplugin

import com.intellij.util.xml.*


@NameStrategy(JavaNameStrategy::class)
interface MyNode : DomElement {
    fun getValue(): String
}

interface Root : MyNodeWithChildren

interface NodeA : MyNodeWithChildren, MyNodeWithIdAttribute

interface NodeB : MyNodeWithChildren, MyNodeWithIdAttribute

interface NodeRef : MyNodeWithIdAttribute {
    @Attribute("src")
    fun getSrc(): GenericAttributeValue<String>
}

open class NodeRefWithExternalRoot(base: Root, val ref: NodeRef) : Root by base

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

interface MyNodeWithIdAttribute : MyNode {
    @Attribute("id")
    fun getId(): GenericAttributeValue<String>

    @Attribute("title")
    fun getTitle(): GenericAttributeValue<String>
}

class MyDomFileDescription : DomFileDescription<Root>(Root::class.java, "root")