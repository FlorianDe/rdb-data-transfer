package de.florian.rdb.datatransfer.controller

import de.florian.rdb.datatransfer.extensions.getValueOptional
import de.florian.rdb.datatransfer.extensions.getValueOrEmpty
import de.florian.rdb.datatransfer.model.Connection
import de.florian.rdb.datatransfer.model.DMModel
import de.florian.rdb.datatransfer.view.DMView
import java.util.*

class DMController(private var modelField: DMModel) {
    val model get() = modelField

    var storageService = StorageService()
        private set

    var view: DMView? = null
        private set

    fun setView(dmView: DMView) {
        view = dmView
    }

    fun saveConnections() {
        this.storageService.saveConnections(this.model.connections.getValueOrEmpty())
    }

    fun loadConnections() {
        this.model.connections.onNext(this.storageService.retrieveConnections())
    }

    fun getConnections(): Collection<Connection> {
        return this.model.connections.getValueOrEmpty()
    }

    fun addConnection(connection: Connection = Connection.template()) {
        this.model.connections.onNext((getConnections() + connection))
    }

    fun removeConnection(connection: Connection) {
        this.model.connections.onNext((getConnections() - connection))
    }

    fun getSourceConnection(): Optional<Connection> {
        return this.model.sourceConnection.getValueOptional()
    }

    fun getTargetConnection(): Optional<Connection> {
        return this.model.targetConnection.getValueOptional()
    }

}