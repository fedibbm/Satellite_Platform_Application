package com.enit.satellite_platform.modules.community.dto;

import com.enit.satellite_platform.modules.community.entities.Publication.PublicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for creating a new publication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePublicationDto {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Content is required")
    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    private String featuredImage;

    private Integer readingTime;
}
