package com.flightapp.service.impl;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.flightapp.model.Flight;
import com.flightapp.repository.FlightRepository;
import com.flightapp.service.FlightService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FlightServiceImpl implements FlightService {

	private final FlightRepository flightRepository;

	public FlightServiceImpl(FlightRepository flightRepository) {
		this.flightRepository = flightRepository;
	}

	@Override
	public Mono<Flight> addFlight(Flight flight) {
	    return flightRepository
	            .findByAirlineAndFromPlaceAndToPlaceAndDepartureTime(
	                    flight.getAirline(),
	                    flight.getFromPlace(),
	                    flight.getToPlace(),
	                    flight.getDepartureTime()
	            )
	            .flatMap(existing -> 
	                    Mono.<Flight>error(new ResponseStatusException(HttpStatus.CONFLICT, "Flight already exists"))
	            )
	            .switchIfEmpty(flightRepository.save(flight));
	}


//    @Override
//    public Mono<Void> deleteFlight(String flightId) {
//        return flightRepository.deleteById(flightId);
//    }

//    @Override
//    public Mono<Flight> updateFlight(String id, Map<String, Object> updates) {
//        return flightRepository.findById(id)
//                .switchIfEmpty(Mono.error(new RuntimeException("Flight not found")))
//                .flatMap(flight -> {
//                    updates.forEach((key, value) -> {
//                        switch (key) {
//                            case "airline" -> flight.setAirline((String) value);
//                            case "fromPlace" -> flight.setFromPlace((String) value);
//                            case "toPlace" -> flight.setToPlace((String) value);
//                            case "departureTime" ->
//                                    flight.setDepartureTime(LocalDateTime.parse((String) value));
//                            case "arrivalTime" ->
//                                    flight.setArrivalTime(LocalDateTime.parse((String) value));
//                            case "price" ->
//                                    flight.setPrice(((Number) value).intValue());
//                            case "totalSeats" ->
//                                    flight.setTotalSeats(((Number) value).intValue());
//                            case "availableSeats" ->
//                                    flight.setAvailableSeats(((Number) value).intValue());
//                        }
//                    });
//                    return flightRepository.save(flight);
//                });
//    }

	@Override
	public Flux<Flight> getAllFlights() {
		return flightRepository.findAll();
	}

	@Override
	public Mono<Flight> searchFlightById(String flightId) {
		return flightRepository.findById(flightId).switchIfEmpty(Mono.error(new RuntimeException("Flight not found")));
	}

	@Override
	public Flux<Flight> searchFlights(String from, String to, LocalDateTime start, LocalDateTime end) {
	    return flightRepository.findByFromPlaceAndToPlaceAndDepartureTimeBetween(from, to, start, end);
	}

	@Override
	public Flux<Flight> searchFlightsByAirline(String fromPlace, String toPlace, String airline) {

		return flightRepository.findByFromPlaceAndToPlaceAndAirline(fromPlace, toPlace, airline);
	}
	
	@Override
    public Mono<Flight> reserveSeats(String flightId, int seatCount) {
        return flightRepository.findById(flightId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flight not found")))
                .flatMap(flight -> {
                    if (flight.getAvailableSeats() < seatCount) {
                        return Mono.error(new RuntimeException("Not enough seats"));
                    }
                    flight.setAvailableSeats(flight.getAvailableSeats() - seatCount);
                    return flightRepository.save(flight);
                });
    }

    @Override
    public Mono<Flight> releaseSeats(String flightId, int seatCount) {
        return flightRepository.findById(flightId)
                .switchIfEmpty(Mono.error(new RuntimeException("Flight not found")))
                .flatMap(flight -> {
                    flight.setAvailableSeats(flight.getAvailableSeats() + seatCount);
                    return flightRepository.save(flight);
                });
    }
}
