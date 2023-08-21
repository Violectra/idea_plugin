package com.github.violectra.ideaplugin.model

import com.intellij.util.xml.Attribute
import com.intellij.util.xml.GenericAttributeValue

interface NodeRef : MyNodeWithIdAttribute {
    @Attribute("src")
    fun getSrc(): GenericAttributeValue<String>
}