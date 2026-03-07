package main.java.edu.cit.uy.researchcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private Long id;
    private String email;
    private String firstname;
    private String lastname;
    private String role;
    private String createdAt;
    private String accessToken;
    private String refreshToken;
}
