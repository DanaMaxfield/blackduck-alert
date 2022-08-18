package com.synopsys.integration.alert.channel.jira.server.distribution.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.synopsys.integration.alert.api.channel.issue.IssueTrackerResponsePostProcessor;
import com.synopsys.integration.alert.api.channel.issue.callback.IssueTrackerCallbackInfoCreator;
import com.synopsys.integration.alert.api.channel.issue.event.IssueTrackerCommentEvent;
import com.synopsys.integration.alert.api.channel.issue.model.IssueCommentModel;
import com.synopsys.integration.alert.api.channel.issue.search.ExistingIssueDetails;
import com.synopsys.integration.alert.api.channel.issue.search.IssueCategoryRetriever;
import com.synopsys.integration.alert.api.channel.issue.search.enumeration.IssueCategory;
import com.synopsys.integration.alert.api.channel.issue.search.enumeration.IssueStatus;
import com.synopsys.integration.alert.api.event.EventManager;
import com.synopsys.integration.alert.channel.jira.server.JiraServerProperties;
import com.synopsys.integration.alert.channel.jira.server.JiraServerPropertiesFactory;
import com.synopsys.integration.alert.channel.jira.server.distribution.JiraServerMessageSenderFactory;
import com.synopsys.integration.alert.common.persistence.accessor.JobDetailsAccessor;
import com.synopsys.integration.alert.common.persistence.accessor.JobSubTaskAccessor;
import com.synopsys.integration.alert.common.persistence.model.job.details.JiraServerJobDetailsModel;
import com.synopsys.integration.alert.descriptor.api.model.ChannelKeys;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jira.common.model.request.IssueCommentRequestModel;
import com.synopsys.integration.jira.common.model.response.IssueCommentResponseModel;
import com.synopsys.integration.jira.common.server.service.FieldService;
import com.synopsys.integration.jira.common.server.service.IssueSearchService;
import com.synopsys.integration.jira.common.server.service.IssueService;
import com.synopsys.integration.jira.common.server.service.JiraServerServiceFactory;
import com.synopsys.integration.jira.common.server.service.ProjectService;

class JiraServerCommentEventHandlerTest {
    private Gson gson = new Gson();
    private AtomicInteger issueCounter;
    private EventManager eventManager;
    private JobSubTaskAccessor jobSubTaskAccessor;
    private IssueTrackerResponsePostProcessor responsePostProcessor;

    @BeforeEach
    public void init() {
        issueCounter = new AtomicInteger(0);
        eventManager = Mockito.mock(EventManager.class);
        jobSubTaskAccessor = Mockito.mock(JobSubTaskAccessor.class);
        responsePostProcessor = Mockito.mock(IssueTrackerResponsePostProcessor.class);
    }

    @Test
    void handleUnknownJobTest() {
        UUID parentEventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Set<Long> notificationIds = Set.of(1L, 2L, 3L, 4L);

        JiraServerPropertiesFactory propertiesFactory = Mockito.mock(JiraServerPropertiesFactory.class);
        IssueTrackerCallbackInfoCreator callbackInfoCreator = new IssueTrackerCallbackInfoCreator();
        IssueCategoryRetriever issueCategoryRetriever = new IssueCategoryRetriever();
        JiraServerMessageSenderFactory messageSenderFactory = new JiraServerMessageSenderFactory(
            gson,
            ChannelKeys.JIRA_SERVER,
            propertiesFactory,
            callbackInfoCreator,
            issueCategoryRetriever,
            eventManager,
            jobSubTaskAccessor
        );
        JobDetailsAccessor<JiraServerJobDetailsModel> jobDetailsAccessor = jobId1 -> Optional.empty();

        JiraServerCommentEventHandler handler = new JiraServerCommentEventHandler(
            eventManager,
            jobSubTaskAccessor,
            gson,
            propertiesFactory,
            messageSenderFactory,
            jobDetailsAccessor,
            responsePostProcessor
        );
        ExistingIssueDetails<String> existingIssueDetails = new ExistingIssueDetails<>("id", "key", "summary", "link", IssueStatus.UNKNOWN, IssueCategory.BOM);
        IssueCommentModel<String> model = new IssueCommentModel<>(existingIssueDetails, List.of(), null);
        JiraServerCommentEvent event = new JiraServerCommentEvent(
            IssueTrackerCommentEvent.createDefaultEventDestination(ChannelKeys.JIRA_SERVER),
            parentEventId,
            jobId,
            notificationIds,
            model
        );
        handler.handle(event);
        assertEquals(0, issueCounter.get());
    }

