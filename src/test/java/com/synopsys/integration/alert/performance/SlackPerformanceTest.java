package com.synopsys.integration.alert.performance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.synopsys.integration.alert.channel.slack.descriptor.SlackDescriptor;
import com.synopsys.integration.alert.common.descriptor.ProviderDescriptor;
import com.synopsys.integration.alert.common.descriptor.config.ui.ChannelDistributionUIConfig;
import com.synopsys.integration.alert.common.descriptor.config.ui.ProviderDistributionUIConfig;
import com.synopsys.integration.alert.common.enumeration.ConfigContextEnum;
import com.synopsys.integration.alert.common.enumeration.FrequencyType;
import com.synopsys.integration.alert.common.enumeration.ProcessingType;
import com.synopsys.integration.alert.common.rest.model.FieldModel;
import com.synopsys.integration.alert.common.rest.model.FieldValueModel;
import com.synopsys.integration.alert.common.rest.model.JobFieldModel;
import com.synopsys.integration.alert.performance.model.SlackPerformanceProperties;
import com.synopsys.integration.exception.IntegrationException;

public class SlackPerformanceTest extends IntegrationPerformanceTest {
    private final static String SLACK_PERFORMANCE_JOB_NAME = "Slack Performance Job";
    private final SlackPerformanceProperties slackProperties = new SlackPerformanceProperties();

    @Test
    @Ignore
    public void testSlackJob() throws Exception {
        runTest();
    }

    @Override
    public String createJob() throws IntegrationException {
        Map<String, FieldValueModel> providerKeyToValues = new HashMap<>();
        providerKeyToValues.put(ProviderDescriptor.KEY_PROVIDER_CONFIG_ID, new FieldValueModel(List.of(getBlackDuckProviderID()), true));
        providerKeyToValues.put(ProviderDistributionUIConfig.KEY_NOTIFICATION_TYPES, new FieldValueModel(List.of("BOM_EDIT", "POLICY_OVERRIDE", "RULE_VIOLATION", "RULE_VIOLATION_CLEARED", "VULNERABILITY"), true));
        providerKeyToValues.put(ProviderDistributionUIConfig.KEY_PROCESSING_TYPE, new FieldValueModel(List.of(ProcessingType.DEFAULT.name()), true));
        providerKeyToValues.put(ProviderDistributionUIConfig.KEY_FILTER_BY_PROJECT, new FieldValueModel(List.of("true"), true));
        providerKeyToValues.put(ProviderDistributionUIConfig.KEY_CONFIGURED_PROJECT, new FieldValueModel(List.of(getBlackDuckProperties().getBlackDuckProjectName()), true));
        FieldModel jobProviderConfiguration = new FieldModel(getBlackDuckProperties().getBlackDuckProviderKey(), ConfigContextEnum.DISTRIBUTION.name(), providerKeyToValues);

        Map<String, FieldValueModel> slackKeyToValues = new HashMap<>();
        slackKeyToValues.put(ChannelDistributionUIConfig.KEY_ENABLED, new FieldValueModel(List.of("true"), true));
        slackKeyToValues.put(ChannelDistributionUIConfig.KEY_CHANNEL_NAME, new FieldValueModel(List.of(slackProperties.getSlackChannelKey()), true));
        slackKeyToValues.put(ChannelDistributionUIConfig.KEY_NAME, new FieldValueModel(List.of(getJobName()), true));
        slackKeyToValues.put(ChannelDistributionUIConfig.KEY_FREQUENCY, new FieldValueModel(List.of(FrequencyType.REAL_TIME.name()), true));
        slackKeyToValues.put(ChannelDistributionUIConfig.KEY_PROVIDER_NAME, new FieldValueModel(List.of(getBlackDuckProperties().getBlackDuckProviderKey()), true));

        slackKeyToValues.put(SlackDescriptor.KEY_WEBHOOK, new FieldValueModel(List.of(slackProperties.getSlackChannelWebhook()), true));
        slackKeyToValues.put(SlackDescriptor.KEY_CHANNEL_NAME, new FieldValueModel(List.of(slackProperties.getSlackChannelName()), true));
        slackKeyToValues.put(SlackDescriptor.KEY_CHANNEL_USERNAME, new FieldValueModel(List.of(slackProperties.getSlackChannelUsername()), true));

        FieldModel jobSlackConfiguration = new FieldModel(slackProperties.getSlackChannelKey(), ConfigContextEnum.DISTRIBUTION.name(), slackKeyToValues);

        JobFieldModel jobFieldModel = new JobFieldModel(null, Set.of(jobSlackConfiguration, jobProviderConfiguration));

        String jobConfigBody = getGson().toJson(jobFieldModel);

        getAlertRequestUtility().executePostRequest("/api/configuration/job/validate", jobConfigBody, String.format("Validating the Job %s failed.", getJobName()));
        getAlertRequestUtility().executePostRequest("/api/configuration/job/test", jobConfigBody, String.format("Testing the Job %s failed.", getJobName()));
        String creationResponse = getAlertRequestUtility().executePostRequest("/api/configuration/job", jobConfigBody, String.format("Could not create the Job %s.", getJobName()));

        JsonObject jsonObject = getGson().fromJson(creationResponse, JsonObject.class);
        return jsonObject.get("jobId").getAsString();
    }

    @Override
    public String getJobName() {
        return SLACK_PERFORMANCE_JOB_NAME;
    }

}
