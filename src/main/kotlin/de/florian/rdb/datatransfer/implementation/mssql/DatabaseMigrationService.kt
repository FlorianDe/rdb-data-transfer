package de.florian.rdb.datatransfer.implementation.mssql

import com.microsoft.sqlserver.jdbc.SQLServerBulkCopy
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopyOptions
import com.microsoft.sqlserver.jdbc.SQLServerException
import de.florian.rdb.datatransfer.model.DBConnectionProperties
import de.florian.rdb.datatransfer.model.DataTransferTableModel
import org.slf4j.LoggerFactory
import java.sql.*

class DatabaseMigrationService(
    private val sourceConnectionProperties: DBConnectionProperties,
    private val targetConnectionProperties: DBConnectionProperties,
    private val dbObjectTransferList: List<DataTransferTableModel>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    var sourceConnection: Connection
    var targetConnection: Connection

    var batchSize: Int = 5_000
    var tableCopyTimeout = 30 * 60
    var safeMode = false

    init {
        sourceConnectionProperties.let {
            this.sourceConnection = DriverManager.getConnection(it.jdbcUrl, it.username, it.password)
        }
        targetConnectionProperties.let {
            this.targetConnection = DriverManager.getConnection(it.jdbcUrl, it.username, it.password)
        }
    }

    fun migrate(): Map<String, Long> {
        val destinationTableRowsDiff: HashMap<String, Long> = HashMap()

        if (dbObjectTransferList.isEmpty()) {
            log.debug("No tables defined. Won't copy anything!")
            return destinationTableRowsDiff
        }

        try {
            val stmtSource: Statement = this.sourceConnection.createStatement()
            val stmtDestination: Statement = this.targetConnection.createStatement()
            lateinit var bulkCopy: SQLServerBulkCopy

            dbObjectTransferList.forEach {
                setConstraintCheck(stmtDestination, it.table, false)
            }
            try {
                this.targetConnection.autoCommit = false
                for (dbObject in dbObjectTransferList) {
                    val start = System.currentTimeMillis()
                    //TODO REFACTOR HERE IF WE WANT TO ALLOW TRANSFER BETWEEN DIFFERENT SCHEMAS
                    val fullSourceTableName = if (dbObject.schema.isEmpty()) dbObject.table else "${dbObject.schema}.${dbObject.table}"
                    val fullDestinationTableName = if (dbObject.schema.isEmpty()) dbObject.table  else "${dbObject.schema}.${dbObject.table}"

                    getSchema(stmtSource, dbObject)
                    getSchema(stmtDestination, dbObject)

                    val countDestTableBefore = getRowCount(stmtDestination, fullDestinationTableName)

                    //language=TSQL
                    stmtDestination.executeUpdate("DELETE FROM $fullDestinationTableName")

                    log.debug("Inserting new data to destination table: $fullDestinationTableName")

                    //language=TSQL
                    val rsSourceData = stmtSource.executeQuery("SELECT * FROM $fullSourceTableName")

                    bulkCopy = SQLServerBulkCopy(this.targetConnection)
                    val copyOptions = SQLServerBulkCopyOptions()
                    copyOptions.bulkCopyTimeout = this.tableCopyTimeout
                    copyOptions.isKeepIdentity = true
                    copyOptions.batchSize = this.batchSize
                    if (!safeMode) {
                        copyOptions.isUseInternalTransaction
                    }
                    bulkCopy.bulkCopyOptions = copyOptions
                    bulkCopy.destinationTableName = fullDestinationTableName

                    bulkCopy.writeToServer(rsSourceData)

                    val countDestTableAfter = getRowCount(stmtDestination, fullDestinationTableName)
                    destinationTableRowsDiff[dbObject.table] = (countDestTableAfter - countDestTableBefore).toLong()

                    if (!safeMode) {
                        this.targetConnection.commit()
                    }

                    log.debug("Took ${(System.currentTimeMillis() - start)} ms.\n")
                }
                dbObjectTransferList.forEach {
                    setConstraintCheck(stmtDestination, it.table, true)
                }
                if (safeMode) {
                    // Only commit all changes if no exception was thrown during any bulk copy operation!
                    // This insures a valid db state at all times.
                    this.targetConnection.commit()
                }
            } catch (e: SQLServerException) {
                log.error(
                    "Rollback all changes made to the destination database due to an error while bulk copying.",
                    e
                )
                destinationTableRowsDiff.clear()
            } finally {
                for (closable in listOf<AutoCloseable>(
                    this.sourceConnection,
                    this.targetConnection,
                    stmtSource,
                    stmtDestination,
                    bulkCopy
                )) {
                    try {
                        closable.close()
                    } catch (ignore: Exception) {
                    }
                }
            }
        } catch (sqlExc: SQLException) {
            log.error("Could not create the all necessary jdbc connections.", sqlExc)
        }

        return destinationTableRowsDiff
    }

    @Throws(SQLException::class)
    private fun getRowCount(stmt: Statement, tableName: String): Int {
        //language=TSQL
        val rs = stmt.executeQuery("SELECT COUNT(*) FROM $tableName")
        rs.next()
        val count = rs.getInt(1)
        rs.close()
        return count
    }

    private fun getSchema(stmt: Statement, dbObject: DataTransferTableModel) {
        //language=TSQL
        val rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='${dbObject.schema}' AND TABLE_NAME='${dbObject.table}'")
        rs.next()
        //log.debug(TableSchema.from(rs))
        rs.close()
    }

    private fun setConstraintCheck(stmt: Statement, tableName: String, enable: Boolean) {
        val check = if (enable) "WITH CHECK CHECK" else "NOCHECK"
        //language=TSQL
        stmt.execute("ALTER TABLE $tableName $check CONSTRAINT ALL")
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Field(val value: String)
data class TableSchema(
    @Field("TABLE_NAME")
    public var table: String? = "",
    @Field("COLUMN_NAME")
    public var column: String? = "",
    @Field("ORDINAL_POSITION")
    public var ordinal: Int? = -1,
    @Field("DATA_TYPE")
    public var dataType: String? = ""
) {
    companion object {
        fun from(rs: ResultSet) {
            var row = TableSchema()
            for (prop in TableSchema::class.java.declaredFields) {
                prop.getDeclaredAnnotation(Field::class.java)?.let {
                    row.javaClass.getDeclaredField(prop.name).set(row, rs.getString(it.value))
                }
            }
        }
    }
}
