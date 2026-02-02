package com.enit.satellite_platform.modules.community.controllers;

import com.enit.satellite_platform.modules.community.dto.CreatePublicationDto;
import com.enit.satellite_platform.modules.community.dto.PublicationResponseDto;
import com.enit.satellite_platform.modules.community.dto.UpdatePublicationDto;
import com.enit.satellite_platform.modules.community.entities.Publication.PublicationStatus;
import com.enit.satellite_platform.modules.community.services.PublicationService;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing publications in the community.
 */
@RestController
@RequestMapping("/api/community/publications")
@CrossOrigin(origins = "*")
@Tag(name = "Community Publications", description = "APIs for managing community publications")
public class PublicationController {

    private static final Logger logger = LoggerFactory.getLogger(PublicationController.class);

    @Autowired
    private PublicationService publicationService;

    private String getCurrentEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
    }

    @Operation(summary = "Create a new publication")
    @PostMapping
    public ResponseEntity<GenericResponse<PublicationResponseDto>> createPublication(
            @Valid @RequestBody CreatePublicationDto createDto) {
        try {
            String email = getCurrentEmail();
            PublicationResponseDto publication = publicationService.createPublication(createDto, email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse<>("SUCCESS", "Publication created successfully", publication));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error creating publication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error creating publication: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get all published publications")
    @GetMapping
    public ResponseEntity<GenericResponse<Page<PublicationResponseDto>>> getAllPublications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            String userEmail = null;
            try {
                userEmail = getCurrentEmail();
            } catch (Exception e) {
                // User not authenticated
            }
            
            Page<PublicationResponseDto> publications = publicationService.getAllPublished(pageable, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publications retrieved successfully", publications));
        } catch (Exception e) {
            logger.error("Error fetching publications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error fetching publications", null));
        }
    }

    @Operation(summary = "Get publication by ID")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse<PublicationResponseDto>> getPublicationById(@PathVariable String id) {
        try {
            String userEmail = null;
            try {
                userEmail = getCurrentEmail();
            } catch (Exception e) {
                // User not authenticated
            }
            
            PublicationResponseDto publication = publicationService.getPublicationById(id, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publication retrieved successfully", publication));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching publication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error fetching publication", null));
        }
    }

    @Operation(summary = "Update a publication")
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse<PublicationResponseDto>> updatePublication(
            @PathVariable String id,
            @Valid @RequestBody UpdatePublicationDto updateDto) {
        try {
            String email = getCurrentEmail();
            PublicationResponseDto publication = publicationService.updatePublication(id, updateDto, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publication updated successfully", publication));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error updating publication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error updating publication", null));
        }
    }

    @Operation(summary = "Delete a publication")
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse<Void>> deletePublication(@PathVariable String id) {
        try {
            String email = getCurrentEmail();
            publicationService.deletePublication(id, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publication deleted successfully", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error deleting publication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error deleting publication", null));
        }
    }

    @Operation(summary = "Like or unlike a publication")
    @PostMapping("/{id}/like")
    public ResponseEntity<GenericResponse<PublicationResponseDto>> toggleLike(@PathVariable String id) {
        try {
            String email = getCurrentEmail();
            PublicationResponseDto publication = publicationService.toggleLike(id, email);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Like toggled successfully", publication));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error toggling like", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error toggling like", null));
        }
    }

    @Operation(summary = "Search publications")
    @GetMapping("/search")
    public ResponseEntity<GenericResponse<Page<PublicationResponseDto>>> searchPublications(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            String userEmail = null;
            try { userEmail = getCurrentEmail(); } catch (Exception e) {}
            
            Page<PublicationResponseDto> publications = publicationService.searchPublications(query, pageable, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Search completed", publications));
        } catch (Exception e) {
            logger.error("Error searching publications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error searching", null));
        }
    }

    @Operation(summary = "Get publications by tag")
    @GetMapping("/tag/{tag}")
    public ResponseEntity<GenericResponse<Page<PublicationResponseDto>>> getPublicationsByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            String userEmail = null;
            try { userEmail = getCurrentEmail(); } catch (Exception e) {}
            
            Page<PublicationResponseDto> publications = publicationService.getPublicationsByTag(tag, pageable, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publications retrieved", publications));
        } catch (Exception e) {
            logger.error("Error fetching publications by tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error fetching publications", null));
        }
    }

    @Operation(summary = "Get trending publications")
    @GetMapping("/trending")
    public ResponseEntity<GenericResponse<Page<PublicationResponseDto>>> getTrendingPublications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            String userEmail = null;
            try { userEmail = getCurrentEmail(); } catch (Exception e) {}
            
            Page<PublicationResponseDto> publications = publicationService.getTrendingPublications(pageable, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Trending publications retrieved", publications));
        } catch (Exception e) {
            logger.error("Error fetching trending publications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error fetching trending publications", null));
        }
    }

    @Operation(summary = "Get publications by author")
    @GetMapping("/author/{email}")
    public ResponseEntity<GenericResponse<Page<PublicationResponseDto>>> getPublicationsByAuthor(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            String userEmail = null;
            try { userEmail = getCurrentEmail(); } catch (Exception e) {}
            
            Page<PublicationResponseDto> publications = publicationService.getPublicationsByAuthor(email, pageable, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publications retrieved", publications));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching publications by author", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error fetching publications", null));
        }
    }

    @Operation(summary = "Get my publications")
    @GetMapping("/my")
    public ResponseEntity<GenericResponse<Page<PublicationResponseDto>>> getMyPublications(
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String email = getCurrentEmail();
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            Page<PublicationResponseDto> publications = publicationService.getMyPublications(email, status, pageable);
            return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Publications retrieved", publications));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("FAILURE", e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching user publications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("FAILURE", "Error fetching publications", null));
        }
    }
}
