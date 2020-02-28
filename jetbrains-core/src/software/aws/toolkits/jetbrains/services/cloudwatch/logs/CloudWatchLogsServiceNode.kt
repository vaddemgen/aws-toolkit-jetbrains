// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs

import com.intellij.openapi.project.Project
import icons.AwsIcons
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup
import software.aws.toolkits.jetbrains.core.explorer.AwsExplorerService
import software.aws.toolkits.jetbrains.core.explorer.nodes.AwsExplorerNode
import software.aws.toolkits.jetbrains.core.explorer.nodes.AwsExplorerResourceNode
import software.aws.toolkits.jetbrains.core.explorer.nodes.CacheBackedAwsExplorerServiceRootNode
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.resources.CloudWatchResources

class CloudWatchLogsServiceNode(project: Project) : CacheBackedAwsExplorerServiceRootNode<LogGroup>(
    project,
    AwsExplorerService.CLOUDWATCH,
    CloudWatchResources.LIST_LOG_GROUPS
) {
    override fun toNode(child: LogGroup): AwsExplorerNode<*> = CloudWatchLogsNode(nodeProject, child.arn(), child.logGroupName())
}

class CloudWatchLogsNode(
    project: Project,
    val arn: String,
    val logGroupName: String
) : AwsExplorerResourceNode<String>(
    project,
    CloudFormationClient.SERVICE_NAME,
    logGroupName,
    AwsIcons.Resources.CLOUDFORMATION_STACK
) {
    override fun resourceType() = "stream"

    override fun resourceArn() = arn

    override fun displayName() = logGroupName

    override fun onDoubleClick() {
        GlobalScope.launch {
            val window = project?.let { CloudWatchLogWindow.getInstance(it) }
            window?.showLogGroup(logGroupName)
        }
    }
}
