package com.sunset.eveil.feature.mapping.presentation.dto;

import com.sunset.eveil.feature.mapping.exception.EmptyMappingValueException;
import com.sunset.eveil.global.mask.MaskingType;
import jakarta.validation.constraints.NotNull;

public record UpdateMappingTypeRequest(String value, @NotNull MaskingType type) {

	public UpdateMappingTypeRequest {
		if (value == null || value.isBlank() || value.trim().isEmpty()) {
			throw new EmptyMappingValueException();
		}
		value = value.trim();
	}
}
