// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.ui.components.JBTextArea
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

object WrapCellRenderer : JBTextArea(), TableCellRenderer {
    init {
        lineWrap = true
        wrapStyleWord = true
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        text = value.toString().trimEnd()
        setSize(table.columnModel.getColumn(column).width, preferredSize.height)
        if (table.getRowHeight(row) != preferredSize.height) {
            table.setRowHeight(row, preferredSize.height)
        }
        return this
    }
}