    @Test
    void handleCommentTest() throws IntegrationException {
        UUID parentEventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Set<Long> notificationIds = Set.of(1L, 2L, 3L, 4L);

        JiraServerPropertiesFactory propertiesFactory = Mockito.mock(JiraServerPropertiesFactory.class);
        JiraServerProperties jiraProperties = Mockito.mock(JiraServerProperties.class);
        JiraServerServiceFactory jiraServiceFactory = Mockito.mock(JiraServerServiceFactory.class);
        IssueService issueService = Mockito.mock(IssueService.class);
        IssueSearchService issueSearchService = Mockito.mock(IssueSearchService.class);
        FieldService fieldService = Mockito.mock(FieldService.class);
        ProjectService projectService = Mockito.mock(ProjectService.class);

        Mockito.doAnswer(invocation -> {
            issueCounter.incrementAndGet();
            return new IssueCommentResponseModel();
        }).when(issueService).addComment(Mockito.any(IssueCommentRequestModel.class));

        Mockito.when(jiraServiceFactory.createFieldService()).thenReturn(fieldService);
        Mockito.when(jiraServiceFactory.createIssueService()).thenReturn(issueService);
        Mockito.when(jiraServiceFactory.createIssueSearchService()).thenReturn(issueSearchService);
        Mockito.when(jiraServiceFactory.createProjectService()).thenReturn(projectService);
        Mockito.when(jiraProperties.createJiraServicesServerFactory(Mockito.any(), Mockito.any())).thenReturn(jiraServiceFactory);
        Mockito.when(propertiesFactory.createJiraPropertiesWithJobId(jobId)).thenReturn(jiraProperties);

        IssueTrackerCallbackInfoCreator callbackInfoCreator = new IssueTrackerCallbackInfoCreator();
        IssueCategoryRetriever issueCategoryRetriever = new IssueCategoryRetriever();
        JiraServerMessageSenderFactory messageSenderFactory = new JiraServerMessageSenderFactory(
            gson,
            ChannelKeys.JIRA_SERVER,
            propertiesFactory,
            callbackInfoCreator,
            issueCategoryRetriever,
            eventManager,
            jobSubTaskAccessor
        );
        JobDetailsAccessor<JiraServerJobDetailsModel> jobDetailsAccessor = jobId1 -> Optional.of(createJobDetails(jobId));

        JiraServerCommentEventHandler handler = new JiraServerCommentEventHandler(
            eventManager,
            jobSubTaskAccessor,
            gson,
            propertiesFactory,
            messageSenderFactory,
            jobDetailsAccessor,
            responsePostProcessor
        );
        ExistingIssueDetails<String> existingIssueDetails = new ExistingIssueDetails<>("id", "key", "summary", "link", IssueStatus.UNKNOWN, IssueCategory.BOM);
        IssueCommentModel<String> model = new IssueCommentModel<>(existingIssueDetails, List.of("A comment"), null);
        JiraServerCommentEvent event = new JiraServerCommentEvent(
            IssueTrackerCommentEvent.createDefaultEventDestination(ChannelKeys.JIRA_SERVER),
            parentEventId,
            jobId,
            notificationIds,
            model
        );
        handler.handle(event);
        assertEquals(1, issueCounter.get());
    }

    private JiraServerJobDetailsModel createJobDetails(UUID jobId) {
        return new JiraServerJobDetailsModel(
            jobId,
            true,
            "user",
            "jiraProject",
            "Task",
            "Resolve",
            "Reopen",
            List.of(),
            ""
        );
    }
}
