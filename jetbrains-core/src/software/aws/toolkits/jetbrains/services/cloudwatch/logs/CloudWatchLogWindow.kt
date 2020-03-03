// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.aws.toolkits.jetbrains.core.awsClient
import software.aws.toolkits.jetbrains.core.toolwindow.ToolkitToolWindowManager
import software.aws.toolkits.jetbrains.core.toolwindow.ToolkitToolWindowType
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor.CloudWatchLogStream
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor.CloudWatchLogs
import software.aws.toolkits.resources.message

class CloudWatchLogWindow(private val project: Project) {
    private val toolWindow = ToolkitToolWindowManager.getInstance(project, CW_LOGS_TOOL_WINDOW)

    fun showLogGroup(logGroup: String) {
        val existingWindow = toolWindow.find(logGroup)
        if (existingWindow != null) {
            runInEdt {
                existingWindow.show()
            }
            return
        }
        val client = project.awsClient<CloudWatchLogsClient>()
        val groups = CloudWatchLogs(project, client, logGroup)
        runInEdt {
            toolWindow.addTab(logGroup, groups.component!!, activate = true, id = logGroup)
        }
    }

    fun showLogStream(logGroup: String, logStream: String, fromHead: Boolean = true, title: String? = null) {
        val client = project.awsClient<CloudWatchLogsClient>()
        //val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).apply { setViewer(true) }.console
        val id = "$logGroup/$logStream"
        val displayName = title ?: id
        // dispose existing window if it exists to update. TODO fix this massive hack
        val existingWindow = toolWindow.find(id)
        if (existingWindow != null) {
            runInEdt {
                existingWindow.dispose()
            }
        }
        val group = CloudWatchLogStream(client, logGroup, logStream)
        runInEdt {
            toolWindow.addTab(displayName, group.content, activate = true, id = id)
        }
        /*
        val events = client.getLogEventsPaginator { it.logGroupName(logGroup).logStreamName(logStream).startFromHead(fromHead) }.events()
        if (events.none()) {
            console.print(message("ecs.service.logs.empty", "$logGroup/$logStream\n"), ConsoleViewContentType.NORMAL_OUTPUT)
        } else {
            events.forEach { console.print("${it.message().trim()}\n", ConsoleViewContentType.NORMAL_OUTPUT) }
        }*/
        // allow one column to be selected for copy paste
        // table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    companion object {
        private val CW_LOGS_TOOL_WINDOW = ToolkitToolWindowType("AWS.CloudWatchLog", message("cloudwatch.log.toolwindow"))
        fun getInstance(project: Project) = ServiceManager.getService(project, CloudWatchLogWindow::class.java)
    }
}
