package dev.prasadgaikwad.springaiapps.agents;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused unit tests for the deterministic local tools — pure logic exercised directly
 * (the module's integration seam covers the agent flow end-to-end).
 */
class CalculatorToolsTest {

    private final CalculatorTools tools = new CalculatorTools();

    @Test
    void addAndMultiply() {
        assertThat(tools.add(2, 3)).isEqualTo(5);
        assertThat(tools.multiply(4, 5)).isEqualTo(20);
    }

    @Test
    void convertDistanceBothWays() {
        assertThat(tools.convertUnit(10, "km", "mi")).isCloseTo(6.21371, org.assertj.core.data.Offset.offset(1e-4));
        assertThat(tools.convertUnit(10, "miles", "kilometers")).isCloseTo(16.0934, org.assertj.core.data.Offset.offset(1e-3));
    }

    @Test
    void convertMass() {
        assertThat(tools.convertUnit(1, "kg", "lb")).isCloseTo(2.20462, org.assertj.core.data.Offset.offset(1e-4));
    }

    @Test
    void rejectsIncompatibleUnits() {
        assertThatThrownBy(() -> tools.convertUnit(1, "km", "kg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countDaysBetweenDates() {
        assertThat(tools.daysBetween("2024-01-01", "2024-01-31")).isEqualTo(30);
    }

    @Test
    void listToolNames() {
        assertThat(tools.toolNames()).containsExactly("add", "multiply", "convertUnit", "daysBetween");
    }
}
