// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.ui.feedback

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import icons.AwsIcons
import org.jetbrains.annotations.TestOnly
import software.aws.toolkits.jetbrains.services.telemetry.TelemetryService
import software.aws.toolkits.resources.message

class FeedbackDialog(project: Project, initiallyPositive: Boolean) : DialogWrapper(project) {
    val panel = SubmitFeedbackPanel(initiallyPositive)

    init {
        title = feedbackTitle
        init()
    }

    override fun doOKAction() {
        if (okAction.isEnabled) {
            setOKButtonText(message("feedback.submitting"))
            isOKActionEnabled = false

            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val sentiment = panel.sentiment ?: throw RuntimeException("sentiment was null after validation")
                    val comment = panel.comment ?: throw RuntimeException("comment was null after validation")
                    ApplicationManager.getApplication().executeOnPooledThread {
                        TelemetryService.getInstance().sendFeedback(sentiment, comment)
                        ApplicationManager.getApplication().invokeLater({
                            close(OK_EXIT_CODE)
                        }, ModalityState.stateForComponent(panel.panel))
                    }
                } catch (e: Exception) {
                    Messages.showMessageDialog(panel.panel, message("feedback.submit_failed", e), message("feedback.submit_failed_title"), null)
                }
            }
        }
    }

    public override fun doValidate(): ValidationInfo? {
        val sentiment = panel.sentiment
        val comment = panel.comment

        sentiment ?: return ValidationInfo(message("feedback.validation.no_sentiment"))

        return when {
            comment == null || comment.isEmpty() -> ValidationInfo(message("feedback.validation.empty_comment"))
            comment.length >= SubmitFeedbackPanel.MAX_LENGTH -> ValidationInfo(message("feedback.validation.comment_too_long"))
            else -> null
        }
    }

    override fun createCenterPanel() = panel.panel

    @TestOnly
    internal fun getViewForTesting(): SubmitFeedbackPanel = panel

    companion object {
        private val feedbackTitle = message("feedback.title")
        fun getAction(project: Project) =
            object : DumbAwareAction(feedbackTitle, message("feedback.description"), AwsIcons.Misc.SMILE_GREY) {
                override fun actionPerformed(e: AnActionEvent) {
                    FeedbackDialog(project, true).showAndGet()
                }
            }
    }
}
