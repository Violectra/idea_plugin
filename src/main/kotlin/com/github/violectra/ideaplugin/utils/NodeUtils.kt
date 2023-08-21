package com.github.violectra.ideaplugin.utils

import com.intellij.ui.RowsDnDSupport
import javax.swing.tree.DefaultMutableTreeNode

class NodeUtils {
    companion object {
        fun calculateIndexWithPosition(
            targetTreeNode: DefaultMutableTreeNode,
            curTreeNode: DefaultMutableTreeNode,
            position: RowsDnDSupport.RefinedDropSupport.Position
        ): Int {
            val intoIndex = calculateAboveIndex(targetTreeNode, curTreeNode)
            return if (position == RowsDnDSupport.RefinedDropSupport.Position.BELOW) intoIndex + 1 else intoIndex
        }

        private fun calculateAboveIndex(target: DefaultMutableTreeNode, current: DefaultMutableTreeNode): Int {
            val targetIndex = target.parent.getIndex(target)
            return if (target.parent == current.parent) {
                val currentIndex = target.parent.getIndex(current)
                if (targetIndex > currentIndex) targetIndex - 1 else targetIndex
            } else {
                targetIndex
            }
        }
    }
}