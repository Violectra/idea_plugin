package com.github.violectra.ideaplugin.model

import com.intellij.util.messages.Topic
import javax.swing.tree.TreeNode


interface ReloadTreeNotifier {
    companion object {
        val RELOAD_MY_TREE_TOPIC = Topic.create("RELOAD_MY_TREE", ReloadTreeNotifier::class.java)
    }

    fun handleTreeReloading(root: TreeNode?, isSameTree: Boolean)
}