package com.dataart.tmurzenkov.cassandra.service;

import com.dataart.tmurzenkov.cassandra.dao.RoomByGuestAndDateDao;
import com.dataart.tmurzenkov.cassandra.dao.GuestDao;
import com.dataart.tmurzenkov.cassandra.dao.RoomByHotelAndDateDao;
import com.dataart.tmurzenkov.cassandra.dao.RoomDao;
import com.dataart.tmurzenkov.cassandra.model.dto.BookingRequest;
import com.dataart.tmurzenkov.cassandra.model.entity.Guest;
import com.dataart.tmurzenkov.cassandra.model.entity.room.Room;
import com.dataart.tmurzenkov.cassandra.model.entity.room.RoomByHotelAndDate;
import com.dataart.tmurzenkov.cassandra.model.entity.room.RoomByGuestAndDate;
import com.dataart.tmurzenkov.cassandra.model.exception.RecordExistsException;
import com.dataart.tmurzenkov.cassandra.model.exception.RecordNotFoundException;
import com.dataart.tmurzenkov.cassandra.service.impl.service.GuestServiceImpl;
import com.dataart.tmurzenkov.cassandra.service.impl.validation.GuestValidatorServiceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.dataart.tmurzenkov.cassandra.service.util.DateUtils.format;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

/**
 * UTs for the {@link GuestServiceImpl}.
 *
 * @author tmurzenkov
 */
