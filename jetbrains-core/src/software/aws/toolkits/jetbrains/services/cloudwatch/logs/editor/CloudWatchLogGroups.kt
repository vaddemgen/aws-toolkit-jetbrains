// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.CloudWatchLogWindow
import software.aws.toolkits.resources.message
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.swing.JScrollPane
import javax.swing.SortOrder
import javax.swing.table.TableRowSorter

class CloudWatchLogs(private val project: Project, private val cloudWatchLogsClient: CloudWatchLogsClient, private val logGroup: String) :
    SimpleToolWindowPanel(false, false) {
    private val table: TableView<LogStream> = TableView(
        ListTableModel<LogStream>(
            arrayOf(CloudWatchLogsColumn(), CloudWatchLogsColumnDate()),
            listOf<LogStream>(),
            1,
            SortOrder.ASCENDING
        )
    )
    private val scrollPane: JScrollPane = ScrollPaneFactory.createScrollPane(table)
    private val doubleClickListener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount < 2 || e.button != MouseEvent.BUTTON1) {
                return
            }
            val row = table.rowAtPoint(e.point).takeIf { it >= 0 } ?: return
            val window = CloudWatchLogWindow.getInstance(project)
            GlobalScope.launch {
                window.showLogStream(logGroup, table.getRow(row).logStreamName())
            }
        }
    }

    init {
        table.rowSorter = object : TableRowSorter<ListTableModel<LogStream>>(table.listTableModel) {
            init {
                sortKeys = listOf(SortKey(1, SortOrder.DESCENDING))
                setSortable(0, false)
                setSortable(1, false)
            }
        }
        setContent(scrollPane)
        table.addMouseListener(doubleClickListener)

        GlobalScope.launch {
            populateModel()
        }
    }

    private suspend fun populateModel() = withContext(Dispatchers.IO) {
        val streams = cloudWatchLogsClient.describeLogStreamsPaginator(DescribeLogStreamsRequest.builder().logGroupName(logGroup).build())
        streams.filterNotNull().firstOrNull()?.logStreams()?.let { runInEdt { table.tableViewModel.items = it } }
    }
}

class CloudWatchLogsColumn : ColumnInfo<LogStream, String>("log groups <change this is not localized>") {
    override fun valueOf(item: LogStream?): String? = item?.logStreamName()
}

class CloudWatchLogsColumnDate : ColumnInfo<LogStream, String>(message("general.time")) {
    override fun valueOf(item: LogStream?): String? {
        item ?: return null
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.ofEpochMilli(item.lastEventTimestamp()).atOffset(ZoneOffset.UTC))
    }
}
