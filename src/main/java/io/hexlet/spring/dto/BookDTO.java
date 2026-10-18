package io.hexlet.spring.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookDTO {

    private Long id;
    private Long authorId;
    private String slug;
    private String title;
    private String description;
    private Integer publishedYear;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
