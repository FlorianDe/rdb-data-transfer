package de.florian.rdb.datatransfer.view.datasource.selection

import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.model.Connection
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.*
import javax.swing.*


class DatasourceSelectionDialog(private val controller: DMController, connectionSelected: (Connection) -> Unit) :
    JDialog() {
    companion object {
        const val NO_CONNECTION_SELECTED_VIEW_NAME = "NO_CONNECTION_SELECTED_VIEW_NAME"
        const val CONNECTION_SELECTED_VIEW_NAME = "CONNECTION_SELECTED_VIEW_NAME"
    }

    private var connectionDetailsPanel: JPanel
    private var selectedConnection: BehaviorSubject<Optional<Connection>> = BehaviorSubject.create()

    private val log = LoggerFactory.getLogger(javaClass)


    init {
        layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        title = "Datasource Selection"
        modalityType = ModalityType.APPLICATION_MODAL

        val content = JPanel().apply {
            layout = BorderLayout()
        }

        val connectionsMasterView = DatasourceSelectionMasterView(controller, selectedConnection)

        val connectionDetailsView: JComponent =
            DatasourceSelectionDetailView(
                controller,
                connectionsMasterView,
                selectedConnection
            )

        val noConnectionSelectedPanel = JPanel().apply {
            add(JLabel("SELECT A DATASOURCE"))
        }

        connectionDetailsPanel = JPanel().apply {
            layout = CardLayout()

            add(NO_CONNECTION_SELECTED_VIEW_NAME, noConnectionSelectedPanel)
            add(CONNECTION_SELECTED_VIEW_NAME, connectionDetailsView)
        }

        val selectBtn = JButton("Select").apply {
            setBounds(10, 10, 40, 40)
            addActionListener {
                synchronized(connectionsMasterView.savedConnectionsList) {
                    if (!connectionsMasterView.savedConnectionsList.isSelectionEmpty) {
                        val connection =
                            connectionsMasterView.savedConnectionsModel.get(connectionsMasterView.savedConnectionsList.selectedIndex)
                        connectionSelected(connection)
                    }
                }
                dispose()
            }
        }

        val cancelBtn = JButton("Cancel").apply {
            setBounds(10, 10, 40, 40)
            addActionListener {
                dispose()
            }
        }

        val btnRowPanel = JPanel().apply {
            layout = FlowLayout()

            add(selectBtn)
            add(cancelBtn)
        }

        content.add(btnRowPanel, BorderLayout.SOUTH)
        content.add(connectionDetailsPanel, BorderLayout.CENTER)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, connectionsMasterView, content)
        add(splitPane)

        selectedConnection.subscribe {
            log.debug("Connection $it selected for edit.")
            showConnectionForm(it.isPresent)
        }

        this.pack()
        this.isLocationByPlatform = true
        isVisible = true
    }

    private fun showConnectionForm(value: Boolean = true) {
        GlobalScope.launch(Dispatchers.Main) {
            val connectionDetailsPanelCardLayout = connectionDetailsPanel.layout as CardLayout
            connectionDetailsPanelCardLayout.show(
                connectionDetailsPanel,
                if (value) CONNECTION_SELECTED_VIEW_NAME else NO_CONNECTION_SELECTED_VIEW_NAME
            )
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(800, 600)
    }
}


