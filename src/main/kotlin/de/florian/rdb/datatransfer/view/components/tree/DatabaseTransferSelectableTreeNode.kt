package de.florian.rdb.datatransfer.view.components.tree

import com.github.weisj.darklaf.components.SelectableTreeNode
import de.florian.rdb.datatransfer.model.DataTransferTableModel

class DatabaseTransferSelectableTreeNode(
    label: String,
    isSelected: Boolean,
    val associatedObject : DataTransferTableModel? = null,
    val includeInSelectionResult: Boolean = false
) : SelectableTreeNode(label, isSelected, true) {
    override fun getUserObject(): Boolean {
        return super.getUserObject() as Boolean
    }
}
