package com.dataart.tmurzenkov.cassandra.controller;

import com.dataart.tmurzenkov.cassandra.TestUtils;
import com.dataart.tmurzenkov.cassandra.model.dto.SearchRequest;
import com.dataart.tmurzenkov.cassandra.model.entity.room.Room;
import com.dataart.tmurzenkov.cassandra.model.exception.RecordExistsException;
import com.dataart.tmurzenkov.cassandra.service.impl.ExceptionInterceptor;
import com.dataart.tmurzenkov.cassandra.service.impl.RoomServiceImpl;
import com.dataart.tmurzenkov.cassandra.service.impl.ServiceResourceAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dataart.tmurzenkov.cassandra.TestUtils.RoomTestUtils.buildRoom;
import static com.dataart.tmurzenkov.cassandra.TestUtils.RoomTestUtils.buildRooms;
import static com.dataart.tmurzenkov.cassandra.TestUtils.asJson;
import static com.dataart.tmurzenkov.cassandra.controller.uri.RoomUris.ADD_ROOM;
import static com.dataart.tmurzenkov.cassandra.controller.uri.RoomUris.GET_FREE_ROOMS;
import static com.dataart.tmurzenkov.cassandra.service.impl.ExceptionInterceptor.Constants.RECORD_ALREADY_EXISTS;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * {@link RoomController} UTs.
 *
 * @author tmurzenkov
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomControllerTest {
    @Mock
    private RoomServiceImpl roomService;
    @Mock
    private ServiceResourceAssembler<Room, Resource<Room>> serviceResourceAssembler;
    @InjectMocks
    private RoomController sut;
    private MockMvc mockMvc;

    /**
     * Inits mock mvc.
     */
    @Before
    public void init() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
        mockMvc = standaloneSetup(sut)
                .setControllerAdvice(new ExceptionInterceptor())
                .setMessageConverters(mappingJackson2HttpMessageConverter)
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    public void shouldAddRoom() throws Exception {
        final Room room = buildRoom();
        final Resource<Room> roomResource = new Resource<>(room);

        when(roomService.addRoomToHotel(eq(room))).thenReturn(room);
        when(serviceResourceAssembler.withController(eq(sut.getClass()))).thenReturn(serviceResourceAssembler);
        when(serviceResourceAssembler.toResource(eq(room))).thenReturn(roomResource);

        mockMvc
                .perform(post(ADD_ROOM).content(asJson(room)).contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string(asJson(roomResource)));
    }

    @Test
    public void shouldNotAddTheSameRoomTwice() throws Exception {
        final Room room = buildRoom();
        final String exceptionMessage = format("The room is already inserted in DB. Room info '%s'", room);
        final RuntimeException alreadyAddedRoom = new RecordExistsException(exceptionMessage);
        final ResponseEntity response = TestUtils.HttpResponseTest.build(alreadyAddedRoom, RECORD_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);

        when(roomService.addRoomToHotel(eq(room))).thenThrow(alreadyAddedRoom);

        mockMvc
                .perform(post(ADD_ROOM).content(asJson(room)).contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().string(asJson(response.getBody())));
    }

    @Test
    public void shouldFindAllBookedRooms() throws Exception {
        final LocalDate start = LocalDate.now();
        final LocalDate end = start.plusDays(3);
        final UUID hotelId = UUID.randomUUID();
        final SearchRequest searchRequest = new SearchRequest(start, end, hotelId);
        final List<Room> rooms = buildRooms(3);
        List<Resource<Room>> roomAsResources = rooms.stream().map(room -> new Resource<>(room)).collect(Collectors.toList());

        when(roomService.findFreeRoomsInTheHotel(eq(searchRequest))).thenReturn(rooms);
        when(serviceResourceAssembler.toResource(eq(rooms))).thenReturn(roomAsResources);

        mockMvc
                .perform(get(GET_FREE_ROOMS).content(asJson(searchRequest)).contentType(APPLICATION_JSON))
                .andExpect(status().isFound())
                .andExpect(content().string(asJson(roomAsResources)));
    }

    @Test
    public void shouldNotFindAllBookedRoomsForReverseStartEnd() throws Exception {
        final SearchRequest searchRequest = new SearchRequest();
        mockMvc
                .perform(get(GET_FREE_ROOMS).content(asJson(searchRequest)).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}