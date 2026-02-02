package com.enit.satellite_platform.modules.community.dto;

import com.enit.satellite_platform.modules.community.entities.Publication.PublicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Data Transfer Object for publication response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationResponseDto {

    private String id;
    
    private AuthorDto author;
    
    private String title;
    
    private String description;
    
    private String content;
    
    private List<String> tags;
    
    private PublicationStatus status;
    
    private Long viewCount;
    
    private Long likeCount;
    
    private Long commentCount;
    
    private String featuredImage;
    
    private Integer readingTime;
    
    private Date createdAt;
    
    private Date updatedAt;
    
    private Date publishedAt;
    
    private boolean isLikedByCurrentUser;

    /**
     * Nested DTO for author information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private String id;
        private String name;
        private String email;
    }
}
