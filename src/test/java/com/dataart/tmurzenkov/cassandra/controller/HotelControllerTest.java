package com.dataart.tmurzenkov.cassandra.controller;

import com.dataart.tmurzenkov.cassandra.model.entity.hotel.Hotel;
import com.dataart.tmurzenkov.cassandra.service.impl.ServiceResourceAssembler;
import com.dataart.tmurzenkov.cassandra.service.impl.service.HotelServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.hateoas.Resource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.dataart.tmurzenkov.cassandra.TestUtils.HotelTestUtils.buildHotel;
import static com.dataart.tmurzenkov.cassandra.TestUtils.HotelTestUtils.buildHotels;
import static com.dataart.tmurzenkov.cassandra.TestUtils.HotelTestUtils.buildEmptyHotel;
import static com.dataart.tmurzenkov.cassandra.TestUtils.HttpResponseTest.build;
import static com.dataart.tmurzenkov.cassandra.TestUtils.asJson;
import static com.dataart.tmurzenkov.cassandra.controller.uri.HotelUris.ADD_HOTEL;
import static com.dataart.tmurzenkov.cassandra.controller.uri.HotelUris.HOTELS_IN_THE_CITY;
import static com.dataart.tmurzenkov.cassandra.service.impl.ExceptionInterceptor.Constants.QUERY_EXECUTION_EXCEPTION;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UTs for the {@link HotelController}.
 *
 * @author tmurzenkov
 */
@RunWith(MockitoJUnitRunner.class)
public class HotelControllerTest extends AbstractControllerUnitTest<HotelController> {
    @Mock
    private HotelServiceImpl hotelService;
    @Mock
    private ServiceResourceAssembler<Hotel, Resource<Hotel>> resourceAssembler;
    @InjectMocks
    private HotelController sut;
    private MockMvc mockMvc;

    /**
     * Inits {@link MockMvc}.
     */
    @Before
    public void init() {
        this.mockMvc = this.init(sut);
    }

    @Test
    public void shouldAddNewHotelToTheSystem() throws Exception {
        final UUID hotelId = randomUUID();
        final Hotel hotel = buildHotel(hotelId);
        final Resource<Hotel> hotelResource = new Resource<>(hotel);

        when(hotelService.addHotel(eq(hotel))).thenReturn(hotel);
        when(resourceAssembler.withController(eq(HotelController.class))).thenReturn(resourceAssembler);
        when(resourceAssembler.toResource(eq(hotel))).thenReturn(hotelResource);

        mockMvc
                .perform(post(ADD_HOTEL).content(asJson(hotel)).contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string(asJson(hotelResource)));

        verify(hotelService).addHotel(eq(hotel));
        verify(resourceAssembler).toResource(eq(hotel));
        verifyNoMoreInteractions(hotelService);
    }

    @Test
    public void shouldNotAddHotelWithNullId() throws Exception {
        final Hotel hotel = buildEmptyHotel();
        final RuntimeException exception = new IllegalArgumentException("Cannot add the hotel with empty id. ");

        when(hotelService.addHotel(eq(hotel))).thenThrow(exception);
        mockMvc
                .perform(post(ADD_HOTEL).content(asJson(hotel)).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(asJson(build(exception, QUERY_EXECUTION_EXCEPTION, BAD_REQUEST).getBody())));
    }

    @Test
    public void shouldNotAddHotelWithEmptyName() throws Exception {
        final Hotel hotel = buildHotel(UUID.randomUUID());
        hotel.setName("");
        final RuntimeException exception = new IllegalArgumentException("Cannot add the hotel with empty name. ");

        when(hotelService.addHotel(eq(hotel))).thenThrow(exception);
        mockMvc
                .perform(post(ADD_HOTEL).content(asJson(hotel)).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(asJson(build(exception, QUERY_EXECUTION_EXCEPTION, BAD_REQUEST).getBody())));
    }

    @Test
    public void shouldNotAddHotelWithEmptyPhone() throws Exception {
        final Hotel hotel = buildHotel(UUID.randomUUID());
        hotel.setPhone("");
        final RuntimeException exception = new IllegalArgumentException("Cannot add the hotel with empty phone field. ");

        when(hotelService.addHotel(eq(hotel))).thenThrow(exception);
        mockMvc
                .perform(post(ADD_HOTEL).content(asJson(hotel)).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(asJson(build(exception, QUERY_EXECUTION_EXCEPTION, BAD_REQUEST).getBody())));
    }

    @Test
    public void shouldNotAddHotelWithEmptyAddress() throws Exception {
        final Hotel hotel = buildHotel(UUID.randomUUID());
        hotel.setAddress(null);
        final RuntimeException exception = new IllegalArgumentException("Cannot add the hotel with empty address info. ");

        when(hotelService.addHotel(eq(hotel))).thenThrow(exception);
        mockMvc
                .perform(post(ADD_HOTEL).content(asJson(hotel)).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(asJson(build(exception, QUERY_EXECUTION_EXCEPTION, BAD_REQUEST).getBody())));
    }


    @Test
    public void shouldFindAllHotelsForTheCityName() throws Exception {
        final String city = "London";
        final List<Hotel> hotels = buildHotels();
        final List<Resource<Hotel>> hotelResources = hotels.stream().map(Resource::new).collect(toList());

        when(hotelService.findAllHotelsInTheCity(eq(city))).thenReturn(hotels);
        when(resourceAssembler.toResource(eq(hotels))).thenReturn(hotelResources);
        mockMvc
                .perform(get(HOTELS_IN_THE_CITY, city).contentType(APPLICATION_JSON))
                .andExpect(status().isFound())
                .andExpect(content().string(asJson(hotelResources)));
    }

    @Test
    public void shouldNotFindAllHotelsForTheEmptyCityName() throws Exception {
        final String city = "";

        mockMvc
                .perform(get(HOTELS_IN_THE_CITY, city).contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}
