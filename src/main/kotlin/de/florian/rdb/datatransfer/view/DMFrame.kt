package de.florian.rdb.datatransfer.view

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.implementation.mssql.DatabaseMigrationService
import de.florian.rdb.datatransfer.view.transfer.TablesInformationTreePanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Image
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


class DMFrame(private val controller: DMController) : JFrame() {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        //System.setProperty("sun.java2d.noddraw", "true")
        //System.setProperty("sun.java2d.d3d", "false")
        //System.setProperty("sun.java2d.opengl", "true")
        //setDefaultLookAndFeelDecorated(false)
        LafManager.setDecorationsEnabled(false)
        LafManager.install(DarculaTheme())
        prepareGUI()
    }

    private fun prepareGUI() {
        var iconImg: Optional<Image> = Optional.empty()
        try {
            iconImg = Optional.ofNullable(
                ImageIO.read(
                    Objects.requireNonNull(
                        javaClass.classLoader.getResource("img/rdb_transfer_icon_256.png")
                    )
                )
            )
        } catch (e: IOException) {
            log.error("The tray icon image couldn't be loaded.", e)
        } catch (e: NullPointerException) {
            log.error("The tray icon image couldn't be loaded.", e)
        }

        title = "Rdb-data-transfer"
        iconImg.ifPresent {
            iconImage = it
        }

        val headerPanel = JPanel().apply {
            layout = BorderLayout()

            val sourcePanel = ConnectionSelectionPanel(
                controller,
                "Source",
                controller.model.sourceConnectionProperties
            )
            val targetPanel = ConnectionSelectionPanel(
                controller,
                "Target",
                controller.model.targetConnectionProperties
            )

            add(sourcePanel, BorderLayout.WEST)
            add(targetPanel, BorderLayout.EAST)
        }

        val sourceTablesInformationPanel = TablesInformationTreePanel(controller, controller.model.sourceDatabase)
        //val targetTablesInformationPanel = TablesInformationTreePanel(controller, controller.model.targetDatabase)

        val transferBtn = JButton("Transfer").apply {
            fun validTransfer(): Boolean {
                return controller.model.sourceConnectionProperties.value?.isPresent == true &&
                        controller.model.targetConnectionProperties.value?.isPresent == true
            }
            addActionListener {
                val selectedTables = sourceTablesInformationPanel.getIncludedTables()
                val selectedTablesString = selectedTables.joinToString(",\n") { "[${it.db}].[${it.schema}].${it.table}" }
                val sourceOpt = controller.getSourceConnection()
                val targetOpt = controller.getTargetConnection()

                log.debug(
                    """
                    selectedTables=$selectedTablesString
                    source=$sourceOpt
                    target=$targetOpt
                """.trimMargin()
                )

                if (sourceOpt.isPresent && targetOpt.isPresent) {
                    val source = sourceOpt.get().copy()
                    val target = targetOpt.get().copy()
                    val message = """
                        Do you really want to transfer tables:
                        $selectedTablesString
                        from: $source
                        to: $target
                    """.trimIndent()

                    val result = JOptionPane.showConfirmDialog(
                        null, message,
                        "Database tables transfer", JOptionPane.OK_CANCEL_OPTION
                    )

                    if (result == JOptionPane.YES_OPTION) {
                        val totalTime = measureTimeMillis {
                            val migration = DatabaseMigrationService(
                                sourceConnectionProperties = source,
                                targetConnectionProperties = target,
                                dbObjectTransferList = selectedTables
                            ).migrate()
                            log.debug("Migrated:\n$migration")
                        }
                        log.debug("Total transfer took $totalTime ms.")
                    }
                }
            }

            isEnabled = validTransfer()

            controller.model.sourceConnectionProperties
                .mergeWith(controller.model.targetConnectionProperties)
                .subscribe {
                    val enable = validTransfer()
                    GlobalScope.launch(Dispatchers.Main) {
                        isEnabled = enable
                        println("Transferbtn enabled: $isEnabled")
                    }
                }
        }

        val bottomPanel = JPanel().apply {
            add(transferBtn)
        }

        layout = BorderLayout()
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent?) {
                exitProcess(0)
            }
        })
        add(headerPanel, BorderLayout.NORTH)
        add(bottomPanel, BorderLayout.SOUTH)
        add(sourceTablesInformationPanel, BorderLayout.CENTER)
        //add(targetTablesInformationPanel, BorderLayout.EAST)

        pack()
        isLocationByPlatform = true
        isVisible = true
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(400, 600)
    }

    override fun getMinimumSize(): Dimension {
        return Dimension(300, 500)
    }
}

