package com.github.violectra.ideaplugin.utils

import com.intellij.psi.xml.XmlElement
import com.intellij.ui.RowsDnDSupport

class XmlUtils {
    companion object {

        fun xmlInsertIntoPosition(
            current: XmlElement?,
            target: XmlElement?,
            position: RowsDnDSupport.RefinedDropSupport.Position
        ) {
            current?.let { cur ->
                target?.parent?.let { targetParent ->
                    if (position == RowsDnDSupport.RefinedDropSupport.Position.BELOW) {
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