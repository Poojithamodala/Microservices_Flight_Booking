package com.flightapp.controller;

import com.flightapp.model.FLIGHTTYPE;
import com.flightapp.model.Passenger;
import com.flightapp.model.Ticket;
import com.flightapp.service.BookingService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flight")
public class BookingController {

    private final BookingService bookingService;

    @Data
    public static class BookingRequest {
        private String userEmail;
        private String returnFlightId;
        private FLIGHTTYPE tripType;
        private List<@Valid Passenger> passengers;
    }

//    @PostMapping("/booking/{flightId}")
//    public Mono<ResponseEntity<String>> bookTicket(@PathVariable("flightId") String departureFlightId,
//                                                   @RequestBody BookingRequest request) {
//        return bookingService.bookTicket(
//                        request.getUserEmail(),
//                        departureFlightId,
//                        request.getReturnFlightId(),
//                        request.getPassengers(),
//                        request.getTripType())
//                .map(pnr -> ResponseEntity.ok("PNR: " + pnr));
//    }
    @PostMapping("/booking/{departureFlightId}")
    public Mono<String> bookTicket(@PathVariable String departureFlightId,
                                   @RequestBody BookingRequest request) {
        return bookingService.bookTicket(
                request.getUserEmail(),
                departureFlightId,
                request.getReturnFlightId(),
                request.getPassengers(),
                request.getTripType()
        );
    }

    @GetMapping("/ticket/{pnr}")
    public Mono<Ticket> getTicket(@PathVariable String pnr) {
        return bookingService.getByPnr(pnr);
    }

    @GetMapping("/booking/history/{emailId}")
    public Flux<Ticket> history(@PathVariable String emailId) {
        return bookingService.historyByEmail(emailId);
    }

    @DeleteMapping("/booking/cancel/{pnr}")
    public Mono<String> cancel(@PathVariable String pnr) {
        return bookingService.cancelByPnr(pnr);
    }
}