@RunWith(MockitoJUnitRunner.class)
public class GuestServiceTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private GuestValidatorServiceImpl validatorService;
    @Mock
    private GuestDao guestDao;
    @Mock
    private RoomDao roomDao;
    @Mock
    private RoomByHotelAndDateDao roomByHotelAndDateDao;
    @Mock
    private RoomByGuestAndDateDao byGuestAndDateDao;
    @InjectMocks
    private GuestServiceImpl sut;

    @Test
    public void shouldBookRoom() {
        final Integer roomNumber = 1;
        final BookingRequest bookingRequest = getBookingRequest(roomNumber);
        final RoomByHotelAndDate roomByHotelAndDate = new RoomByHotelAndDate(bookingRequest);
        final RoomByGuestAndDate expectedRoomByGuestAndDate = new RoomByGuestAndDate(bookingRequest);
        final Room room = new Room(roomByHotelAndDate);
        expectedRoomByGuestAndDate.setConfirmationNumber(valueOf(bookingRequest.hashCode()));

        when(byGuestAndDateDao.insert(any(RoomByGuestAndDate.class))).thenReturn(expectedRoomByGuestAndDate);
        when(roomDao.findOne(eq(room.getCompositeId()))).thenReturn(room);

        sut.performBooking(bookingRequest);

        verify(roomDao).findOne(eq(room.getCompositeId()));
        verify(roomByHotelAndDateDao).insert(eq(roomByHotelAndDate));
        verify(byGuestAndDateDao).insert(eq(expectedRoomByGuestAndDate));
        verify(byGuestAndDateDao, never()).save(any(RoomByGuestAndDate.class));
    }

    @Test
    public void shouldNotBookRoomForNullBookingRequest() {
        final String exceptionMessage = "Cannot perform reservation for empty reservation request. ";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(exceptionMessage);

        sut.performBooking(null);

        verify(byGuestAndDateDao, never()).save(any(RoomByGuestAndDate.class));
    }


    @Test
    public void shouldNotPerformBookingRequestIfSuchRoomDoesNotExists(){
        final Integer roomNumber = 1;
        final BookingRequest bookingRequest = getBookingRequest(roomNumber);
        final String exceptionMessage = format("The following room does not exists. Room number: '%d', hotel id: '%s'",
                bookingRequest.getRoomNumber(), bookingRequest.getHotelId());

        thrown.expect(RecordNotFoundException.class);
        thrown.expectMessage(exceptionMessage);

        sut.performBooking(bookingRequest);
    }

    @Test
    public void shouldThrowAlreadyBookedException() {
        final Integer roomNumber = 1;
        final BookingRequest bookingRequest = getBookingRequest(roomNumber);
        final RoomByGuestAndDate expectedRoomByGuestAndDate = new RoomByGuestAndDate(bookingRequest);
        final String exceptionMessage = format("The following room is already booked. Room number: '%d', hotel id: '%s'",
                bookingRequest.getRoomNumber(), bookingRequest.getHotelId());

        thrown.expect(RecordExistsException.class);
        thrown.expectMessage(exceptionMessage);

        when(byGuestAndDateDao.exists(eq(expectedRoomByGuestAndDate.getCompositeId()))).thenReturn(true);

        sut.performBooking(bookingRequest);

        verify(byGuestAndDateDao, never()).save(any(RoomByGuestAndDate.class));
    }

    @Test
    public void shouldFindBookedRooms() {
        final Integer roomsFound = 3;
        final UUID guestId = randomUUID();
        final LocalDate bookingDate = now();
        final List<RoomByGuestAndDate> roomByGuestAndDates = generateRooms(guestId, bookingDate, roomsFound);
        final List<RoomByHotelAndDate> expectedBookedRoomByHotelAndDates = roomByGuestAndDates.stream().map(RoomByHotelAndDate::new).collect(toList());

        when(byGuestAndDateDao.getAllBookedRooms(eq(guestId), eq(bookingDate))).thenReturn(roomByGuestAndDates);

        List<RoomByHotelAndDate> actualBookedRoomByHotelAndDates = sut.findBookedRoomsForTheGuestIdAndDate(guestId, bookingDate);

        verify(byGuestAndDateDao).getAllBookedRooms(eq(guestId), eq(bookingDate));
        assertFalse(actualBookedRoomByHotelAndDates.isEmpty());
        assertEquals(actualBookedRoomByHotelAndDates.size(), expectedBookedRoomByHotelAndDates.size());
        assertTrue(expectedBookedRoomByHotelAndDates.containsAll(actualBookedRoomByHotelAndDates));
        assertTrue(actualBookedRoomByHotelAndDates.containsAll(expectedBookedRoomByHotelAndDates));
    }

    @Test
    public void shouldThrowExceptionIfNoBookedRoomsWereFound() {
        final UUID guestId = randomUUID();
        final LocalDate bookingDate = now();
        final String exceptionMessage = format("Cannot find the booked rooms for the customer id '%s' and given date '%s'",
                guestId, format(bookingDate));
        thrown.expect(RecordNotFoundException.class);
        thrown.expectMessage(exceptionMessage);
        sut.findBookedRoomsForTheGuestIdAndDate(guestId, bookingDate);
        when(byGuestAndDateDao.getAllBookedRooms(eq(guestId), eq(bookingDate))).thenReturn(emptyList());
        verify(byGuestAndDateDao).getAllBookedRooms(eq(guestId), eq(bookingDate));
    }

    @Test
    public void shouldThrowExceptionsForNullGuestIdWhenLookingForBookedRooms() {
        final String exceptionMessage = "Cannot perform search of the booked room for the null guest id ";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(exceptionMessage);
        sut.findBookedRoomsForTheGuestIdAndDate(null, now());
        verify(byGuestAndDateDao, never()).getAllBookedRooms(any(), any());
    }

    @Test
    public void shouldThrowExceptionsForNullBookingDateWhenLookingForBookedRooms() {
        final String exceptionMessage = "Cannot perform search of the booked room for the null reservation date ";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(exceptionMessage);
        sut.findBookedRoomsForTheGuestIdAndDate(randomUUID(), null);
        verify(byGuestAndDateDao, never()).getAllBookedRooms(any(), any());
    }

    @Test
    public void shouldThrowExceptionsForNullParametersDateWhenLookingForBookedRooms() {
        final String exceptionMessage = "Cannot perform search of the booked room for the null guest id ";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(exceptionMessage);
        sut.findBookedRoomsForTheGuestIdAndDate(null, null);
        verify(byGuestAndDateDao, never()).getAllBookedRooms(any(), any());
    }

    @Test
    public void shouldRegistersTheNewGuest() {
        final UUID guestId = UUID.randomUUID();
        final Guest expectedGuest = new Guest();
        expectedGuest.setId(guestId);
        expectedGuest.setFirstName("Test name");
        expectedGuest.setLastName("Test last name");

        when(guestDao.insert(eq(expectedGuest))).thenReturn(expectedGuest);
        doNothing().when(validatorService).checkIfExists(eq(expectedGuest));
        doCallRealMethod().when(validatorService).validateInfo(eq(expectedGuest));

        Guest actualGuest = sut.registerNewGuest(expectedGuest);

        verify(guestDao).insert(eq(expectedGuest));
        verify(validatorService).validateInfo(eq(expectedGuest));
        verify(validatorService).checkIfExists(eq(expectedGuest));
        assertEquals(actualGuest, expectedGuest);
    }

    @Test
    public void shouldNotRegisterTheNewGuestIfSuchInformationExists() {
        final UUID guestId = UUID.randomUUID();
        final Guest expectedGuest = new Guest();
        expectedGuest.setId(guestId);
        expectedGuest.setFirstName("Test name");
        expectedGuest.setLastName("Test last name");
        final String exceptionMessage = format("The guest information is already stored in DB. "
                + "Guest id: '%s', name: '%s', surname: '%s'", guestId, expectedGuest.getFirstName(), expectedGuest.getLastName());
        thrown.expect(RecordExistsException.class);
        thrown.expectMessage(exceptionMessage);
        doThrow(new RecordExistsException(exceptionMessage)).when(validatorService).checkIfExists(eq(expectedGuest));

        sut.registerNewGuest(expectedGuest);

        verify(validatorService).checkIfExists(eq(expectedGuest));
        verify(guestDao).exists(eq(expectedGuest.getCompositeId()));
        verify(guestDao, never()).insert(any());
    }

    private List<RoomByGuestAndDate> generateRooms(UUID guestId, LocalDate bookingDate, Integer roomsFound) {
        return IntStream.of(0, roomsFound + 1)
                .mapToObj(roomNumber -> new RoomByGuestAndDate(guestId, bookingDate, roomNumber))
                .collect(toList());
    }

    private BookingRequest getBookingRequest(Integer roomNumber) {
        final UUID hotelId = randomUUID();
        final UUID guestId = randomUUID();
        final LocalDate bookingDate = now();

        final BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setBookingDate(bookingDate);
        bookingRequest.setGuestId(guestId);
        bookingRequest.setHotelId(hotelId);
        bookingRequest.setRoomNumber(roomNumber);
        return bookingRequest;
    }
}
