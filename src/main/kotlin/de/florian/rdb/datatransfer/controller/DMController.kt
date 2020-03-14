package de.florian.rdb.datatransfer.controller

import de.florian.rdb.datatransfer.extensions.getValueOptional
import de.florian.rdb.datatransfer.extensions.getValueOrEmpty
import de.florian.rdb.datatransfer.implementation.DatabaseQueryExecutor
import de.florian.rdb.datatransfer.implementation.mssql.DatabaseQueryExecutorMSQL
import de.florian.rdb.datatransfer.model.Connection
import de.florian.rdb.datatransfer.model.DMModel
import de.florian.rdb.datatransfer.view.DMView
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.sql.DriverManager
import java.util.*

class DMController(private var modelField: DMModel) {
    var databaseQueryExecutor: DatabaseQueryExecutor = DatabaseQueryExecutorMSQL()
        private set

    val model get() = modelField

    var storageService = StorageService()
        private set

    var view: DMView? = null
        private set

    init {
        this.model.sourceConnection.subscribe{
            refreshSourceDatabaseInformation()
        }
    }

    //TODO SHOW LOADING INDICATOR AND DONT RUN BLOCKING
    fun refreshSourceDatabaseInformation(){
        if(Objects.nonNull(databaseQueryExecutor)){
            this.model.sourceConnection.getValueOptional().ifPresent{
                runBlocking {
                    val res = async {databaseQueryExecutor!!.getDatabaseInformation(DriverManager.getConnection(it.jdbcUrl, it.username, it.password))}
                    model.database.onNext(Optional.of(res.await()))
                }
            }
        }
    }

    fun setView(dmView: DMView) {
        view = dmView
    }

    fun saveConnections() {
        this.storageService.saveConnections(this.model.storedConnections.getValueOrEmpty())
    }

    fun loadConnections() {
        this.model.storedConnections.onNext(this.storageService.retrieveConnections())
    }

    fun getConnections(): Collection<Connection> {
        return this.model.storedConnections.getValueOrEmpty()
    }

    fun addConnection(connection: Connection = Connection.template()) {
        this.model.storedConnections.onNext((getConnections() + connection))
    }

    fun removeConnection(connection: Connection) {
        this.model.storedConnections.onNext((getConnections() - connection))
    }

    fun getSourceConnection(): Optional<Connection> {
        return this.model.sourceConnection.getValueOptional()
    }

    fun getTargetConnection(): Optional<Connection> {
        return this.model.targetConnection.getValueOptional()
    }
}