/**
 * blackduck-alert
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.alert.channel.jira.actions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.alert.channel.jira.JiraChannel;
import com.synopsys.integration.alert.channel.jira.JiraProperties;
import com.synopsys.integration.alert.channel.jira.descriptor.JiraDescriptor;
import com.synopsys.integration.alert.channel.jira.model.JiraMessageResult;
import com.synopsys.integration.alert.channel.jira.util.JiraTransitionHelper;
import com.synopsys.integration.alert.common.action.ChannelDistributionTestAction;
import com.synopsys.integration.alert.common.descriptor.config.ui.ChannelDistributionUIConfig;
import com.synopsys.integration.alert.common.descriptor.config.ui.ProviderDistributionUIConfig;
import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.event.DistributionEvent;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopsys.integration.alert.common.exception.AlertFieldException;
import com.synopsys.integration.alert.common.message.model.MessageContentGroup;
import com.synopsys.integration.alert.common.message.model.MessageResult;
import com.synopsys.integration.alert.common.message.model.ProviderMessageContent;
import com.synopsys.integration.alert.common.persistence.accessor.FieldAccessor;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jira.common.cloud.model.components.TransitionComponent;
import com.synopsys.integration.jira.common.cloud.rest.service.IssueService;
import com.synopsys.integration.jira.common.cloud.rest.service.JiraCloudServiceFactory;
import com.synopsys.integration.rest.RestConstants;

@Component
public class JiraDistributionTestAction extends ChannelDistributionTestAction {
    private final Logger logger = LoggerFactory.getLogger(JiraDistributionTestAction.class);
    private Gson gson;

    @Autowired
    public JiraDistributionTestAction(JiraChannel jiraChannel, Gson gson) {
        super(jiraChannel);
        this.gson = gson;
    }

    @Override
    public JiraChannel getDistributionChannel() {
        return (JiraChannel) super.getDistributionChannel();
    }

    @Override
    public MessageResult testConfig(String jobId, String destination, FieldAccessor fieldAccessor) throws IntegrationException {
        String messageId = UUID.randomUUID().toString();

        final JiraMessageResult initialTestResult = createAndSendMessage(jobId, fieldAccessor, ItemOperation.ADD, messageId);
        String initialIssueKey = initialTestResult.getUpdatedIssueKeys()
                                     .stream()
                                     .findFirst()
                                     .orElseThrow(() -> new AlertException("Failed to create a new issue"));

        JiraProperties jiraProperties = new JiraProperties(fieldAccessor);
        JiraCloudServiceFactory jiraCloudServiceFactory = jiraProperties.createJiraServicesCloudFactory(logger, gson);
        IssueService issueService = jiraCloudServiceFactory.createIssueService();

        Optional<String> optionalResolveTransitionName = fieldAccessor.getString(JiraDescriptor.KEY_RESOLVE_WORKFLOW_TRANSITION).filter(StringUtils::isNotBlank);
        if (optionalResolveTransitionName.isPresent()) {
            String resolveTransitionName = optionalResolveTransitionName.get();
            return testTransitions(jobId, fieldAccessor, messageId, issueService, resolveTransitionName, initialIssueKey);
        }
        return initialTestResult;
    }

    private JiraMessageResult testTransitions(String jobId, FieldAccessor fieldAccessor, String messageId, IssueService issueService, String resolveTransitionName, String initialIssueKey) throws IntegrationException {
        JiraTransitionHelper transitionHelper = new JiraTransitionHelper(issueService);
        String fromStatus = "Initial";
        String toStatus = "Resolve";
        Optional<String> possibleSecondIssueKey = Optional.empty();
        try {
            Map<String, String> transitionErrors = new HashMap<>();
            Map<String, String> resolveErrors = validateTransition(transitionHelper, initialIssueKey, resolveTransitionName, JiraDescriptor.KEY_RESOLVE_WORKFLOW_TRANSITION, JiraTransitionHelper.DONE_STATUS_CATEGORY_KEY);
            transitionErrors.putAll(resolveErrors);
            JiraMessageResult finalResult = createAndSendMessage(jobId, fieldAccessor, ItemOperation.DELETE, messageId);

            Optional<String> optionalReopenTransitionName = fieldAccessor.getString(JiraDescriptor.KEY_OPEN_WORKFLOW_TRANSITION).filter(StringUtils::isNotBlank);
            if (optionalReopenTransitionName.isPresent()) {
                fromStatus = toStatus;
                toStatus = "Reopen";
                Map<String, String> reopenErrors = validateTransition(transitionHelper, initialIssueKey, optionalReopenTransitionName.get(), JiraDescriptor.KEY_OPEN_WORKFLOW_TRANSITION, JiraTransitionHelper.TODO_STATUS_CATEGORY_KEY);
                transitionErrors.putAll(reopenErrors);
                JiraMessageResult reopenResult = createAndSendMessage(jobId, fieldAccessor, ItemOperation.ADD, messageId);
                possibleSecondIssueKey = reopenResult.getUpdatedIssueKeys()
                                             .stream()
                                             .findFirst()
                                             .filter(secondIssueKey -> !StringUtils.equals(secondIssueKey, initialIssueKey));

                if (reopenErrors.isEmpty()) {
                    fromStatus = toStatus;
                    toStatus = "Resolve";
                    Map<String, String> reResolveErrors = validateTransition(transitionHelper, initialIssueKey, resolveTransitionName, JiraDescriptor.KEY_RESOLVE_WORKFLOW_TRANSITION, JiraTransitionHelper.DONE_STATUS_CATEGORY_KEY);
                    transitionErrors.putAll(reResolveErrors);
                    finalResult = createAndSendMessage(jobId, fieldAccessor, ItemOperation.DELETE, messageId);
                }
            }

            if (transitionErrors.isEmpty()) {
                return finalResult;
            } else {
                throw new AlertFieldException(transitionErrors);
            }
        } catch (AlertFieldException fieldException) {
            safelyCleanUpIssue(issueService, initialIssueKey);
            throw fieldException;
        } catch (AlertException alertException) {
            logger.debug("Error testing Jira Cloud config", alertException);
            String errorMessage = String.format("There were problems transitioning the test issue from the %s status to the %s status: %s", fromStatus, toStatus, alertException.getMessage());
            possibleSecondIssueKey.ifPresent(key -> safelyCleanUpIssue(issueService, key));
            throw new AlertException(errorMessage);
        }
    }

    private DistributionEvent createChannelTestEvent(final String jobId, final FieldAccessor fieldAccessor, ItemOperation operation, String messageId) throws AlertException {
        final ProviderMessageContent messageContent = createTestNotificationContent(fieldAccessor, operation, messageId);

        final String channelName = fieldAccessor.getStringOrEmpty(ChannelDistributionUIConfig.KEY_CHANNEL_NAME);
        final String providerName = fieldAccessor.getStringOrEmpty(ChannelDistributionUIConfig.KEY_PROVIDER_NAME);
        final String formatType = fieldAccessor.getStringOrEmpty(ProviderDistributionUIConfig.KEY_FORMAT_TYPE);

        return new DistributionEvent(jobId, channelName, RestConstants.formatDate(new Date()), providerName, formatType, MessageContentGroup.singleton(messageContent), fieldAccessor);
    }

    private JiraMessageResult createAndSendMessage(String jobId, FieldAccessor fieldAccessor, ItemOperation operation, String messageId) throws IntegrationException {
        logger.debug("Sending {} test message...", operation.name());
        final DistributionEvent resolveIssueEvent = createChannelTestEvent(jobId, fieldAccessor, operation, messageId);
        JiraMessageResult messageResult = getDistributionChannel().sendMessage(resolveIssueEvent);
        logger.debug("{} test message sent!", operation.name());
        return messageResult;
    }

    private Map<String, String> validateTransition(JiraTransitionHelper jiraTransitionHelper, String issueKey, String transitionName, String transitionKey, String statusCategoryKey) throws IntegrationException {
        Optional<TransitionComponent> transitionComponent = jiraTransitionHelper.retrieveIssueTransition(issueKey, transitionName);
        if (transitionComponent.isPresent()) {
            boolean isValidTransition = jiraTransitionHelper.doesTransitionToExpectedStatusCategory(transitionComponent.get(), statusCategoryKey);
            if (!isValidTransition) {
                return Map.of(transitionKey, "The provided transition would not result in an allowed status category.");
            }
        } else {
            return Map.of(transitionKey, "The provided transition is not possible from the issue state that it would transition from.");
        }
        return Map.of();
    }

    private void safelyCleanUpIssue(IssueService issueService, String issueKey) {
        try {
            issueService.deleteIssue(issueKey);
        } catch (IntegrationException e) {
            logger.warn("There was a problem trying to delete a the Jira Cloud distribution test issue, {}: {}", issueKey, e);
        }
    }

}
