package com.github.violectra.ideaplugin.utils

import javax.swing.tree.DefaultMutableTreeNode

class TreeNodeUtils {
    companion object {
        fun calculateIndexWithPosition(
            targetTreeNode: DefaultMutableTreeNode,
            curTreeNode: DefaultMutableTreeNode,
            isBelow: Boolean
        ): Int {
            val intoIndex = calculateAboveIndex(targetTreeNode, curTreeNode)
            return if (isBelow) intoIndex + 1 else intoIndex
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