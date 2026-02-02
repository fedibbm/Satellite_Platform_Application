package com.enit.satellite_platform.modules.community.services;

import com.enit.satellite_platform.modules.community.dto.CreatePublicationDto;
import com.enit.satellite_platform.modules.community.dto.PublicationResponseDto;
import com.enit.satellite_platform.modules.community.dto.UpdatePublicationDto;
import com.enit.satellite_platform.modules.community.entities.Publication;
import com.enit.satellite_platform.modules.community.entities.Publication.PublicationStatus;
import com.enit.satellite_platform.modules.community.repositories.PublicationRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Service class for managing publications.
 * Provides methods for creating, retrieving, updating, deleting publications,
 * and handling engagement features like likes and views.
 */
@Service
public class PublicationService {

    private static final Logger logger = LoggerFactory.getLogger(PublicationService.class);

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a new publication.
     */
    @Transactional
    public PublicationResponseDto createPublication(CreatePublicationDto createDto, String email) {
        logger.info("Creating publication with title: {} for user: {}", createDto.getTitle(), email);

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Publication publication = Publication.builder()
                .author(author)
                .title(createDto.getTitle())
                .description(createDto.getDescription())
                .content(createDto.getContent())
                .tags(createDto.getTags())
                .status(createDto.getStatus())
                .featuredImage(createDto.getFeaturedImage())
                .readingTime(createDto.getReadingTime())
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .createdAt(new Date())
                .updatedAt(new Date())
                .deleted(false)
                .build();

        if (publication.getStatus() == PublicationStatus.PUBLISHED) {
            publication.setPublishedAt(new Date());
        }

        Publication savedPublication = publicationRepository.save(publication);
        logger.info("Publication created successfully with ID: {}", savedPublication.getId());

        return convertToResponseDto(savedPublication, email);
    }

    /**
     * Gets all published publications.
     */
    public Page<PublicationResponseDto> getAllPublished(Pageable pageable, String userEmail) {
        logger.info("Fetching all published publications");
        Page<Publication> publications = publicationRepository.findAllPublished(pageable);
        return publications.map(pub -> convertToResponseDto(pub, userEmail));
    }

    /**
     * Gets publications by author.
     */
    public Page<PublicationResponseDto> getPublicationsByAuthor(String authorEmail, Pageable pageable, String userEmail) {
        logger.info("Fetching publications for author: {}", authorEmail);
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Author not found: " + authorEmail));
        Page<Publication> publications = publicationRepository.findByAuthorIdAndDeletedFalse(author.getId().toString(), pageable);
        return publications.map(pub -> convertToResponseDto(pub, userEmail));
    }

    /**
     * Gets a publication by ID and increments view count.
     */
    @Transactional
    public PublicationResponseDto getPublicationById(String publicationId, String userEmail) {
        logger.info("Fetching publication: {}", publicationId);
        Publication publication = publicationRepository.findByIdAndDeletedFalse(new ObjectId(publicationId))
                .orElseThrow(() -> new RuntimeException("Publication not found: " + publicationId));
        publication.setViewCount(publication.getViewCount() + 1);
        publicationRepository.save(publication);
        return convertToResponseDto(publication, userEmail);
    }

    /**
     * Updates a publication.
     */
    @Transactional
    public PublicationResponseDto updatePublication(String publicationId, UpdatePublicationDto updateDto, String email) {
        logger.info("Updating publication: {}", publicationId);
        Publication publication = publicationRepository.findByIdAndDeletedFalse(new ObjectId(publicationId))
                .orElseThrow(() -> new RuntimeException("Publication not found: " + publicationId));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!publication.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only update your own publications");
        }

        if (updateDto.getTitle() != null) publication.setTitle(updateDto.getTitle());
        if (updateDto.getDescription() != null) publication.setDescription(updateDto.getDescription());
        if (updateDto.getContent() != null) publication.setContent(updateDto.getContent());
        if (updateDto.getTags() != null) publication.setTags(updateDto.getTags());
        if (updateDto.getFeaturedImage() != null) publication.setFeaturedImage(updateDto.getFeaturedImage());
        if (updateDto.getReadingTime() != null) publication.setReadingTime(updateDto.getReadingTime());
        if (updateDto.getStatus() != null) {
            PublicationStatus oldStatus = publication.getStatus();
            publication.setStatus(updateDto.getStatus());
            if (oldStatus != PublicationStatus.PUBLISHED && updateDto.getStatus() == PublicationStatus.PUBLISHED) {
                publication.setPublishedAt(new Date());
            }
        }

