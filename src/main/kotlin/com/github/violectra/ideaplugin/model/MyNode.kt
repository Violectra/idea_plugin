package com.github.violectra.ideaplugin.model

import com.intellij.util.xml.DomElement
import com.intellij.util.xml.JavaNameStrategy
import com.intellij.util.xml.NameStrategy

@NameStrategy(JavaNameStrategy::class)
interface MyNode : DomElement {
    fun getValue(): String
}