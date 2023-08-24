package com.github.violectra.ideaplugin.model

import com.intellij.util.messages.Topic
import kotlin.Any


interface ChangeTreeNotifier {
    fun handleTreeNodeInserting(current: Any, target: Any, isInto: Boolean, isAfter: Boolean)

    companion object {
        val CHANGE_MY_TREE_TOPIC = Topic.create("CHANGE_MY_TREE", ChangeTreeNotifier::class.java)
    }
}