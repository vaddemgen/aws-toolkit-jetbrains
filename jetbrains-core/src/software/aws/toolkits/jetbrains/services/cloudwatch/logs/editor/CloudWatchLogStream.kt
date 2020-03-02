// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.table.TableCellRenderer

class CloudWatchLogStream(client: CloudWatchLogsClient, logGroup: String, logStream: String) : SimpleToolWindowPanel(false, false) {
    lateinit var content: JPanel
    lateinit var logsPanel: JPanel
    lateinit var searchLabel: JLabel
    lateinit var searchField: JTextField
    lateinit var showAsButton: JButton
    private val defaultModel = ListTableModel<OutputLogEvent>(
        object : ColumnInfo<OutputLogEvent, String>("time <change this is not localized>") {
            override fun valueOf(item: OutputLogEvent?): String? = item?.timestamp().toString()
        },
        object : ColumnInfo<OutputLogEvent, String>("message <change this is not localized>") {
            override fun valueOf(item: OutputLogEvent?): String? = item?.message()
        }
    )
    private val wrappingModel = ListTableModel<OutputLogEvent>(defaultModel.columnInfos[0],
        object : ColumnInfo<OutputLogEvent, String>("message <change this is not localized>") {
            override fun valueOf(item: OutputLogEvent?): String? = item?.message()

            override fun getRenderer(item: OutputLogEvent?): TableCellRenderer? = WrapCellRenderer
        })

    private var logsTableView: TableView<OutputLogEvent> = TableView<OutputLogEvent>(defaultModel)

    init {
        // allow one column to be selected for copy paste
        //logsTableView.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
        val logsScrollPane = ScrollPaneFactory.createScrollPane(logsTableView)
        logsPanel.add(logsScrollPane)
        GlobalScope.launch {
            val events = client.getLogEventsPaginator { it.logGroupName(logGroup).logStreamName(logStream) }.events()
            events.filterNotNull().let { runInEdt { logsTableView.tableViewModel.items = it } }
        }
        showAsButton.addActionListener {
            logsTableView.setModelAndUpdateColumns(wrappingModel)
        }
    }
}
