package de.florian.rdb.datatransfer

import de.florian.jextensions.io.file.SingleInstanceLock
import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.model.DMModel
import de.florian.rdb.datatransfer.view.DMView
import org.slf4j.LoggerFactory
import javax.swing.SwingUtilities


@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
class Application {
    private val log = LoggerFactory.getLogger(javaClass)
    private val singleInstanceLock: SingleInstanceLock = SingleInstanceLock()

    init {
        val model: DMModel
        val controller: DMController
        var view: DMView

        if (!singleInstanceLock.isSingleInstanceRunning) {
            model = DMModel()
            controller = DMController(model)
            SwingUtilities.invokeLater { view = DMView(controller) }
        } else {
            log.error("An instance of this application is already running!")
        }
    }
}

fun main(args: Array<String>) {
    Application()
}


