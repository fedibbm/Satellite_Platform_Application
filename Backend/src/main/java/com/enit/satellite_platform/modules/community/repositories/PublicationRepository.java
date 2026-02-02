package com.enit.satellite_platform.modules.community.repositories;

import com.enit.satellite_platform.modules.community.entities.Publication;
import com.enit.satellite_platform.modules.community.entities.Publication.PublicationStatus;
import com.enit.satellite_platform.shared.repository.SoftDeletableRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Publication} entities in MongoDB.
 * Provides methods for performing CRUD operations and custom queries on publications.
 * Extends SoftDeletableRepository to inherit soft delete query methods.
 */
@Repository
public interface PublicationRepository extends SoftDeletableRepository<Publication, ObjectId> {

    /**
     * Finds all publications by a specific author.
     *
     * @param authorId The String ID of the author.
     * @param pageable Pagination information.
     * @return A page of non-deleted publications by the author.
     */
    @Query(value = "{ 'author.$id': ?0, 'deleted': false }")
    Page<Publication> findByAuthorIdAndDeletedFalse(String authorId, Pageable pageable);

    /**
     * Finds all publications with a specific status.
     *
     * @param status   The publication status.
     * @param pageable Pagination information.
     * @return A page of non-deleted publications with the given status.
     */
    @Query(value = "{ 'status': ?0, 'deleted': false }")
    Page<Publication> findByStatusAndDeletedFalse(PublicationStatus status, Pageable pageable);

    /**
     * Finds all published publications (visible to community).
     *
     * @param pageable Pagination information.
     * @return A page of published non-deleted publications.
     */
    @Query(value = "{ 'status': 'PUBLISHED', 'deleted': false }")
    Page<Publication> findAllPublished(Pageable pageable);

    /**
     * Finds publications by tag.
     *
     * @param tag      The tag to search for.
     * @param pageable Pagination information.
     * @return A page of publications with the specified tag.
     */
    @Query(value = "{ 'tags': ?0, 'status': 'PUBLISHED', 'deleted': false }")
    Page<Publication> findByTagAndPublished(String tag, Pageable pageable);

    /**
     * Finds publications by title (case-insensitive search).
     *
     * @param title    The title to search for.
     * @param pageable Pagination information.
     * @return A page of publications matching the title.
     */
    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' }, 'status': 'PUBLISHED', 'deleted': false }")
    Page<Publication> searchByTitle(String title, Pageable pageable);

    /**
     * Finds publications by author and status.
     *
     * @param authorId The String ID of the author.
     * @param status   The publication status.
     * @param pageable Pagination information.
     * @return A page of publications.
     */
    @Query(value = "{ 'author.$id': ?0, 'status': ?1, 'deleted': false }")
    Page<Publication> findByAuthorIdAndStatus(String authorId, PublicationStatus status, Pageable pageable);

    /**
     * Finds a publication by ID and ensures it's not deleted.
     *
     * @param id The ObjectId of the publication.
     * @return An Optional containing the publication if found and not deleted.
     */
    @Query(value = "{ '_id': ?0, 'deleted': false }")
    Optional<Publication> findByIdAndDeletedFalse(ObjectId id);

    /**
     * Finds trending publications (most viewed/liked in recent time).
     *
     * @param pageable Pagination information.
     * @return A page of trending publications.
     */
    @Query(value = "{ 'status': 'PUBLISHED', 'deleted': false }", sort = "{ 'viewCount': -1, 'likeCount': -1 }")
    Page<Publication> findTrending(Pageable pageable);

    /**
     * Gets total publication count by author.
     *
     * @param authorId The String ID of the author.
     * @return The count of publications by the author.
     */
    @Query(value = "{ 'author.$id': ?0, 'deleted': false }", count = true)
    Long countByAuthorId(String authorId);
}
