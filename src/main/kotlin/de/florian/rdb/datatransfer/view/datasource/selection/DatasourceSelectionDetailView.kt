package de.florian.rdb.datatransfer.view.datasource.selection

import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.extensions.getValueOptional
import de.florian.rdb.datatransfer.model.Connection
import de.florian.rdb.datatransfer.view.components.textfield.IntOnlyDocumentFilter
import de.florian.rdb.datatransfer.view.util.UiUtil
import de.florian.rdb.datatransfer.view.util.UiUtil.Companion.compoundNamedBorder
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.PlainDocument
import kotlin.reflect.KMutableProperty1


class DatasourceSelectionDetailView(
    controller: DMController,
    masterView: JComponent,
    selectedConnection: BehaviorSubject<Optional<Connection>>
) : JPanel() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val name = LabeledTextField(
        "Name",
        masterView,
        selectedConnection,
        Connection::name
    ) { it, value -> it.name = value }

    private val comment = LabeledTextField(
        "Comment",
        masterView,
        selectedConnection,
        Connection::comment
    ) { it, value -> it.comment = value }

    private val host = LabeledTextField(
        "Host",
        masterView,
        selectedConnection,
        Connection::host
    ) { it, value -> it.host = value }

    private val port = LabeledTextField(
        "Port",
        masterView,
        selectedConnection,
        Connection::port,
        JTextField().apply {
            (this.document as PlainDocument).documentFilter = IntOnlyDocumentFilter()
        }
    ) { it, value -> it.port = Integer.valueOf(value) }

    private val user = LabeledTextField(
        "User",
        masterView,
        selectedConnection,
        Connection::username
    ) { it, value -> it.username = value }

    private val password = LabeledTextField(
        "Password",
        masterView,
        selectedConnection,
        Connection::password,
        JPasswordField().apply { putClientProperty("JPasswordField.showViewIcon", true) }
    ) { it, value -> it.password = value }

    private val database = LabeledTextField(
        "Database",
        masterView,
        selectedConnection,
        Connection::database
    ) { it, value -> it.database = value }

    //private val schema = LabeledTextField("Schema", masterView, selectedConnection) //{ it, value -> it. = value }
    private val url = LabeledTextField(
        "Url",
        masterView,
        selectedConnection,
        Connection::jdbcUrl
    ) { it, value -> it.jdbcUrl = value }

    init {
        layout = BorderLayout()
        val container = JPanel().apply {
            layout = GridBagLayout()
            border = compoundNamedBorder("Connection parameters")
        }

        val inputs = listOf(
            name to Pair(0, 0),
            comment to Pair(1, 0),
            host to Pair(2, 0),
            port to Pair(2, 1),
            user to Pair(3, 0),
            password to Pair(4, 0),
            database to Pair(5, 0),
            url to Pair(7, 0)
            //schema to Pair(6, 0)
        )
        for (elem in inputs) {
            val y = elem.second.first
            val x = elem.second.second
            container.add(elem.first.label, UiUtil.createGbc(y, 2 * x))
            container.add(elem.first.input, UiUtil.createGbc(y, 1 + 2 * x))
        }

        selectedConnection.subscribe {
            it.ifPresent { con ->
                for (elem in inputs) {
                    elem.first.input.text = elem.first.connectionProperty.get(con).toString()
                }
            }
        }

        add(container, BorderLayout.NORTH)
    }
}

class LabeledTextField(
    label: String,
    masterView: JComponent,
    selectedConnection: BehaviorSubject<Optional<Connection>>,
    val connectionProperty: KMutableProperty1<Connection, *>,
    valueField: JTextField = JTextField(),
    valueChanged: (Connection, String) -> Unit
) : JPanel() {
    val label = JLabel("$label:")
    val input = valueField

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(this.label)
        add(this.input)

        fun updateValue() {
            selectedConnection.getValueOptional().ifPresent {
                GlobalScope.launch(Dispatchers.Main) {
                    valueChanged(it, input.text)
                    masterView.repaint()
                }
            }
        }
        input.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                updateValue()
            }

            override fun insertUpdate(e: DocumentEvent?) {
                updateValue()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                updateValue()
            }
        })
    }
}