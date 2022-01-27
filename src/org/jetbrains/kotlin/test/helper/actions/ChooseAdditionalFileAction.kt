package org.jetbrains.kotlin.test.helper.actions

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.io.UniqueNameBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import org.jetbrains.kotlin.test.helper.state.PreviewEditorState
import org.jetbrains.kotlin.test.helper.ui.TestDataEditor
import java.awt.Component
import java.io.File
import javax.swing.*

class ChooseAdditionalFileAction(
    private val testDataEditor: TestDataEditor,
    private val previewEditorState: PreviewEditorState
) : ComboBoxAction() {
    companion object {
        private const val NO_NAME_PROVIDED = "## no name provided ##"
    }

    private val files: Sequence<VirtualFile>
        get() = previewEditorState.previewEditors.asSequence().mapNotNull { it.file }

    /**
     * If two or more files have the same name, we want to display the parts of their full paths that differ.
     * This is the same thing that IDEA does for tab titles when two files with the same names are opened.
     */
    private var uniqueNameBuilder = createUniqueNameBuilder()

    private lateinit var model: DefaultComboBoxModel<FileEditor>
    private lateinit var box: ComboBox<FileEditor>

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        return DefaultActionGroup()
    }

    private fun ComboBox<*>.updateBoxWidth() {
        val fileNameWithMaxLength = previewEditorState.previewEditors
            .map { it.presentableName }
            .maxByOrNull { it.length }
            ?: NO_NAME_PROVIDED
        setMinimumAndPreferredWidth(getFontMetrics(font).stringWidth(fileNameWithMaxLength) + 80)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        model = DefaultComboBoxModel(previewEditorState.previewEditors.toTypedArray())
        box = ComboBox(model).apply {
            item = previewEditorState.currentPreview
            updateBoxWidth()
            addActionListener {
                if (item != null) {
                    previewEditorState.chooseNewEditor(item)
                    testDataEditor.updatePreviewEditor()
                }
            }
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val originalComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    text = (value as? FileEditor).presentableName
                    return originalComponent
                }
            }
            putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, true)
        }

        val label = JBLabel("Available files: ")

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(label)
            add(box)
        }
    }

    private fun createUniqueNameBuilder(): UniqueNameBuilder<VirtualFile>? {
        val project = testDataEditor.editor.project ?: return null
        val builder = UniqueNameBuilder<VirtualFile>(project.basePath, File.separator, 40)
        for (file in files) {
            builder.addPath(file, file.path)
        }
        return builder
    }

    private val FileEditor?.presentableName: String
        get() {
            val file = this?.file ?: return NO_NAME_PROVIDED
            if (file.toNioPath().parent == testDataEditor.baseEditor.file?.toNioPath()?.parent)
                return file.name
            return uniqueNameBuilder?.getShortPath(file) ?: file.name
        }

    fun updateBoxList() {
        model.removeAllElements()
        model.addAll(previewEditorState.previewEditors)
        uniqueNameBuilder = createUniqueNameBuilder()
        box.item = previewEditorState.currentPreview
        box.updateBoxWidth()
    }
}
