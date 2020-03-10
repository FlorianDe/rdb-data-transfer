package de.florian.rdb.datatransfer.view.tree

import com.github.weisj.darklaf.components.SelectableTreeNode

class CstmSelectableTreeNode : SelectableTreeNode {
    constructor() : super()
    constructor(label: String?, isSelected: Boolean) : super(label, isSelected, true)
    constructor(label: String?, isSelected: Boolean, allowsChildren: Boolean) : super(
        label,
        isSelected,
        allowsChildren
    )

    override fun getUserObject(): Boolean {
        return super.getUserObject() as Boolean
    }
}