        publication.setUpdatedAt(new Date());
        Publication updatedPublication = publicationRepository.save(publication);
        return convertToResponseDto(updatedPublication, email);
    }

    /**
     * Deletes a publication (soft delete).
     */
    @Transactional
    public void deletePublication(String publicationId, String email) {
        logger.info("Deleting publication: {}", publicationId);
        Publication publication = publicationRepository.findByIdAndDeletedFalse(new ObjectId(publicationId))
                .orElseThrow(() -> new RuntimeException("Publication not found: " + publicationId));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!publication.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only delete your own publications");
        }

        publication.setDeleted(true);
        publication.setDeletedAt(new Date());
        publicationRepository.save(publication);
    }

    /**
     * Toggles like on a publication.
     */
    @Transactional
    public PublicationResponseDto toggleLike(String publicationId, String email) {
        logger.info("Toggling like on publication: {}", publicationId);
        Publication publication = publicationRepository.findByIdAndDeletedFalse(new ObjectId(publicationId))
                .orElseThrow(() -> new RuntimeException("Publication not found: " + publicationId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        String userId = user.getId().toString();
        if (publication.getLikedBy().contains(userId)) {
            publication.getLikedBy().remove(userId);
            publication.setLikeCount(publication.getLikeCount() - 1);
        } else {
            publication.getLikedBy().add(userId);
            publication.setLikeCount(publication.getLikeCount() + 1);
        }

        Publication updated = publicationRepository.save(publication);
        return convertToResponseDto(updated, email);
    }

    /**
     * Searches publications by title.
     */
    public Page<PublicationResponseDto> searchPublications(String query, Pageable pageable, String userEmail) {
        Page<Publication> publications = publicationRepository.searchByTitle(query, pageable);
        return publications.map(pub -> convertToResponseDto(pub, userEmail));
    }

    /**
     * Gets publications by tag.
     */
    public Page<PublicationResponseDto> getPublicationsByTag(String tag, Pageable pageable, String userEmail) {
        Page<Publication> publications = publicationRepository.findByTagAndPublished(tag, pageable);
        return publications.map(pub -> convertToResponseDto(pub, userEmail));
    }

    /**
     * Gets trending publications.
     */
    public Page<PublicationResponseDto> getTrendingPublications(Pageable pageable, String userEmail) {
        Page<Publication> publications = publicationRepository.findTrending(pageable);
        return publications.map(pub -> convertToResponseDto(pub, userEmail));
    }

    /**
     * Gets current user's publications.
     */
    public Page<PublicationResponseDto> getMyPublications(String email, PublicationStatus status, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        Page<Publication> publications;
        if (status != null) {
            publications = publicationRepository.findByAuthorIdAndStatus(user.getId().toString(), status, pageable);
        } else {
            publications = publicationRepository.findByAuthorIdAndDeletedFalse(user.getId().toString(), pageable);
        }
        return publications.map(pub -> convertToResponseDto(pub, email));
    }

    private PublicationResponseDto convertToResponseDto(Publication publication, String userEmail) {
        boolean isLiked = false;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                isLiked = publication.getLikedBy().contains(user.getId().toString());
            }
        }

        return PublicationResponseDto.builder()
                .id(publication.getId().toString())
                .author(PublicationResponseDto.AuthorDto.builder()
                        .id(publication.getAuthor().getId().toString())
                        .name(publication.getAuthor().getName())
                        .email(publication.getAuthor().getEmail())
                        .build())
                .title(publication.getTitle())
                .description(publication.getDescription())
                .content(publication.getContent())
                .tags(publication.getTags())
                .status(publication.getStatus())
                .viewCount(publication.getViewCount())
                .likeCount(publication.getLikeCount())
                .commentCount(publication.getCommentCount())
                .featuredImage(publication.getFeaturedImage())
                .readingTime(publication.getReadingTime())
                .createdAt(publication.getCreatedAt())
                .updatedAt(publication.getUpdatedAt())
                .publishedAt(publication.getPublishedAt())
                .isLikedByCurrentUser(isLiked)
                .build();
    }
}
