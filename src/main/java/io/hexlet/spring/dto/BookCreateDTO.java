package io.hexlet.spring.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookCreateDTO {

    @NotNull
    private Long authorId;

    @NotNull
    @NotBlank
    @Size(max = 255)
    private String slug;

    @NotNull
    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull
    @Min(1000)
    @Max(2100)
    private Integer publishedYear;
}
