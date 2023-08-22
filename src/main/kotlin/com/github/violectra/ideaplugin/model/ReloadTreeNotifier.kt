package com.github.violectra.ideaplugin.model

import com.intellij.util.messages.Topic
import javax.swing.tree.DefaultMutableTreeNode


interface ReloadTreeNotifier {
    fun handleTreeReloading(root: DefaultMutableTreeNode?, isSameTree: Boolean)

    companion object {
        val RELOAD_MY_TREE_TOPIC = Topic.create("RELOAD_MY_TREE", ReloadTreeNotifier::class.java)
    }
}