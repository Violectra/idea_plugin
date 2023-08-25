package com.github.violectra.ideaplugin.listeners

import com.intellij.util.messages.Topic
import javax.swing.tree.TreeNode


interface ReloadTreeListener {
    companion object {
        val RELOAD_MY_TREE_TOPIC = Topic.create("RELOAD_MY_TREE", ReloadTreeListener::class.java)
    }

    fun handleTreeReloading(root: TreeNode?)
}