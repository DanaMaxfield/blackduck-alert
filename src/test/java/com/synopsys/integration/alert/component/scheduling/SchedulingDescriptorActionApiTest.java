package com.synopsys.integration.alert.component.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.alert.common.descriptor.config.context.DescriptorActionApi;
import com.synopsys.integration.alert.common.enumeration.ConfigContextEnum;
import com.synopsys.integration.alert.web.model.FieldModel;
import com.synopsys.integration.alert.web.model.FieldValueModel;
import com.synopsys.integration.exception.IntegrationException;

public class SchedulingDescriptorActionApiTest {
    private static final FieldValueModel FIELD_HOUR_OF_DAY = new FieldValueModel(new ArrayList<>(), false);
    private static final FieldValueModel FIELD_PURGE_FREQUENCY = new FieldValueModel(new ArrayList<>(), false);
    private static final Map<String, FieldValueModel> FIELD_MAP = Map.of(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY, FIELD_HOUR_OF_DAY,
        SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS, FIELD_PURGE_FREQUENCY);
    private static final FieldModel FIELD_MODEL = new FieldModel(SchedulingDescriptor.SCHEDULING_COMPONENT, ConfigContextEnum.GLOBAL.name(), FIELD_MAP);
    private final SchedulingUIConfig schedulingUIConfig = new SchedulingUIConfig();

    @AfterEach
    public void cleanup() {
        FIELD_HOUR_OF_DAY.setValue("");
        FIELD_PURGE_FREQUENCY.setValue("");
    }

    @Test
    public void validateConfigWithNoErrorsTest() {
        final Map<String, String> fieldErrors = new HashMap<>();
        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();

        FIELD_HOUR_OF_DAY.setValue("1");
        FIELD_PURGE_FREQUENCY.setValue("1");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);
        assertEquals(null, fieldErrors.get(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY));
        assertEquals(null, fieldErrors.get(SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS));
    }

    //    @Test
    //    public void validateConfigHasErrorWhenNullTest() {
    //        final Map<String, String> fieldErrors = new HashMap<>();
    //        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();
    //
    //        FIELD_HOUR_OF_DAY.setValue(null);
    //        FIELD_PURGE_FREQUENCY.setValue(null);
    //        actionApi.validateConfig(FIELD_MODEL, fieldErrors);
    //        assertEquals("Must be a number between 0 and 23", fieldErrors.get(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY));
    //        assertEquals("Must be a number between 1 and 7", fieldErrors.get(SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS));
    //    }

    @Test
    public void validateConfigHasErrorWhenEmptyStringTest() {
        final Map<String, String> fieldErrors = new HashMap<>();
        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();

        FIELD_HOUR_OF_DAY.setValue("");
        FIELD_PURGE_FREQUENCY.setValue("");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);
        assertEquals(DescriptorActionApi.REQUIRED_FIELD_MISSING, fieldErrors.get(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY));
        assertEquals(DescriptorActionApi.REQUIRED_FIELD_MISSING, fieldErrors.get(SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS));
    }

    @Test
    public void validateConfigHasErrorWhenValuesNotNumericTest() {
        final Map<String, String> fieldErrors = new HashMap<>();
        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();

        FIELD_HOUR_OF_DAY.setValue("not a number");
        FIELD_PURGE_FREQUENCY.setValue("not a number");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);
        assertEquals("Must be a number between 0 and 23", fieldErrors.get(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY));
        assertEquals("Must be a number between 1 and 7", fieldErrors.get(SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS));
    }

    @Test
    public void validateConfigHasErrorWhenHourOutOfRangeTest() {
        final Map<String, String> fieldErrors = new HashMap<>();
        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();

        FIELD_HOUR_OF_DAY.setValue("-1");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);
        assertEquals("Must be a number between 0 and 23", fieldErrors.get(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY));

        fieldErrors.clear();
        FIELD_HOUR_OF_DAY.setValue("24");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);
        assertEquals("Must be a number between 0 and 23", fieldErrors.get(SchedulingUIConfig.KEY_DAILY_DIGEST_HOUR_OF_DAY));
    }

    @Test
    public void validateConfigHasErrorWhenPurgeFrequencyOutOfRangeTest() {
        final Map<String, String> fieldErrors = new HashMap<>();
        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();

        FIELD_PURGE_FREQUENCY.setValue("0");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);
        assertEquals("Must be a number between 1 and 7", fieldErrors.get(SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS));

        fieldErrors.clear();
        FIELD_PURGE_FREQUENCY.setValue("8");
        actionApi.validateConfig(schedulingUIConfig.createFields(), FIELD_MODEL, fieldErrors);

        assertEquals("Must be a number between 1 and 7", fieldErrors.get(SchedulingUIConfig.KEY_PURGE_DATA_FREQUENCY_DAYS));
    }

    @Test
    public void testConfigTest() {
        final SchedulingDescriptorActionApi actionApi = new SchedulingDescriptorActionApi();
        try {
            actionApi.testConfig(schedulingUIConfig.createFields(), null);
            fail("Expected exception to be thrown");
        } catch (final IntegrationException e) {
            assertEquals("Should not be implemented", e.getMessage());
        }
    }
}
