package com.github.violectra.ideaplugin

import com.github.violectra.ideaplugin.model.Root
import com.intellij.util.xml.DomFileDescription


class MyDomFileDescription : DomFileDescription<Root>(Root::class.java, "root")