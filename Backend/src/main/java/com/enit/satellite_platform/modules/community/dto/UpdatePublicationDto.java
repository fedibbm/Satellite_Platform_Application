package com.enit.satellite_platform.modules.community.dto;

import com.enit.satellite_platform.modules.community.entities.Publication.PublicationStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for updating an existing publication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePublicationDto {

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;

    private List<String> tags;

    private PublicationStatus status;

    private String featuredImage;

    private Integer readingTime;
}
