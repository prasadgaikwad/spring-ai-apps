package dev.prasadgaikwad.springaiapps.agents;

import java.util.Arrays;
import java.util.List;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialects;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused validation of the tool input schemas that Spring AI generates and hands to the
 * networknt validator, plus the nullable/date-time semantics that changed in 3.0.8/3.0.9.
 */
class ToolSchemaValidationTest {

    @Test
    void generatedDaysBetweenSchemaAcceptsValidArgumentsAndRejectsWrongTypes() {
        ToolCallback callback = Arrays.stream(MethodToolCallbackProvider.builder()
                .toolObjects(new CalculatorTools())
                .build()
                .getToolCallbacks())
                .filter(tool -> tool.getToolDefinition().name().equals("daysBetween"))
                .findFirst()
                .orElseThrow();

        String schemaJson = callback.getToolDefinition().inputSchema();
        assertThat(schemaJson).contains("\"start\"", "\"end\"").doesNotContain("\"format\"");

        Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(schemaJson, InputFormat.JSON);
        assertThat(validate(schema, "{\"start\":\"2024-01-01\",\"end\":\"2024-01-31\"}")).isEmpty();
        assertThat(validate(schema, "{\"start\":1,\"end\":\"2024-01-31\"}")).isNotEmpty();
    }

    @Test
    void openApiNullableAppliesToTheValueAndNotToChildSchemas() {
        Schema schema = openApiSchema("""
                {
                  "type": "object",
                  "properties": {
                    "nickname": {"type": "string", "nullable": true},
                    "profile": {
                      "type": "object",
                      "nullable": true,
                      "properties": {"city": {"type": "string"}}
                    }
                  }
                }
                """);

        assertThat(validate(schema, "{\"nickname\":null,\"profile\":null}")).isEmpty();
        assertThat(validate(schema, "{\"nickname\":\"Ada\",\"profile\":{\"city\":null}}")).isNotEmpty();
    }

    @Test
    void dateTimeFormatRequiresRfc3339SeparatorAndOffset() {
        Schema schema = openApiSchema("{\"type\":\"string\",\"format\":\"date-time\"}");

        assertThat(validate(schema, "\"2026-11-23T12:34:56Z\"")).isEmpty();
        assertThat(validate(schema, "\"2026-11-23t12:34:56+01:00\"")).isEmpty();
        assertThat(validate(schema, "\"2026-11-23 12:34:56Z\"")).isNotEmpty();
        assertThat(validate(schema, "\"2016-12-31T23:59:60\"")).isNotEmpty();
    }

    private static Schema openApiSchema(String schemaJson) {
        return SchemaRegistry.withDialect(Dialects.getOpenApi30()).getSchema(schemaJson, InputFormat.JSON);
    }

    private static List<com.networknt.schema.Error> validate(Schema schema, String instanceJson) {
        return schema.validate(instanceJson, InputFormat.JSON,
                (context, schemaContext) -> context.executionConfig(
                        config -> config.formatAssertionsEnabled(true)));
    }
}
