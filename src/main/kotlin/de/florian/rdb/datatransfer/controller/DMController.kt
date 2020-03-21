package de.florian.rdb.datatransfer.controller

import de.florian.rdb.datatransfer.extensions.getValueOptional
import de.florian.rdb.datatransfer.extensions.getValueOrEmpty
import de.florian.rdb.datatransfer.implementation.DatabaseQueryExecutor
import de.florian.rdb.datatransfer.implementation.mssql.DatabaseQueryExecutorMSQL
import de.florian.rdb.datatransfer.model.DB
import de.florian.rdb.datatransfer.model.DBConnectionProperties
import de.florian.rdb.datatransfer.model.DMModel
import de.florian.rdb.datatransfer.view.DMView
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
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
        this.model.sourceConnectionProperties.subscribe{
            it.ifPresent{updateDatabaseInformation(it, model.sourceDatabase)}
        }
        this.model.targetConnectionProperties.subscribe{
            it.ifPresent{updateDatabaseInformation(it, model.targetDatabase)}
        }
        Observables.combineLatest(model.sourceDatabase, model.targetDatabase).subscribe {
            if(it.first.isPresent && it.second.isPresent){
                println("transferable")
            }
        }
    }

    //TODO SHOW LOADING INDICATOR AND DONT RUN BLOCKING
    fun updateDatabaseInformation(connectionProperties: DBConnectionProperties, dbModel: BehaviorSubject<Optional<DB>>) {
        if (Objects.nonNull(databaseQueryExecutor)) {
            runBlocking {
                val res = async {
                    databaseQueryExecutor!!.getDatabaseInformation(
                        DriverManager.getConnection(
                            connectionProperties.jdbcUrl,
                            connectionProperties.username,
                            connectionProperties.password
                        )
                    )
                }
                dbModel.onNext(Optional.of(res.await()))
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

    fun getConnections(): Collection<DBConnectionProperties> {
        return this.model.storedConnections.getValueOrEmpty()
    }

    fun addConnection(connectionProperties: DBConnectionProperties = DBConnectionProperties.template()) {
        this.model.storedConnections.onNext((getConnections() + connectionProperties))
    }

    fun removeConnection(connectionProperties: DBConnectionProperties) {
        this.model.storedConnections.onNext((getConnections() - connectionProperties))
    }

    fun getSourceConnection(): Optional<DBConnectionProperties> {
        return this.model.sourceConnectionProperties.getValueOptional()
    }

    fun getTargetConnection(): Optional<DBConnectionProperties> {
        return this.model.targetConnectionProperties.getValueOptional()
    }
}