package de.florian.rdb.datatransfer.view

import com.github.weisj.darklaf.components.OverlayScrollPane
import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.model.Connection
import de.florian.rdb.datatransfer.view.util.UiUtil
import de.florian.rdb.datatransfer.view.util.UiUtil.Companion.emptyBorder
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class DatasourceSelectionDialog(private val controller: DMController, connectionSelected: (Connection) -> Unit) :
    JDialog() {
    companion object {
        const val NO_CONNECTION_SELECTED_VIEW_NAME = "NO_CONNECTION_SELECTED_VIEW_NAME"
        const val CONNECTION_SELECTED_VIEW_NAME = "CONNECTION_SELECTED_VIEW_NAME"
    }

    private var connectionDetailsPanel: JPanel
    private var selectedConnection: BehaviorSubject<Optional<Connection>> = BehaviorSubject.create()

    private val log = LoggerFactory.getLogger(javaClass)

    private val name = LabeledTextField("Name")
    private val comment = LabeledTextField("Comment")
    private val host = LabeledTextField("Host")
    private val port = LabeledTextField("Port")
    private val user = LabeledTextField("User")
    private val password = LabeledTextField("Password")
    private val database = LabeledTextField("Database")
    private val schema = LabeledTextField("Schcema")
    private val url = LabeledTextField("Url")

    init {
        layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        title = "Datasource Selection"
        modalityType = ModalityType.APPLICATION_MODAL


        val savedConnectionsPanel = JPanel().apply {
            layout = BorderLayout()
        }
        val savedConnectionsModel = DefaultListModel<Connection>()
        val savedConnectionsList = JList(savedConnectionsModel).apply {
            background = Color(70, 72, 74)
            border = emptyBorder()
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
        val connectionActionRow = JPanel().apply { layout = GridLayout(1, 2) }
        val addConectionBtn = JButton("\u2795 Add").apply {
            addActionListener {
                //TODO ACCESS REAL MODEL
                val con = Connection(
                    "localhost",
                    1433,
                    "SA",
                    "PW",
                    "db",
                    "jdbc://localhost:1433?database=db",
                    "SQLDRIVER",
                    "testname1",
                    "testcomment"
                )
                savedConnectionsModel.addElement(con)
            }
        }
        val removeConectionBtn = JButton("\u274C Remove").apply {
            addActionListener {
                //TODO ACCESS REAL MODEL
                val index = savedConnectionsList.selectedIndex
                if (index > 0) {
                    savedConnectionsModel.remove(index)
                } else {
                    log.debug("No connection for deletion selected!")
                }
            }
        }
        connectionActionRow.add(addConectionBtn)
        connectionActionRow.add(removeConectionBtn)

        val content = JPanel().apply {
            layout = BorderLayout()
        }

        val connectionFormComponent: JComponent = JPanel().apply {
            layout = GridBagLayout();
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Connection parameters"),
                BorderFactory.createEmptyBorder(
                    UiUtil.STANDARD_BORDER,
                    UiUtil.STANDARD_BORDER,
                    UiUtil.STANDARD_BORDER,
                    UiUtil.STANDARD_BORDER
                )
            )
        }

        val noConnectionSelectedPanel = JPanel().apply {
            add(JLabel("SELECT A DATASOURCE"))
        }

        connectionDetailsPanel = JPanel().apply {
            layout = CardLayout()

            add(NO_CONNECTION_SELECTED_VIEW_NAME, noConnectionSelectedPanel)
            add(CONNECTION_SELECTED_VIEW_NAME, connectionFormComponent)
        }

        val inputs = listOf(
            name to Pair(0, 0),
            comment to Pair(1, 0),
            host to Pair(2, 0),
            port to Pair(2, 1),
            user to Pair(3, 0),
            password to Pair(4, 0),
            database to Pair(5, 0),
            schema to Pair(6, 0),
            url to Pair(7, 0)
        )
        for (row in inputs) {
            val y = row.second.first
            val x = row.second.second
            connectionFormComponent.add(row.first.label, UiUtil.createGbc(y, 2 * x))
            connectionFormComponent.add(row.first.input, UiUtil.createGbc(y, 1 + 2 * x))
        }

        val selectBtn = JButton("Select").apply {
            setBounds(10, 10, 40, 40)
            addActionListener {
                synchronized(savedConnectionsList) {
                    if (!savedConnectionsList.isSelectionEmpty) {
                        val connection = savedConnectionsModel.get(savedConnectionsList.selectedIndex)
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

        savedConnectionsPanel.add(connectionActionRow, BorderLayout.SOUTH)
        savedConnectionsPanel.add(savedConnectionsListScrollPane, BorderLayout.CENTER)
        content.add(btnRowPanel, BorderLayout.SOUTH)
        content.add(connectionDetailsPanel, BorderLayout.CENTER)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, savedConnectionsPanel, content)
        add(splitPane)

        selectedConnection.subscribe {
            log.debug("Connection selected for edit.")
            //TODO CHANGE ALL FIELDS

            showConnectionForm(it.isPresent)
        }

        //TODO GENERALISE!
        fun updateName() {
            GlobalScope.launch(Dispatchers.Main) {
                selectedConnection.value?.ifPresent {
                    it.name = name.input.text
                    savedConnectionsList.repaint()
                }
            }
        }
        name.input.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                updateName()
            }

            override fun insertUpdate(e: DocumentEvent?) {
                updateName()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                updateName()
            }
        })

        this.pack()
        this.isLocationByPlatform = true
        isVisible = true
    }

    private fun showConnectionForm(value: Boolean) {
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

class LabeledTextField(label: String) : JPanel() {
    val label = JLabel("$label:")
    val input = JTextField()

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(this.label)
        add(this.input)
    }
}


