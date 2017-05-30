package com.dataart.tmurzenkov.cassandra.service;

import com.dataart.tmurzenkov.cassandra.model.dto.BookingRequest;
import com.dataart.tmurzenkov.cassandra.model.entity.Guest;
import com.dataart.tmurzenkov.cassandra.model.entity.room.Room;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Guest service.
 *
 * @author tmurzenkov
 */
public interface GuestService {

    /**
     * Registers the new {@link Guest} in the system.
     *
     * @param guest {@link Guest}
     * @return registered {@link Guest}
     */
    Guest registerNewGuest(Guest guest);

    /**
     * Finds all booked {@link Room}s in by the provided {@link UUID} guest id.
     *
     * @param guestId     {@link UUID}
     * @param bookingDate {@link Date}
     * @return {@link List} of {@link Room}
     */
    List<Room> findBookedRoomsForTheGuestIdAndDate(UUID guestId, LocalDate bookingDate);

    /**
     * Performs the actual booking.
     *
     * @param bookingRequest {@link BookingRequest}
     * @return {@link BookingRequest}
     */
    BookingRequest performBooking(BookingRequest bookingRequest);
}
