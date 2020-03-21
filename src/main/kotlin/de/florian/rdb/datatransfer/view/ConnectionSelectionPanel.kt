package de.florian.rdb.datatransfer.view

import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.model.DBConnectionProperties
import de.florian.rdb.datatransfer.view.datasource.selection.DatasourceSelectionDialog
import de.florian.rdb.datatransfer.view.util.UiUtil.Companion.compoundNamedBorder
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class ConnectionSelectionPanel(
    private val controller: DMController,
    groupName: String,
    private val connectionProperties: BehaviorSubject<Optional<DBConnectionProperties>>
) : JPanel() {
    companion object {
        private const val NO_DATASOURCE_SELECTED_TEXT = "Choose a database"
    }

    private var datasource: JLabel
    private var selectBtn: JButton

    init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = compoundNamedBorder(groupName)

        datasource = JLabel(NO_DATASOURCE_SELECTED_TEXT)
        selectBtn = JButton("Select connection...")
        selectBtn.addActionListener {
            DatasourceSelectionDialog(
                controller
            ) {
                this.connectionProperties.onNext(
                    Optional.of(it)
                )
            }
        }
        add(datasource)
        add(selectBtn)

        connectionProperties.subscribe {
            val connectionLabelTxt = when (it.isPresent) {
                true -> "${it.get()}"
                else -> NO_DATASOURCE_SELECTED_TEXT
            }

            GlobalScope.launch(Dispatchers.Main) {
                datasource.text = connectionLabelTxt
            }
        }
    }
}