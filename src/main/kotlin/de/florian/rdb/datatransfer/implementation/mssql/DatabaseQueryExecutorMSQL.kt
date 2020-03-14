package de.florian.rdb.datatransfer.implementation.mssql

import de.florian.rdb.datatransfer.implementation.DatabaseQueryExecutor
import de.florian.rdb.datatransfer.model.Column
import de.florian.rdb.datatransfer.model.DB
import de.florian.rdb.datatransfer.model.Schema
import de.florian.rdb.datatransfer.model.Table
import java.sql.Connection


class DatabaseQueryExecutorMSQL : DatabaseQueryExecutor() {
    enum class COL {
        TABLE_CATALOG,
        TABLE_SCHEMA ,
        TABLE_NAME,
        COLUMN_NAME,
        DATA_TYPE,
        TOTAL_ROW_COUNT,
        ORDINAL_POSITION
    }

    override suspend fun getDatabaseInformation(connection: Connection): DB {
        val dbName = connection.catalog
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery(
            """
            SELECT A.TABLE_CATALOG, A.TABLE_SCHEMA, A.TABLE_NAME, A.COLUMN_NAME, A.DATA_TYPE, B.TOTAL_ROW_COUNT
            FROM (SELECT * FROM $dbName.INFORMATION_SCHEMA.COLUMNS) A,
                 (SELECT SCHEMA_NAME(schema_id)   AS TABLE_SCHEMA,
                         [Tables].name            AS TABLE_NAME,
                         SUM([Partitions].[rows]) AS [TOTAL_ROW_COUNT]
                  FROM sys.tables AS [Tables]
                           JOIN sys.partitions AS [Partitions] ON [Tables].[object_id] = [Partitions].[object_id]
                      AND [Partitions].index_id IN (0, 1)
                  GROUP BY SCHEMA_NAME(schema_id), [Tables].name) B
            WHERE A.TABLE_SCHEMA = B.TABLE_SCHEMA
              AND A.TABLE_NAME = B.TABLE_NAME
            ORDER BY A.TABLE_CATALOG, A.TABLE_SCHEMA, A.TABLE_NAME, A.ORDINAL_POSITION;
            """.trimIndent()
        )

        data class SchemaExtraction(
            val schema : String = "",
            val tableName : String = "",
            val columnName : String = "",
            val dataType : String = "",
            val tableRowCount : Long = 0
        )
        val extracted = mutableListOf<SchemaExtraction>()
        while (rs.next()) {
            extracted.add(
                SchemaExtraction(
                    schema = rs.getString(COL.TABLE_SCHEMA.name),
                    tableName = rs.getString(COL.TABLE_NAME.name),
                    columnName = rs.getString(COL.COLUMN_NAME.name),
                    dataType = rs.getString(COL.DATA_TYPE.name),
                    tableRowCount = rs.getLong(COL.TOTAL_ROW_COUNT.name)
                )
            )
        }

        val nestedDb = extracted.groupBy { it.schema }.map {
            it.key to it.value.groupBy { it.tableName }
        }
        val db = DB(dbName, mutableListOf())
        for (schema in nestedDb){
            val schemaName = schema.first
            val s = Schema(schemaName, mutableListOf())
            for(table in schema.second){
                val tableName = table.key
                val t = Table(tableName, mutableListOf(), table.value.first().tableRowCount)
                for (column in table.value){
                    t.columns.add(Column(column.columnName, column.dataType))
                }
                s.tables.add(t)
            }
            db.schemas.add(s)
        }

        return db
    }
}