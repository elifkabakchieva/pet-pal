package com.petpal.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProviderRequest(@NotBlank @Size(min = 5, max = 500) String note) {
}
