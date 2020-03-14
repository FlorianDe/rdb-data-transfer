package de.florian.rdb.datatransfer.view.datasource.selection

import com.github.weisj.darklaf.components.OverlayScrollPane
import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.extensions.getValueOptional
import de.florian.rdb.datatransfer.model.Connection
import de.florian.rdb.datatransfer.view.util.UiUtil
import io.reactivex.subjects.BehaviorSubject
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import java.util.*
import javax.swing.*

class DatasourceSelectionMasterView(
    controller: DMController,
    selectedConnection: BehaviorSubject<Optional<Connection>>
) : JPanel() {
    private val log = LoggerFactory.getLogger(javaClass)

    var savedConnectionsList: JList<Connection>
    var savedConnectionsModel: DefaultListModel<Connection>

    init {
        layout = BorderLayout()

        savedConnectionsModel = DefaultListModel<Connection>()
        savedConnectionsList = JList(savedConnectionsModel).apply {
            background = Color(70, 72, 74)
            border = UiUtil.emptyBorder(2)
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        savedConnectionsList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val lsm = e.source as JList<*>
                if (lsm.isSelectionEmpty) {
                    selectedConnection.onNext(Optional.empty())
                } else {
                    synchronized(savedConnectionsList) {
                        val connection = savedConnectionsModel.get(savedConnectionsList.selectedIndex)
                        selectedConnection.onNext(Optional.of(connection))
                    }
                }
            }
        }
        val savedConnectionsListScrollPane = OverlayScrollPane(savedConnectionsList)

        val saveAllConectionsBtn = JButton("\uD83D\uDCBE Save all").apply {
            addActionListener {
                controller.saveConnections()
            }
        }
        val refreshConectionsBtn = JButton("\u27F2 Refresh").apply {
            addActionListener {
                controller.loadConnections()
            }
        }
        val connectionASaveAllRefreshRow = JPanel().apply {
            layout = GridLayout(1, 2)
            add(saveAllConectionsBtn)
            add(refreshConectionsBtn)
        }

        val addConectionBtn = JButton("\u2795 Add").apply {
            addActionListener {
                controller.addConnection()
            }
        }
        val copyConectionBtn = JButton("⎘ Copy").apply {
            addActionListener {
                selectedConnection.getValueOptional().ifPresent {
                    controller.addConnection(it.copy(name = "${it.name}-copy"))
                }
            }
            selectedConnection.subscribe {
                this.isEnabled = selectedConnection.getValueOptional().isPresent
            }
        }
        val removeConectionBtn = JButton("\u274C Remove").apply {
            addActionListener {
                val index = savedConnectionsList.selectedIndex
                if (index != -1) {
                    controller.removeConnection(savedConnectionsModel[index])
                } else {
                    log.debug("No connection for deletion selected!")
                }
            }
        }
        val connectionAddRemoveRow = JPanel().apply {
            layout = GridLayout(1, 3)
            add(addConectionBtn)
            add(copyConectionBtn)
            add(removeConectionBtn)
        }

        add(connectionASaveAllRefreshRow, BorderLayout.NORTH)
        add(connectionAddRemoveRow, BorderLayout.SOUTH)
        add(savedConnectionsListScrollPane, BorderLayout.CENTER)

        controller.model.storedConnections.subscribe {
            //TODO MAYBE EVENT DRIVEN?
            savedConnectionsModel.clear()
            // Add elements one by one, since addAll is only available in > Java 11
            it?.let { cons -> cons.forEach { con -> savedConnectionsModel.addElement(con) } }
            selectedConnection.onNext(Optional.empty())
        }
    }
}