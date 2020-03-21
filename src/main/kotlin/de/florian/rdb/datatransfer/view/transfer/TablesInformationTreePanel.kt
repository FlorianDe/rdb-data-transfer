package de.florian.rdb.datatransfer.view.transfer

import com.github.weisj.darklaf.components.OverlayScrollPane
import com.github.weisj.darklaf.components.SelectableTreeNode
import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.model.DB
import de.florian.rdb.datatransfer.model.DataTransferTableModel
import de.florian.rdb.datatransfer.view.components.tree.DatabaseTransferSelectableTreeNode
import io.reactivex.subjects.BehaviorSubject
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreeSelectionModel

class TablesInformationTreePanel(
    val controller: DMController,
    database: BehaviorSubject<Optional<DB>>
) : JPanel() {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val NO_DB_SELECTED_ROOT_NODE = DefaultMutableTreeNode("NO DB SELECTED")
    }

    private val root = NO_DB_SELECTED_ROOT_NODE
    private val treemodel = DefaultTreeModel(root)
    init{
        database.subscribe {
            if (it.isPresent) {
                val db = it.get()
                val rootNode = DefaultMutableTreeNode(db.name)
                for (schema in db.schemas) {
                    val schemaNode = DatabaseTransferSelectableTreeNode(schema.name, false)
                    for (table in schema.tables) {
                        val tableNode = DatabaseTransferSelectableTreeNode(
                            label = "${table.name} [${table.records}]",
                            isSelected = false,
                            associatedObject = DataTransferTableModel(
                                db = db.name,
                                schema = schema.name,
                                table = table.name
                            ),
                            includeInSelectionResult = true
                        )
                        for (column in table.columns) {
                            tableNode.add(DefaultMutableTreeNode("${column.name}  -  ${column.type}", false))
                        }
                        schemaNode.add(tableNode)
                    }
                    rootNode.add(schemaNode)
                }
                treemodel.setRoot(rootNode)
            } else {
                treemodel.setRoot(NO_DB_SELECTED_ROOT_NODE)
            }
        }
        val tree = JTree(treemodel).apply {
            background = Color(62, 67, 76)
            putClientProperty("JTree.lineStyle", "Dashed")
            //putClientProperty("JTree.alternateRowColor", true)
        }
        tree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION

        tree.addTreeSelectionListener {
            val path = it.newLeadSelectionPath
            if (path != null) {
                log.debug(path.lastPathComponent.toString())
                val selected = path.lastPathComponent
                if (selected is SelectableTreeNode) {
                    val oldSelectionValue = selected.userObject as Boolean
                    selected.userObject = !oldSelectionValue
                    setSelectionChildrenRecursive(selected, !oldSelectionValue)
                    setSelectionParentRecursive(selected, !oldSelectionValue)
                }
                tree.clearSelection()
            }
        }

        val tablesTreeScrollPane = OverlayScrollPane(tree)

        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        add(tablesTreeScrollPane)
    }

    fun getSelectedNodes(parent: DefaultMutableTreeNode): List<SelectableTreeNode> {
        return parent.depthFirstEnumeration()
            .asSequence()
            .mapNotNull { if (it is SelectableTreeNode) it else null }
            .filter { it.userObject == true}
            .toList()
    }

    fun getIncludedTables() : List<DataTransferTableModel>{
        return getSelectedNodes(treemodel.root as DefaultMutableTreeNode)
            .mapNotNull { if (it is DatabaseTransferSelectableTreeNode) it else null }
            .filter { it.includeInSelectionResult }
            .mapNotNull { it.associatedObject }
    }

    private fun setSelectionParentRecursive(selected: TreeNode, value: Boolean) {
        val parentNode = selected.parent
        if (parentNode != null && parentNode is SelectableTreeNode) {
            val parentValue = parentNode.children().asSequence().all {
                it is SelectableTreeNode && it.userObject as Boolean
            }
            parentNode.userObject = parentValue
            setSelectionParentRecursive(parentNode, !value)
        }
    }

    private fun setSelectionChildrenRecursive(node: TreeNode, value: Boolean) {
        for (child in node.children()) {
            if (child is SelectableTreeNode) {
                child.userObject = value
                setSelectionChildrenRecursive(child, value)
            }
        }
    }
}