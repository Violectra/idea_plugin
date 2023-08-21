package com.github.violectra.ideaplugin.utils

import com.github.violectra.ideaplugin.model.*

class MyNodeUtils {
    companion object {

        fun getMovableNode(node: MyNode): MyNode {
            return if (node is RootWithExternalRef) node.nodeRef else node
        }

        fun objectToString(userObject: Any?) = if (userObject is MyNode) nodeToString(userObject) else null

        private fun nodeToString(root: MyNode): String {
            val tag = getTag(root)
            return when (root) {
                is RootWithExternalRef -> {
                    val nodeRef = root.nodeRef
                    val srcFileName = nodeRef.getSrc().value
                    val id = nodeRef.getId().value
                    "$tag[$id, $srcFileName] ${getTitle(nodeRef)}"
                }

                is NodeRef -> {
                    val srcFileName = root.getSrc().value
                    val id = root.getId().value
                    "$tag[$id, $srcFileName] ${getTitle(root)}"
                }

                is Root -> tag
                is MyNodeWithIdAttribute -> {
                    val id = root.getId().value
                    "$tag[$id] ${getTitle(root)}"
                }

                else -> ""
            }
        }

        private fun getTag(root: MyNode): String {
            val tag = when (root) {
                is NodeA -> "A"
                is NodeB -> "B"
                is NodeRef -> "Ref"
                is RootWithExternalRef -> "MyRef"
                else -> "root"
            }
            return tag
        }

        private fun getTitle(root: MyNodeWithIdAttribute) = root.getTitle().value ?: root.getValue()

    }
}