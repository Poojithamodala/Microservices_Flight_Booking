package com.flightapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Document
public class Passenger {

    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String gender;

    @NotNull
    @Min(1)
    private Integer age;

    @NotBlank
    private String seatNumber;

    private String mealPreference;

    private String ticketId;
}
