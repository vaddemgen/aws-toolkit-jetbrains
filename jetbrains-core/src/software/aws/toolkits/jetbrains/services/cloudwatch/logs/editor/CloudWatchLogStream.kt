package software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor

import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField

class CloudWatchLogStream(client: CloudWatchLogsClient, logGroup: String, logStream: String) {
    lateinit var content: JPanel
    lateinit var searchLabel: JLabel
    lateinit var searchField: JTextField
    var logsTableView: TableView<OutputLogEvent> = TableView<OutputLogEvent>(defaultModel)
    lateinit var logsScrollPane: JScrollPane

    init {
        logsScrollPane.setViewportView(logsTableView)
        GlobalScope.launch {
            val events = client.getLogEventsPaginator { it.logGroupName(logGroup).logStreamName(logStream) }.events()
            events.filterNotNull().let { logsTableView.tableViewModel.items = it }
        }
    }
}

private val defaultModel = ListTableModel<OutputLogEvent>(
    object : ColumnInfo<OutputLogEvent, String>("log groups <change this is not localized>") {
        override fun valueOf(item: OutputLogEvent?): String? = item?.message()
    }
)
