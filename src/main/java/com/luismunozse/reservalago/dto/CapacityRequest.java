package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CapacityRequest {

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 0, message = "La capacidad no puede ser negativa")
    private Integer capacity;
}
