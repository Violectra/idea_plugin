package com.github.violectra.ideaplugin.utils

import com.intellij.psi.xml.XmlElement

class XmlUtils {
    companion object {

        fun xmlInsertIntoPosition(
            current: XmlElement?,
            target: XmlElement?,
            isAfter: Boolean
        ) {
            current?.let { cur ->
                target?.parent?.let { targetParent ->
                    if (isAfter) {
                        targetParent.addAfter(cur, target)
                    } else {
                        targetParent.addBefore(cur, target)
                    }
                    cur.parent.deleteChildRange(cur, cur)
                }
            }
        }

        fun xmlInsertInto(
            currentElement: XmlElement?,
            targetElement: XmlElement?
        ) {
            currentElement?.let { cur ->
                targetElement?.let { target ->
                    target.add(cur)
                    cur.parent.deleteChildRange(cur, cur)
                }
            }
        }

    }
}