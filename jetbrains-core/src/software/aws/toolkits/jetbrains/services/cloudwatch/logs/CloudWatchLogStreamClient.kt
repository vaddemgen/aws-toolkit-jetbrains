// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs

import com.intellij.openapi.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent

class CloudWatchLogStreamClient(
    private val client: CloudWatchLogsClient,
    private val logGroup: String,
    private val logStream: String
) : CoroutineScope by GlobalScope, Disposable {
    private var lastLogTimestamp = 0L

    fun loadInitial(callback: ((List<OutputLogEvent>) -> Unit)) = launch {
        val events = client.getLogEvents { it.logGroupName(logGroup).logStreamName(logStream) }.events()
        if (events.isNotEmpty()) {
            lastLogTimestamp = events.last().timestamp()
            callback(events)
        }
    }

    fun startStreaming(callback: ((List<OutputLogEvent>) -> Unit)) {
        if (!this.coroutineContext.isActive) {
            launch {
                while (true) {
                    loadMore(callback)
                    delay(1000L)
                }
            }
        }
    }

    fun pauseStreaming() {
        if (this.coroutineContext.isActive) {
            cancel()
        }
    }

    private fun loadMore(callback: ((List<OutputLogEvent>) -> Unit)) {
        // Add 1 millisecond to query more events
        val queryTimestamp = lastLogTimestamp + 1
        val newEvents = client
            .getLogEventsPaginator { it.logGroupName(logGroup).logStreamName(logStream).startTime(queryTimestamp).build() }
            .events()
            .filterNotNull()
        if (newEvents.isNotEmpty()) {
            lastLogTimestamp = newEvents.last().timestamp()
            callback(newEvents)
        }
    }

    override fun dispose() {
        pauseStreaming()
    }
}
