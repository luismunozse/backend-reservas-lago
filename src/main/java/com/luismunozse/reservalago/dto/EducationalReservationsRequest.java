package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EducationalReservationsRequest {

    @NotNull(message = "El campo 'enabled' es obligatorio")
    private Boolean enabled;
}
