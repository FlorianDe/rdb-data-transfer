package de.florian.rdb.datatransfer.model

import io.reactivex.subjects.BehaviorSubject
import kotlinx.serialization.Serializable
import java.util.*

data class DB(val name: String, var schemas: MutableList<Schema>)
data class Schema(val name: String, var tables: MutableList<Table>)
data class Table(val name: String, val columns: MutableList<Column>, var records: Long)
data class Column(val name: String, val type: String)

class DMModel {
    enum class DatabaseType {
        MSSQL
    }

    var selectedDatabaseType = DatabaseType.MSSQL

    var storedConnections: BehaviorSubject<Collection<DBConnectionProperties>> = BehaviorSubject.create()
        private set

    var sourceConnectionProperties: BehaviorSubject<Optional<DBConnectionProperties>> = BehaviorSubject.createDefault(Optional.empty())
        private set
    var sourceDatabase: BehaviorSubject<Optional<DB>> = BehaviorSubject.createDefault(Optional.empty())
        private set

    var targetConnectionProperties: BehaviorSubject<Optional<DBConnectionProperties>> = BehaviorSubject.createDefault(Optional.empty())
        private set
    var targetDatabase: BehaviorSubject<Optional<DB>> = BehaviorSubject.createDefault(Optional.empty())
        private set
}

@Serializable
data class DBConnectionProperties(
    var host: String,
    var port: Int = 0,
    /** Login username of the database. */
    var username: String,
    /** Login password of the database. */
    var password: String,
    var database: String,
    /** JDBC URL of the database. */
    var jdbcUrl: String,
    /** Fully qualified name of the JDBC driver. Auto-detected based on the URL by default. */
    var driverClassName: String,

    var name: String? = "",
    var comment: String? = ""
) {
    companion object {
        fun template() = DBConnectionProperties(
            host = "",
            port = 1433,
            username = "",
            password = "",
            database = "",
            jdbcUrl = "",
            driverClassName = "",
            name = "",
            comment = ""
        )
    }

    override fun toString(): String {
        return when {
            !this.name.isNullOrBlank() -> this.name!!
            !this.username.isBlank() && !this.host.isBlank() -> "$username@$host"
            else -> "Noname"
        }
    }
}

data class DataTransferTableModel(
    val db: String,
    val schema: String,
    val table: String
)