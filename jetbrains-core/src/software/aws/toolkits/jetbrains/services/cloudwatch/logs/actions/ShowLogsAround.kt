// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ShowLogsAround : AnAction("Show logs around <LOCALIZE THIS>", "abc <localize this>", AllIcons.Ide.Link), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
    }
}
