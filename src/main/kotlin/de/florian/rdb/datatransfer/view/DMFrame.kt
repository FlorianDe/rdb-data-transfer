package de.florian.rdb.datatransfer.view

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.components.OverlayScrollPane
import com.github.weisj.darklaf.components.SelectableTreeNode
import com.github.weisj.darklaf.theme.DarculaTheme
import de.florian.rdb.datatransfer.controller.DMController
import de.florian.rdb.datatransfer.model.Schema
import de.florian.rdb.datatransfer.model.Table
import de.florian.rdb.datatransfer.view.components.tree.CstmSelectableTreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreeSelectionModel
import kotlin.system.exitProcess


class DMFrame(private val controller: DMController) : JFrame() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val NO_DB_SELECTED_ROOT_NODE = DefaultMutableTreeNode("NO DB SELECTED")

    init {
        //System.setProperty("sun.java2d.noddraw", "true")
        //System.setProperty("sun.java2d.d3d", "false")
        //System.setProperty("sun.java2d.opengl", "true")
        //setDefaultLookAndFeelDecorated(false)
        //LafManager.setDecorationsEnabled(false)
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
                controller.model.sourceConnection
            )
            val targetPanel = ConnectionSelectionPanel(
                controller,
                "Target",
                controller.model.targetConnection
            )

            add(sourcePanel, BorderLayout.WEST)
            add(targetPanel, BorderLayout.EAST)
        }

        val root = NO_DB_SELECTED_ROOT_NODE
        val treemodel = DefaultTreeModel(root)
        this.controller.model.database.subscribe{
            if(it.isPresent){
                val db = it.get()
                val rootNode = DefaultMutableTreeNode(db.name)
                for (schema in db.schemas){
                    val schemaNode = CstmSelectableTreeNode(schema.name, false)
                    for(table in schema.tables){
                        val tableNode = CstmSelectableTreeNode("${table.name} [${table.records}]", false)
                        for (column in table.columns){
                            tableNode.add(CstmSelectableTreeNode("${column.name}  -  ${column.type}", false))
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
            background = Color(70, 72, 74)
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
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            add(tablesTreeScrollPane)
        }

        val transferBtn = JButton("Transfer").apply {
            fun validTransfer(): Boolean {
                return controller.model.sourceConnection.value?.isPresent == true &&
                        controller.model.targetConnection.value?.isPresent == true
            }
            addActionListener {
                val selectedTables =
                    getSelectedNodes(treemodel.root as DefaultMutableTreeNode).joinToString(",") { "${it.label} -> ${it.userObject} : ${it.path?.contentToString()}" }
                val sourceOpt = controller.getSourceConnection()
                val targetOpt = controller.getTargetConnection()

                log.debug(
                    """
                    selectedTables=$selectedTables
                    source=$sourceOpt
                    target=$targetOpt
                """.trimMargin()
                )

                if (sourceOpt.isPresent && targetOpt.isPresent) {
                    val source = sourceOpt.get().copy()
                    val target = targetOpt.get().copy()
                    val message = """
                        Do you really want to transfer tables:
                        $selectedTables
                        from: $source
                        to: $target
                    """.trimIndent()

                    val result = JOptionPane.showConfirmDialog(
                        null, message,
                        "Database tables transfer", JOptionPane.OK_CANCEL_OPTION
                    )

                    if (result == JOptionPane.YES_OPTION) {
                        // TODO START BUSINESS LOGIC + PROCESS
                        log.debug("TRANSFER CONFIRMED")
                    }
                }
            }

            isEnabled = validTransfer()

            controller.model.sourceConnection
                .mergeWith(controller.model.targetConnection)
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
        add(centerPanel, BorderLayout.CENTER)

        setLocationRelativeTo(null)
        pack()
        isVisible = true
    }

    private fun getSelectedNodes(parent: DefaultMutableTreeNode): List<SelectableTreeNode> {
        return parent.depthFirstEnumeration()
            .asSequence()
            .mapNotNull { if (it is SelectableTreeNode) it else null }
            .filter { it.childCount == 0 }
            .filter { it.userObject == true }
            .toList()
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

    override fun getPreferredSize(): Dimension {
        return Dimension(400, 600)
    }

    override fun getMinimumSize(): Dimension {
        return Dimension(300, 500)
    }
}

