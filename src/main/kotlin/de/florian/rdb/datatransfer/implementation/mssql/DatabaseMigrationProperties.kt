package de.florian.rdb.datatransfer.implementation.mssql

import java.util.*

class DatabaseMigrationProperties {
    lateinit var source: CustomDataSource
    lateinit var target: CustomDataSource
    var tables: List<String> = ArrayList()
    var batchSize = 0

    class CustomDataSource {
        /**
         * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
         */
        lateinit var driverClassName: String

        /**
         * JDBC URL of the database.
         */
        lateinit var url: String

        /**
         * Login username of the database.
         */
        lateinit var username: String

        /**
         * Login password of the database.
         */
        lateinit var password: String
    }
}