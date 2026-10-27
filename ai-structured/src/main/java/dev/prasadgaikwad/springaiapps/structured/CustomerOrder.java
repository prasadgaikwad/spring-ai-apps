package dev.prasadgaikwad.springaiapps.structured;

import java.util.List;

/**
 * The target POJO for typed extraction: a structured order parsed from an unstructured
 * customer message, with line items the model must enumerate.
 */
public record CustomerOrder(
        @jakarta.validation.constraints.NotBlank String customerName,
        @jakarta.validation.constraints.NotBlank String product,
        @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(1000) int quantity,
        @jakarta.validation.constraints.NotBlank String shippingAddress,
        List<@jakarta.validation.constraints.NotBlank String> notes) {
}
