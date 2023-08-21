package com.github.violectra.ideaplugin.model

import com.intellij.util.xml.Attribute
import com.intellij.util.xml.GenericAttributeValue

interface MyNodeWithIdAttribute : MyNode {
    @Attribute("id")
    fun getId(): GenericAttributeValue<String>

    @Attribute("title")
    fun getTitle(): GenericAttributeValue<String>
}