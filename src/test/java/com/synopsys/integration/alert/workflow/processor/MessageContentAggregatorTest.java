package com.synopsys.integration.alert.workflow.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.synopsys.integration.alert.TestConstants;
import com.synopsys.integration.alert.common.descriptor.config.ui.ProviderDistributionUIConfig;
import com.synopsys.integration.alert.common.enumeration.FrequencyType;
import com.synopsys.integration.alert.common.message.model.MessageContentGroup;
import com.synopsys.integration.alert.common.persistence.accessor.ConfigurationAccessor;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationFieldModel;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationJobModel;
import com.synopsys.integration.alert.common.provider.Provider;
import com.synopsys.integration.alert.common.rest.model.AlertNotificationWrapper;
import com.synopsys.integration.alert.database.notification.NotificationContent;
import com.synopsys.integration.alert.mock.MockConfigurationModelFactory;
import com.synopsys.integration.alert.provider.blackduck.BlackDuckProvider;
import com.synopsys.integration.alert.provider.blackduck.descriptor.BlackDuckDistributionUIConfig;
import com.synopsys.integration.alert.util.AlertIntegrationTest;
import com.synopsys.integration.alert.workflow.filter.NotificationFilter;
import com.synopsys.integration.blackduck.api.generated.enumeration.NotificationType;

public class MessageContentAggregatorTest extends AlertIntegrationTest {
    @Autowired
    private List<Provider> providers;
    @Autowired
    private ConfigurationAccessor jobConfigReader;
    @Autowired
    private NotificationFilter notificationFilter;

