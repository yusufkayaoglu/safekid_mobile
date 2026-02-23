package com.safekid.mobile.network;

import com.safekid.mobile.network.dto.LocationDto;
import com.safekid.mobile.network.dto.SendLocationRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChildApi {

    @POST("child/location")
    Call<LocationDto> sendLocation(@Body SendLocationRequest request);
}
