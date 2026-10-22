package dev.prasadgaikwad.springaiapps.agents;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * A small, deterministic toolbox exposed to the model as {@code @Tool}-annotated
 * methods.
 *
 * <p>These are the "plain Tool calling" leg of the demo: Spring AI reflects over the
 * annotations to describe each tool to the model and routes the model's tool calls
 * back here. Operations are pure and in-memory so the agent's behaviour is testable
 * offline.
 */
@Component
public class CalculatorTools {

    private final Map<String, String> unitAliases = Map.ofEntries(
            Map.entry("km", "km"), Map.entry("kilometer", "km"), Map.entry("kilometers", "km"),
            Map.entry("mi", "mi"), Map.entry("mile", "mi"), Map.entry("miles", "mi"),
            Map.entry("kg", "kg"), Map.entry("kilogram", "kg"), Map.entry("kilograms", "kg"),
            Map.entry("lb", "lb"), Map.entry("pound", "lb"), Map.entry("pounds", "lb"));

    private static final Map<String, Double> DISTANCE_IN_KM = Map.of("km", 1.0, "mi", 1.609344);
    private static final Map<String, Double> MASS_IN_KG = Map.of("kg", 1.0, "lb", 0.45359237);

    @Tool(name = "add", description = "Add two numbers and return their sum.")
    public double add(@ToolParam(description = "the first addend") double a,
                      @ToolParam(description = "the second addend") double b) {
        return a + b;
    }

    @Tool(name = "multiply", description = "Multiply two numbers and return their product.")
    public double multiply(@ToolParam(description = "the first factor") double a,
                           @ToolParam(description = "the second factor") double b) {
        return a * b;
    }

    @Tool(name = "convertUnit", description = "Convert a value between kilometres and miles, or between kilograms and pounds.")
    public double convertUnit(@ToolParam(description = "the numeric value to convert") double value,
                              @ToolParam(description = "the source unit, e.g. km, mi, kg, lb") String from,
                              @ToolParam(description = "the target unit, e.g. km, mi, kg, lb") String to) {
        String fromCanonical = canonical(from);
        String toCanonical = canonical(to);
        if (isDistance(fromCanonical) && isDistance(toCanonical)) {
            return value * DISTANCE_IN_KM.get(fromCanonical) / DISTANCE_IN_KM.get(toCanonical);
        }
        if (isMass(fromCanonical) && isMass(toCanonical)) {
            return value * MASS_IN_KG.get(fromCanonical) / MASS_IN_KG.get(toCanonical);
        }        throw new IllegalArgumentException("Incompatible units: " + from + " -> " + to);
    }

    @Tool(name = "daysBetween", description = "Return the whole number of days between two ISO-8601 dates (yyyy-MM-dd).")
    public long daysBetween(@ToolParam(description = "the earlier date, yyyy-MM-dd") String start,
                            @ToolParam(description = "the later date, yyyy-MM-dd") String end) {
        return ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end));
    }

    /** Lists the tool names currently exposed, for discovery/introspection. */
    public List<String> toolNames() {
        return List.of("add", "multiply", "convertUnit", "daysBetween");
    }

    private String canonical(String unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be null");
        }
        String key = unit.trim().toLowerCase();
        String canonical = unitAliases.get(key);
        if (canonical == null) {
            throw new IllegalArgumentException("Unsupported unit: " + unit);
        }
        return canonical;
    }

    private boolean isDistance(String unit) {
        return "km".equals(unit) || "mi".equals(unit);
    }

    private boolean isMass(String unit) {
        return "kg".equals(unit) || "lb".equals(unit);
    }
}