    @Test
    public void testNoJobProcessing() throws Exception {
        final FrequencyType frequencyType = FrequencyType.REAL_TIME;
        final String policyContent = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper policyNotification = createNotification(BlackDuckProvider.COMPONENT_NAME, policyContent, NotificationType.RULE_VIOLATION_CLEARED);

        final String vulnerabilityContent = getNotificationContentFromFile(TestConstants.VULNERABILITY_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper vulnerabilityNotification = createNotification(BlackDuckProvider.COMPONENT_NAME, vulnerabilityContent, NotificationType.VULNERABILITY);

        final List<AlertNotificationWrapper> notificationContentList = List.of(policyNotification, vulnerabilityNotification);
        final MessageContentAggregator messageContentAggregator = new MessageContentAggregator(jobConfigReader, providers, notificationFilter);
        final Map<ConfigurationJobModel, List<MessageContentGroup>> topicContentMap = messageContentAggregator.processNotifications(frequencyType, notificationContentList);

        assertTrue(topicContentMap.isEmpty());
    }

    @Test
    public void testJobProcessing() throws Exception {
        final FrequencyType frequencyType = FrequencyType.REAL_TIME;
        final String policyContent = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper policyNotification = createNotification(BlackDuckProvider.COMPONENT_NAME, policyContent, NotificationType.RULE_VIOLATION_CLEARED);

        final String vulnerabilityContent = getNotificationContentFromFile(TestConstants.VULNERABILITY_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper vulnerabilityNotification = createNotification(BlackDuckProvider.COMPONENT_NAME, vulnerabilityContent, NotificationType.VULNERABILITY);

        final List<AlertNotificationWrapper> notificationContentList = List.of(policyNotification, vulnerabilityNotification);

        final List<String> projects = List.of("example", "alert-test-project", "alert-test-project-2");
        final List<String> notificationTypes = List.of(NotificationType.RULE_VIOLATION_CLEARED.name(), NotificationType.VULNERABILITY.name());
        final ConfigurationJobModel jobConfig = createCommonDistributionConfiguration(projects, notificationTypes);
        final ConfigurationAccessor spiedReader = Mockito.spy(jobConfigReader);
        Mockito.doReturn(List.of(jobConfig)).when(spiedReader).getAllJobs();

        final MessageContentAggregator messageContentAggregator = new MessageContentAggregator(spiedReader, providers, notificationFilter);
        final Map<ConfigurationJobModel, List<MessageContentGroup>> topicContentMap = messageContentAggregator.processNotifications(frequencyType, notificationContentList);

        assertFalse(topicContentMap.isEmpty());
        assertEquals(1, topicContentMap.size());
        assertTrue(topicContentMap.containsKey(jobConfig));
        assertEquals(4, topicContentMap.get(jobConfig).size());
    }

    @Test
    public void testJobProcessingFrequencyMismatch() throws Exception {
        final FrequencyType frequencyType = FrequencyType.DAILY;
        final String policyContent = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper policyNotification = createNotification(BlackDuckProvider.COMPONENT_NAME, policyContent, NotificationType.RULE_VIOLATION_CLEARED);

        final String vulnerabilityContent = getNotificationContentFromFile(TestConstants.VULNERABILITY_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper vulnerabilityNotification = createNotification(BlackDuckProvider.COMPONENT_NAME, vulnerabilityContent, NotificationType.VULNERABILITY);

        final List<AlertNotificationWrapper> notificationContentList = List.of(policyNotification, vulnerabilityNotification);

        final ConfigurationAccessor spiedReader = Mockito.spy(jobConfigReader);
        Mockito.doReturn(List.of()).when(spiedReader).getAllJobs();

        final MessageContentAggregator messageContentAggregator = new MessageContentAggregator(spiedReader, providers, notificationFilter);
        final Map<ConfigurationJobModel, List<MessageContentGroup>> topicContentMap = messageContentAggregator.processNotifications(frequencyType, notificationContentList);

        assertTrue(topicContentMap.isEmpty());
    }

    @Test
    public void testJobProcessingProviderMismatch() throws Exception {
        final String unknownProvider = "unknown_provider";
        final FrequencyType frequencyType = FrequencyType.REAL_TIME;
        final String policyContent = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper policyNotification = createNotification(unknownProvider, policyContent, NotificationType.RULE_VIOLATION_CLEARED);

        final String vulnerabilityContent = getNotificationContentFromFile(TestConstants.VULNERABILITY_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper vulnerabilityNotification = createNotification(unknownProvider, vulnerabilityContent, NotificationType.VULNERABILITY);

        final List<AlertNotificationWrapper> notificationContentList = List.of(policyNotification, vulnerabilityNotification);

        final List<String> projects = List.of("bad-project");
        final List<String> notificationTypes = List.of(NotificationType.RULE_VIOLATION_CLEARED.name(), NotificationType.VULNERABILITY.name());
        final ConfigurationJobModel jobConfig = createCommonDistributionConfiguration(projects, notificationTypes);

        final ConfigurationAccessor spiedReader = Mockito.spy(jobConfigReader);
        Mockito.doReturn(List.of(jobConfig)).when(spiedReader).getAllJobs();

        final MessageContentAggregator messageContentAggregator = new MessageContentAggregator(spiedReader, providers, notificationFilter);
        final Map<ConfigurationJobModel, List<MessageContentGroup>> topicContentMap = messageContentAggregator.processNotifications(frequencyType, notificationContentList);

        assertTrue(topicContentMap.containsKey(jobConfig));
        assertTrue(topicContentMap.get(jobConfig).isEmpty());
    }

    @Test
    public void testJobProcessingNotificationTypeMismatch() throws Exception {
        final String unknownProvider = "unknown_provider";
        final FrequencyType frequencyType = FrequencyType.REAL_TIME;
        final String policyContent = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper policyNotification = createNotification(unknownProvider, policyContent, NotificationType.RULE_VIOLATION_CLEARED);

        final String vulnerabilityContent = getNotificationContentFromFile(TestConstants.VULNERABILITY_NOTIFICATION_JSON_PATH);
        final AlertNotificationWrapper vulnerabilityNotification = createNotification(unknownProvider, vulnerabilityContent, NotificationType.VULNERABILITY);

        final List<AlertNotificationWrapper> notificationContentList = List.of(policyNotification, vulnerabilityNotification);

        final List<String> projects = List.of("bad-project");
        final List<String> notificationTypes = List.of(NotificationType.RULE_VIOLATION_CLEARED.name(), NotificationType.VULNERABILITY.name());
        final ConfigurationJobModel jobConfig = createCommonDistributionConfiguration(projects, notificationTypes);

        final ConfigurationAccessor spiedReader = Mockito.spy(jobConfigReader);
        Mockito.doReturn(List.of(jobConfig)).when(spiedReader).getAllJobs();

        final MessageContentAggregator messageContentAggregator = new MessageContentAggregator(spiedReader, providers, notificationFilter);
        final Map<ConfigurationJobModel, List<MessageContentGroup>> topicContentMap = messageContentAggregator.processNotifications(frequencyType, notificationContentList);

        assertTrue(topicContentMap.containsKey(jobConfig));
        assertTrue(topicContentMap.get(jobConfig).isEmpty());
    }

    private ConfigurationJobModel createCommonDistributionConfiguration(final List<String> projectNames, final List<String> notificationTypes) {
        final List<ConfigurationFieldModel> hipChatDistributionFields = MockConfigurationModelFactory.createHipChatDistributionFields();
        hipChatDistributionFields.addAll(MockConfigurationModelFactory.createBlackDuckDistributionFields());
        final ConfigurationJobModel distributionJob = MockConfigurationModelFactory.createDistributionJob(hipChatDistributionFields);
        final ConfigurationFieldModel notificationType = distributionJob.getFieldAccessor().getField(ProviderDistributionUIConfig.KEY_NOTIFICATION_TYPES)
                                                             .orElse(ConfigurationFieldModel.create(ProviderDistributionUIConfig.KEY_NOTIFICATION_TYPES));
        notificationType.setFieldValues(notificationTypes);
        final ConfigurationFieldModel project = distributionJob.getFieldAccessor().getField(BlackDuckDistributionUIConfig.KEY_CONFIGURED_PROJECT).orElse(ConfigurationFieldModel.create(BlackDuckDistributionUIConfig.KEY_CONFIGURED_PROJECT));
        project.setFieldValues(projectNames);
        distributionJob.getFieldAccessor().addFields(Map.of(ProviderDistributionUIConfig.KEY_NOTIFICATION_TYPES, notificationType, BlackDuckDistributionUIConfig.KEY_CONFIGURED_PROJECT, project));
        return distributionJob;
    }

    private String getNotificationContentFromFile(final String notificationJsonFileName) throws Exception {
        final ClassPathResource classPathResource = new ClassPathResource(notificationJsonFileName);
        final File jsonFile = classPathResource.getFile();
        return FileUtils.readFileToString(jsonFile, Charset.defaultCharset());
    }

    private AlertNotificationWrapper createNotification(final String providerName, final String notificationContent, final NotificationType type) {
        return new NotificationContent(new Date(), providerName, new Date(), type.name(), notificationContent);
    }
}
