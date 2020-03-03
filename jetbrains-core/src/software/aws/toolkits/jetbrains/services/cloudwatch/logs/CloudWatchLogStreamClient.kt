// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs

import com.intellij.openapi.Disposable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent

class CloudWatchLogStreamClient(
    private val client: CloudWatchLogsClient,
    private val logGroup: String,
    private val logStream: String,
    private val fromHead: Boolean
) : CoroutineScope by CoroutineScope(CoroutineName("CloudWatchLogsStream")), Disposable {
    private var firstLogTimestamp = 0L
    private var lastLogTimestamp = 0L

    private fun logRequest(request: GetLogEventsRequest, callback: ((List<OutputLogEvent>) -> Unit)) {
        launch {
            val events = client.getLogEvents(request).events()
            if (events.isNotEmpty()) {
                lastLogTimestamp = events.last().timestamp()
                callback(events)
            }
        }
    }

    fun loadInitialAround(startTime: Long, timeScale: Long, callback: ((List<OutputLogEvent>) -> Unit)) {
        logRequest(
            GetLogEventsRequest
                .builder()
                .logGroupName(logGroup)
                .logStreamName(logStream)
                .startTime(startTime - timeScale)
                .endTime(startTime + timeScale).build(),
            callback
        )
    }

    fun loadInitial(callback: ((List<OutputLogEvent>) -> Unit)) {
        logRequest(GetLogEventsRequest.builder().logGroupName(logGroup).logStreamName(logStream).startFromHead(fromHead).build(), callback)
    }

    // TODO implement
    fun loadMore(callback: (List<OutputLogEvent>) -> Unit) {
        launch {
            val events = listOf<OutputLogEvent>()
            if (events.isNotEmpty()) {
                lastLogTimestamp = events.last().timestamp()
                callback(events)
            }
        }
    }

    fun startStreaming(callback: ((List<OutputLogEvent>) -> Unit)) {
        if (coroutineContext[Job]?.children?.firstOrNull() == null) {
            launch {
                while (true) {
                    streamMore(callback)
                    delay(1000L)
                }
            }
        }
    }

    fun pauseStreaming() {
        if (coroutineContext[Job]?.children?.firstOrNull() != null) {
            coroutineContext[Job]?.cancelChildren()
        }
    }

    private fun streamMore(callback: ((List<OutputLogEvent>) -> Unit)) {
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
        cancel()
    }
}
