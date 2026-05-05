package com.fooddrinks.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 1, max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @Size(max = 10, message = "Phone must not exceed 10 characters")
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}
