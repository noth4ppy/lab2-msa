package io.hexlet.spring.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update: a field absent from the JSON stays JsonNullable.undefined() and is not changed.
 * Constraints apply to the wrapped value only when the field is present.
 */
@Getter
@Setter
public class BookUpdateDTO {

    private JsonNullable<@NotNull Long> authorId = JsonNullable.undefined();

    private JsonNullable<@NotBlank @Size(max = 255) String> slug = JsonNullable.undefined();

    private JsonNullable<@NotBlank @Size(max = 255) String> title = JsonNullable.undefined();

    private JsonNullable<String> description = JsonNullable.undefined();

    private JsonNullable<@NotNull @Min(1000) @Max(2100) Integer> publishedYear = JsonNullable.undefined();
}
