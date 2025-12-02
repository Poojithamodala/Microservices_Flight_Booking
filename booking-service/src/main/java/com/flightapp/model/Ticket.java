package com.flightapp.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Document(collection = "tickets")
public class Ticket {

	@Id
	private String id;

	@NotBlank
	private String pnr;

	@NotBlank
	private String userEmail;

	@NotBlank
	private String departureFlightId;

	private String returnFlightId;

	@NotNull
	private FLIGHTTYPE tripType;

	@NotNull
	private LocalDateTime bookingTime;

	private String seatsBooked;

	private String mealType;

	@Min(0)
	private Double totalPrice;

	private boolean canceled;

	@Transient
	private List<Passenger> passengers;
}
