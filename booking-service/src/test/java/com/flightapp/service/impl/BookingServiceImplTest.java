package com.flightapp.service.impl;

import com.flightapp.dto.FlightDto;
import com.flightapp.messaging.BookingEvent;
import com.flightapp.model.FLIGHTTYPE;
import com.flightapp.model.Passenger;
import com.flightapp.model.Ticket;
import com.flightapp.repository.PassengerRepository;
import com.flightapp.repository.TicketRepository;
import com.flightapp.feign.FlightClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private FlightClient flightClient;

    @Mock
    private KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Passenger passenger;
    private FlightDto depFlight;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        passenger = new Passenger();
        passenger.setName("John Doe");
        passenger.setAge(30);
        passenger.setGender("Male");
        passenger.setSeatNumber("A1");

        depFlight = new FlightDto();
        depFlight.setId("FL1");
        depFlight.setAvailableSeats(5);
        depFlight.setPrice(100.0);
    }

    @Test
    void testBookTicketSuccess() {
        when(flightClient.getFlight("FL1")).thenReturn(depFlight);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(passengerRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(List.of(passenger)));

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL1",
                null,
                List.of(passenger),
                FLIGHTTYPE.ONE_WAY))
                .expectNextMatches(pnr -> pnr != null && !pnr.isEmpty())
                .verifyComplete();

        verify(flightClient).reserveSeats("FL1", 1);
        verify(ticketRepository).save(any(Ticket.class));
        verify(passengerRepository).saveAll(anyList());
    }

    @Test
    void testBookTicketNotEnoughSeats() {
        depFlight.setAvailableSeats(0);
        when(flightClient.getFlight("FL1")).thenReturn(depFlight);

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL1",
                null,
                List.of(passenger),
                FLIGHTTYPE.ONE_WAY))
                .expectErrorMatches(e -> e instanceof ResponseStatusException &&
                		((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void testGetByPnr() {
        Ticket ticket = new Ticket();
        ticket.setPnr("PNR123");
        when(ticketRepository.findByPnr("PNR123")).thenReturn(Mono.just(ticket));

        StepVerifier.create(bookingService.getByPnr("PNR123"))
                .expectNext(ticket)
                .verifyComplete();
    }

    @Test
    void testHistoryByEmail() {
        Ticket ticket = new Ticket();
        ticket.setPnr("PNR123");
        when(ticketRepository.findByUserEmail("user@example.com")).thenReturn(Flux.just(ticket));

        StepVerifier.create(bookingService.historyByEmail("user@example.com"))
                .expectNext(ticket)
                .verifyComplete();
    }

    @Test
    void testCancelByPnrSuccess() {
        Ticket ticket = new Ticket();
        ticket.setPnr("PNR123");
        ticket.setDepartureFlightId("FL1");
        ticket.setSeatsBooked("A1");
        ticket.setCanceled(false);

        when(ticketRepository.findByPnr("PNR123")).thenReturn(Mono.just(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(Mono.just(ticket));

        StepVerifier.create(bookingService.cancelByPnr("PNR123"))
                .expectNext("Cancelled Successfully")
                .verifyComplete();

        verify(flightClient).releaseSeats("FL1", 1);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void testCancelByPnrAlreadyCancelled() {
        Ticket ticket = new Ticket();
        ticket.setPnr("PNR123");
        ticket.setCanceled(true);

        when(ticketRepository.findByPnr("PNR123")).thenReturn(Mono.just(ticket));

        StepVerifier.create(bookingService.cancelByPnr("PNR123"))
                .expectNext("Ticket already cancelled")
                .verifyComplete();
    }

    @Test
    void testCancelByPnrNotFound() {
        when(ticketRepository.findByPnr("PNR123")).thenReturn(Mono.empty());

        StepVerifier.create(bookingService.cancelByPnr("PNR123"))
                .expectErrorMatches(e -> e instanceof ResponseStatusException &&
                		((ResponseStatusException) e).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }
    
    @Test
    void testBookTicketReturnFlightNotFound() {
        when(flightClient.getFlight("FL1")).thenReturn(depFlight);
        when(flightClient.getFlight("FL2")).thenReturn(null); // return flight missing

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL1",
                "FL2",
                List.of(passenger),
                FLIGHTTYPE.ROUND_TRIP))
                .expectErrorMatches(e -> e instanceof ResponseStatusException &&
                        ((ResponseStatusException) e).getReason().equals("Return flight not found"))
                .verify();
    }

    @Test
    void testBookTicketReturnFlightNotEnoughSeats() {
        when(flightClient.getFlight("FL1")).thenReturn(depFlight);
        FlightDto retFlight = new FlightDto();
        retFlight.setId("FL2");
        retFlight.setAvailableSeats(0); // not enough seats
        retFlight.setPrice(100.0);
        when(flightClient.getFlight("FL2")).thenReturn(retFlight);

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL1",
                "FL2",
                List.of(passenger),
                FLIGHTTYPE.ROUND_TRIP))
                .expectErrorMatches(e -> e instanceof ResponseStatusException &&
                        ((ResponseStatusException) e).getReason().equals("Not enough seats in return flight"))
                .verify();
    }

    @Test
    void testBookTicketDepartureFlightNotFound() {
        // depFlight = null scenario
        when(flightClient.getFlight("FL_UNKNOWN")).thenReturn(null);

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL_UNKNOWN",
                null,
                List.of(passenger),
                FLIGHTTYPE.ONE_WAY))
                .expectErrorMatches(e -> e instanceof ResponseStatusException &&
                        ((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        e.getMessage().contains("Departure flight not found"))
                .verify();
    }

    @Test
    void testBookTicketRoundTripReturnFlightIdNotNull() {
        // depFlight available
        when(flightClient.getFlight("FL1")).thenReturn(depFlight);
        // retFlight is null
        when(flightClient.getFlight("FL2")).thenReturn(null);

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL1",
                "FL2",
                List.of(passenger),
                FLIGHTTYPE.ROUND_TRIP))
                .expectErrorMatches(e -> e instanceof ResponseStatusException &&
                        ((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        e.getMessage().contains("Return flight not found"))
                .verify();
    }

    @Test
    void testCreateTicketAddsReturnFlightPrice() {
        FlightDto retFlight = new FlightDto();
        retFlight.setId("FL2");
        retFlight.setPrice(150.0);
        retFlight.setAvailableSeats(5);

        when(flightClient.getFlight("FL1")).thenReturn(depFlight);
        when(flightClient.getFlight("FL2")).thenReturn(retFlight);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(passengerRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(List.of(passenger)));

        StepVerifier.create(bookingService.bookTicket(
                "user@example.com",
                "FL1",
                "FL2",
                List.of(passenger),
                FLIGHTTYPE.ROUND_TRIP))
                .expectNextMatches(pnr -> {
                    // verify PNR is generated
                    return pnr != null && !pnr.isEmpty();
                })
                .verifyComplete();

        verify(ticketRepository).save(argThat(ticket ->
                ticket.getTotalPrice() == (depFlight.getPrice() + retFlight.getPrice()) * 1 // seatCount=1
        ));
    }
    
    @Test
    void testCancelByPnrCalculatesSeatCount() {
        Ticket ticket = new Ticket();
        ticket.setPnr("PNR123");
        ticket.setDepartureFlightId("FL1");
        ticket.setReturnFlightId("FL2");
        ticket.setSeatsBooked("A1,A2"); // multiple seats
        ticket.setCanceled(false);

        when(ticketRepository.findByPnr("PNR123")).thenReturn(Mono.just(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(Mono.just(ticket));

        StepVerifier.create(bookingService.cancelByPnr("PNR123"))
                .expectNext("Cancelled Successfully")
                .verifyComplete();

        // seatCount = 2
        verify(flightClient).releaseSeats("FL1", 2);
        verify(flightClient).releaseSeats("FL2", 2);
        verify(ticketRepository).save(any(Ticket.class));
    }
    
    @Test
    void testCancelByPnrNoReturnFlight() {
        Ticket ticket = new Ticket();
        ticket.setPnr("PNR123");
        ticket.setDepartureFlightId("FL1");
        ticket.setReturnFlightId(null); // no return flight
        ticket.setSeatsBooked("A1");
        ticket.setCanceled(false);

        when(ticketRepository.findByPnr("PNR123")).thenReturn(Mono.just(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(Mono.just(ticket));

        StepVerifier.create(bookingService.cancelByPnr("PNR123"))
                .expectNext("Cancelled Successfully")
                .verifyComplete();

        verify(flightClient).releaseSeats("FL1", 1);
        verify(flightClient, never()).releaseSeats(eq("FL2"), anyInt());
        verify(ticketRepository).save(any(Ticket.class));
    }



}
