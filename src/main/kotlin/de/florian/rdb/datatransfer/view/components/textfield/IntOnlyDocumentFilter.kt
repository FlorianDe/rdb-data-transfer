package de.florian.rdb.datatransfer.view.components.textfield

import de.florian.rdb.datatransfer.extensions.isInt
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.Document

import javax.swing.text.DocumentFilter


internal class IntOnlyDocumentFilter : DocumentFilter() {
    @Throws(BadLocationException::class)
    override fun insertString(
        fb: FilterBypass, offset: Int, string: String?,
        attr: AttributeSet?
    ) {
        val doc: Document = fb.document
        val execute = StringBuilder()
            .append(doc.getText(0, doc.length))
            .insert(offset, string)
            .isInt()

        if (execute) {
            super.insertString(fb, offset, string, attr)
        }
    }

    @Throws(BadLocationException::class)
    override fun replace(
        fb: FilterBypass, offset: Int, length: Int, text: String?,
        attrs: AttributeSet?
    ) {
        val doc: Document = fb.document
        val execute = StringBuilder()
            .append(doc.getText(0, doc.length))
            .replace(offset, offset + length, text)
            .isInt()

        if (execute) {
            super.replace(fb, offset, length, text, attrs)
        }
    }

    @Throws(BadLocationException::class)
    override fun remove(fb: FilterBypass, offset: Int, length: Int) {
        val doc: Document = fb.document
        val execute = StringBuilder()
            .append(doc.getText(0, doc.length))
            .delete(offset, offset + length)
            .isInt()

        if (execute) {
            super.remove(fb, offset, length)
        }
    }
}