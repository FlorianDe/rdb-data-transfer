package de.florian.rdb.datatransfer.implementation

import de.florian.rdb.datatransfer.model.DB
import java.sql.Connection

abstract class DatabaseQueryExecutor {
    abstract suspend fun getDatabaseInformation(connection: Connection) : DB
}