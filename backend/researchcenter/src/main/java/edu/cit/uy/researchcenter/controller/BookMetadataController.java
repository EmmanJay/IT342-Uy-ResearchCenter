package edu.cit.uy.researchcenter.controller;

import edu.cit.uy.researchcenter.dto.ApiResponse;
import edu.cit.uy.researchcenter.dto.BookMetadataResponse;
import edu.cit.uy.researchcenter.service.GoogleBooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import edu.cit.uy.researchcenter.model.User;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookMetadataController {

    private final GoogleBooksService googleBooksService;

    /**
     * Fetch book metadata from Google Books API
     * Supports search by ISBN or by title/author query
     *
     * GET /api/v1/books/metadata?isbn=9780134494166
     * GET /api/v1/books/metadata?query=Clean Code Robert Martin
     */
    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata(@AuthenticationPrincipal User user,
                                         @RequestParam(required = false) String isbn,
                                         @RequestParam(required = false) String query) {
        
        if ((isbn == null || isbn.trim().isEmpty()) && 
            (query == null || query.trim().isEmpty())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BOOKS-002", "Either 'isbn' or 'query' parameter must be provided"));
        }

        BookMetadataResponse metadata;
        
        if (isbn != null && !isbn.trim().isEmpty()) {
            metadata = googleBooksService.searchByIsbn(isbn.trim());
        } else {
            metadata = googleBooksService.searchByQuery(query.trim());
        }

        return ResponseEntity.ok(ApiResponse.success(metadata));
    }
}
