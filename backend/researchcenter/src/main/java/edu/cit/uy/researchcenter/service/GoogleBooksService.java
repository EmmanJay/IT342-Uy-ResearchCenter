package edu.cit.uy.researchcenter.service;

import edu.cit.uy.researchcenter.dto.BookMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GoogleBooksService {

    @Value("${google.books.api-key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GOOGLE_BOOKS_URL = "https://www.googleapis.com/books/v1/volumes";

    public BookMetadataResponse searchByIsbn(String isbn) {
        return search("isbn:" + isbn);
    }

    public BookMetadataResponse searchByQuery(String query) {
        return search(query);
    }

    private BookMetadataResponse search(String q) {
        try {
            String response = webClient.get()
                    .uri(GOOGLE_BOOKS_URL + "?q=" + q + "&key=" + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No results from Google Books API");
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");

            if (items == null || items.size() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No book found");
            }

            JsonNode book = items.get(0).get("volumeInfo");
            return parseMetadata(book);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching Google Books API");
        }
    }

    private BookMetadataResponse parseMetadata(JsonNode volumeInfo) {
        BookMetadataResponse metadata = new BookMetadataResponse();

        // Title
        if (volumeInfo.has("title")) {
            metadata.setTitle(volumeInfo.get("title").asText());
        }

        // Authors
        if (volumeInfo.has("authors") && volumeInfo.get("authors").size() > 0) {
            metadata.setAuthor(volumeInfo.get("authors").get(0).asText());
        }

        // Publisher
        if (volumeInfo.has("publisher")) {
            metadata.setPublisher(volumeInfo.get("publisher").asText());
        }

        // Publication Year
        if (volumeInfo.has("publishedDate")) {
            String date = volumeInfo.get("publishedDate").asText();
            if (date.length() >= 4) {
                try {
                    metadata.setPublicationYear(Integer.parseInt(date.substring(0, 4)));
                } catch (NumberFormatException e) {
                    // Skip if parsing fails
                }
            }
        }

        // Description
        if (volumeInfo.has("description")) {
            metadata.setDescription(volumeInfo.get("description").asText());
        }

        // ISBN
        if (volumeInfo.has("industryIdentifiers")) {
            JsonNode identifiers = volumeInfo.get("industryIdentifiers");
            for (JsonNode identifier : identifiers) {
                if (identifier.get("type").asText().equals("ISBN_13")) {
                    metadata.setIsbn(identifier.get("identifier").asText());
                    break;
                }
            }
        }

        // Thumbnail
        if (volumeInfo.has("imageLinks") && volumeInfo.get("imageLinks").has("thumbnail")) {
            metadata.setThumbnail(volumeInfo.get("imageLinks").get("thumbnail").asText());
        }

        return metadata;
    }
}
