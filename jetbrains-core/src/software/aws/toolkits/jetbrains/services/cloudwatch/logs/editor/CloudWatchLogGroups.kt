package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.execution.util.ListTableWithButtons
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
import software.aws.toolkits.core.utils.getLogger
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JScrollPane
import javax.swing.SortOrder

class CloudWatchLogs(private val cloudWatchLogsClient: CloudWatchLogsClient, private val logGroup: String) : SimpleToolWindowPanel(false, false) {
    private val table: TableView<LogStream> = TableView(
        ListTableModel<LogStream>(
            arrayOf(CloudWatchLogsColumn(), CloudWatchLogsColumnDate()),
            listOf<LogStream>(),
            1,
            SortOrder.ASCENDING
        )
    )
    private val scrollPane: JScrollPane = ScrollPaneFactory.createScrollPane(table)

    init {
        setContent(scrollPane)
        GlobalScope.launch {
            populateModel()
        }
    }

    private suspend fun populateModel() = withContext(Dispatchers.IO) {
        val streams = cloudWatchLogsClient.describeLogStreamsPaginator(DescribeLogStreamsRequest.builder().logGroupName(logGroup).build())
        streams.filterNotNull().firstOrNull()?.logStreams()?.let { table.tableViewModel.items = it }
    }
}

class CloudWatchLogsColumn : ColumnInfo<LogStream, String>("log groups <change this is not localized>") {
    override fun valueOf(item: LogStream?): String? = item?.logStreamName()
}

class CloudWatchLogsColumnDate : ColumnInfo<LogStream, String>("date <change this is not localized>") {
    override fun valueOf(item: LogStream?): String? {
        item ?: return null
        return item.lastEventTimestamp().toString()
    }
}
