package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.execution.util.ListTableWithButtons
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
import javax.swing.SortOrder

class CloudWatchLogGroups(private val cloudWatchLogsClient: CloudWatchLogsClient, private val logGroup: String) : ListTableWithButtons<LogStream>() {
    init {
        GlobalScope.launch {
            populateModel()
        }
    }

    override fun createListModel(): ListTableModel<*> =
        ListTableModel<LogStream>(
            arrayOf(CloudWatchLogsColumn(), CloudWatchLogsColumnDate()),
            listOf<LogStream>(),
            1,
            SortOrder.ASCENDING
        )

    private suspend fun populateModel() = withContext(Dispatchers.IO) {
        val streams = cloudWatchLogsClient.describeLogStreamsPaginator(DescribeLogStreamsRequest.builder().logGroupName(logGroup).build())
        streams.filterNotNull().firstOrNull()?.logStreams()?.let { setValues(it) }
    }

    override fun canDeleteElement(selection: LogStream?): Boolean = false

    override fun createElement(): LogStream = LogStream.builder().build()

    override fun isEmpty(element: LogStream?): Boolean = element == null

    override fun cloneElement(variable: LogStream?): LogStream = variable!!.toBuilder().build()

    private companion object {
        val LOG = getLogger<LogStream>()
    }
}

class CloudWatchLogsColumn : ColumnInfo<LogStream, String>("title") {
    override fun valueOf(item: LogStream?): String? = item?.logStreamName()
}

class CloudWatchLogsColumnDate : ColumnInfo<LogStream, String>("date") {
    override fun valueOf(item: LogStream?): String? {
        item ?: return null
        return item.lastEventTimestamp().toString()
    }
}
