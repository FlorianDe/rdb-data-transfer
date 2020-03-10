package de.florian.rdb.datatransfer.view

import de.florian.rdb.datatransfer.controller.DMController


class DMView(private val controller: DMController) {
    private val mcFrame: DMFrame

    init {
        this.controller.setView(this)
        this.mcFrame = DMFrame(controller)
    }
}