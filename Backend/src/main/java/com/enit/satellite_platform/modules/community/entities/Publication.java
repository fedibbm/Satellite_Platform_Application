package com.enit.satellite_platform.modules.community.entities;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.shared.model.SoftDeletable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a publication in the satellite platform community.
 * A publication is created by a user to share research, analysis, or insights
 * with the community. It contains rich text content, metadata, and engagement features.
 */
@CompoundIndexes({
    @CompoundIndex(name = "author_createdAt", def = "{'author': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "status_createdAt", def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "tags_createdAt", def = "{'tags': 1, 'createdAt': -1}")
})
@Document(collection = "publications")
@Builder
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Publication implements SoftDeletable {

    /**
     * The unique identifier of the publication.
     */
    @Id
    private ObjectId id;

    /**
     * The user who authored the publication.
     */
    @DBRef
    @Field("author")
    private User author;

    /**
     * The title of the publication.
     */
    @Field("title")
    @Indexed
    private String title;

    /**
     * A brief description or summary of the publication.
     */
    @Field("description")
    private String description;

    /**
     * The main content of the publication in rich text format (HTML/Markdown).
     */
    @Field("content")
    private String content;

    /**
     * Tags associated with the publication for categorization.
     */
    @Field("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * The publication status (DRAFT, PUBLISHED, ARCHIVED).
     */
    @Field("status")
    @Builder.Default
    private PublicationStatus status = PublicationStatus.DRAFT;

    /**
     * Number of views the publication has received.
     */
    @Field("viewCount")
    @Builder.Default
    private Long viewCount = 0L;

    /**
     * Number of likes/upvotes the publication has received.
     */
    @Field("likeCount")
    @Builder.Default
    private Long likeCount = 0L;

    /**
     * List of user IDs who have liked this publication.
     */
    @Field("likedBy")
    @Builder.Default
    private List<String> likedBy = new ArrayList<>();

    /**
     * Number of comments on the publication.
     */
    @Field("commentCount")
    @Builder.Default
    private Long commentCount = 0L;

    /**
     * Featured image URL or path for the publication.
     */
    @Field("featuredImage")
    private String featuredImage;

    /**
     * Estimated reading time in minutes.
     */
    @Field("readingTime")
    private Integer readingTime;

    /**
     * The date and time when the publication was created.
     */
    @Field("createdAt")
    @CreatedDate
    private Date createdAt;

    /**
     * The date and time when the publication was last updated.
     */
    @Field("updatedAt")
    @LastModifiedDate
    private Date updatedAt;

    /**
     * The date and time when the publication was published.
     */
    @Field("publishedAt")
    private Date publishedAt;

    /**
     * Indicates whether the publication is soft deleted.
     */
    @Field("deleted")
    @Builder.Default
    private boolean deleted = false;

    /**
     * The date and time when the publication was deleted.
     */
    @Field("deletedAt")
    private Date deletedAt;

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public Date getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Enum representing the status of a publication.
     */
    public enum PublicationStatus {
        DRAFT,      // Publication is being drafted
        PUBLISHED,  // Publication is live and visible to community
        ARCHIVED    // Publication has been archived
    }
}
