/*
 * channel
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.channel.azure.boards.distribution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.alert.channel.api.issue.callback.IssueTrackerCallbackInfoCreator;
import com.synopsys.integration.alert.channel.api.issue.send.IssueTrackerIssueResponseCreator;
import com.synopsys.integration.alert.channel.api.issue.send.IssueTrackerMessageSender;
import com.synopsys.integration.alert.channel.api.issue.send.IssueTrackerMessageSenderFactory;
import com.synopsys.integration.alert.channel.azure.boards.AzureBoardsProperties;
import com.synopsys.integration.alert.channel.azure.boards.AzureBoardsPropertiesFactory;
import com.synopsys.integration.alert.channel.azure.boards.distribution.delegate.AzureBoardsIssueCommenter;
import com.synopsys.integration.alert.channel.azure.boards.distribution.delegate.AzureBoardsIssueCreator;
import com.synopsys.integration.alert.channel.azure.boards.distribution.delegate.AzureBoardsIssueTransitioner;
import com.synopsys.integration.alert.channel.azure.boards.distribution.search.AzureBoardsAlertIssuePropertiesManager;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopsys.integration.alert.common.persistence.model.job.details.AzureBoardsJobDetailsModel;
import com.synopsys.integration.alert.common.rest.ProxyManager;
import com.synopsys.integration.alert.descriptor.api.AzureBoardsChannelKey;
import com.synopsys.integration.azure.boards.common.http.AzureApiVersionAppender;
import com.synopsys.integration.azure.boards.common.http.AzureHttpService;
import com.synopsys.integration.azure.boards.common.service.comment.AzureWorkItemCommentService;
import com.synopsys.integration.azure.boards.common.service.state.AzureWorkItemTypeStateService;
import com.synopsys.integration.azure.boards.common.service.workitem.AzureWorkItemService;

@Component
public class AzureBoardsMessageSenderFactory implements IssueTrackerMessageSenderFactory<AzureBoardsJobDetailsModel, Integer> {
    private final Gson gson;
    private final IssueTrackerCallbackInfoCreator callbackInfoCreator;
    private final AzureBoardsChannelKey channelKey;
    private final AzureBoardsPropertiesFactory azureBoardsPropertiesFactory;
    private final ProxyManager proxyManager;

    @Autowired
    public AzureBoardsMessageSenderFactory(
        Gson gson,
        IssueTrackerCallbackInfoCreator callbackInfoCreator,
        AzureBoardsChannelKey channelKey,
        AzureBoardsPropertiesFactory azureBoardsPropertiesFactory,
        ProxyManager proxyManager
    ) {
        this.gson = gson;
        this.callbackInfoCreator = callbackInfoCreator;
        this.channelKey = channelKey;
        this.azureBoardsPropertiesFactory = azureBoardsPropertiesFactory;
        this.proxyManager = proxyManager;
    }

    @Override
    public IssueTrackerMessageSender<Integer> createMessageSender(AzureBoardsJobDetailsModel distributionDetails) throws AlertException {
        AzureBoardsProperties azureBoardsProperties = azureBoardsPropertiesFactory.createAzureBoardsProperties();
        azureBoardsProperties.validateProperties();

        // Initialize Http Service
        AzureHttpService azureHttpService = azureBoardsProperties.createAzureHttpService(proxyManager.createProxyInfo(), gson);

        // Azure Boards Services
        AzureApiVersionAppender apiVersionAppender = new AzureApiVersionAppender();
        AzureWorkItemService workItemService = new AzureWorkItemService(azureHttpService);
        AzureWorkItemTypeStateService workItemTypeStateService = new AzureWorkItemTypeStateService(azureHttpService, apiVersionAppender);
        AzureWorkItemCommentService workItemCommentService = new AzureWorkItemCommentService(azureHttpService, apiVersionAppender);

        return createMessageSender(
            workItemService,
            workItemTypeStateService,
            workItemCommentService,
            azureBoardsProperties.getOrganizationName(),
            distributionDetails
        );
    }

    public IssueTrackerMessageSender<Integer> createMessageSender(
        AzureWorkItemService workItemService,
        AzureWorkItemTypeStateService workItemTypeStateService,
        AzureWorkItemCommentService workItemCommentService,
        String organizationName,
        AzureBoardsJobDetailsModel distributionDetails
    ) {
        IssueTrackerIssueResponseCreator issueResponseCreator = new IssueTrackerIssueResponseCreator(callbackInfoCreator);
        AzureBoardsWorkItemTypeStateRetriever workItemTypeStateRetriever = new AzureBoardsWorkItemTypeStateRetriever(gson, workItemService, workItemTypeStateService);
        AzureBoardsAlertIssuePropertiesManager issuePropertiesManager = new AzureBoardsAlertIssuePropertiesManager();

        // Message Sender Requirements
        AzureBoardsIssueCommenter commenter = new AzureBoardsIssueCommenter(issueResponseCreator, organizationName, distributionDetails, workItemCommentService);
        AzureBoardsIssueTransitioner transitioner = new AzureBoardsIssueTransitioner(commenter, issueResponseCreator, gson, organizationName, distributionDetails, workItemService, workItemTypeStateRetriever);
        AzureBoardsIssueCreator creator = new AzureBoardsIssueCreator(channelKey, commenter, callbackInfoCreator, gson, organizationName, distributionDetails, workItemService, issuePropertiesManager);

        return new IssueTrackerMessageSender<>(creator, transitioner, commenter);
    }

}
