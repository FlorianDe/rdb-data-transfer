package de.florian.rdb.datatransfer.view.util

import java.awt.Container
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JLabel

class UiUtil private constructor() {
    companion object {
        val ONE_SPACE = getSpace(1)
        val TWO_SPACES = getSpace(2)
        val THREE_SPACES = getSpace(3)
        val STANDARD_BORDER = TWO_SPACES
        val WEST_INSETS =
            Insets(
                ONE_SPACE, 0,
                ONE_SPACE,
                ONE_SPACE
            )
        val EAST_INSETS =
            Insets(
                ONE_SPACE,
                ONE_SPACE,
                ONE_SPACE, 0
            )
        const val SIMPLE_FIELD_WIDTH = 20
        const val MAX_LABEL_LENGTH = 35
        fun getSpace(col: Int): Int {
            return 6 * col - 1
        }

        fun compoundNamedBorder(title: String, margin: Int = STANDARD_BORDER) = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(title),
            emptyBorder(margin)
        )

        fun emptyBorder(margin: Int = STANDARD_BORDER) = BorderFactory.createEmptyBorder(margin, margin, margin, margin)

        fun createGbc(y: Int, x: Int): GridBagConstraints {
            val gbc = GridBagConstraints()
            gbc.gridx = x
            gbc.gridy = y
            gbc.anchor = if (x == 0) GridBagConstraints.WEST else GridBagConstraints.EAST
            gbc.fill = if (x == 0) GridBagConstraints.BOTH else GridBagConstraints.HORIZONTAL
            gbc.insets = if (x == 0) WEST_INSETS else EAST_INSETS
            gbc.weightx = if (x == 0) 0.1 else 1.0
            //gbc.weighty = 1.0;
            return gbc
        }

        fun addExpander(container: Container, row: Int) {
            val glueConstraints = GridBagConstraints()
            glueConstraints.gridy = row
            glueConstraints.weighty = 1.0
            glueConstraints.fill = GridBagConstraints.VERTICAL
            container.add(JLabel(), glueConstraints)
        }
    }

    init {
        throw AssertionError()
    }
}