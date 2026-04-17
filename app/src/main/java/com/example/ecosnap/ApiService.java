package com.example.ecosnap;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("user?select=*")
    Call<List<User>> getUserByFirebaseUid(
            @Query("firebase_uid") String uid
    );

    @POST("user")
    Call<Void> insertUser(@Body Map<String, String> data);

    @GET("scan_history?select=*&order=created_at.desc&limit=1")
    Call<List<ScanHistory>> getScanTerakhir(
            @Query("user_id") String userId
    );

    @GET("scan_history?select=*")
    Call<List<ScanHistory>> getScanByUser(
            @Query("user_id") String userId
    );

    @GET("scan_history?select=*")
    Call<List<ScanHistory>> getScanByRw(
            @Query("rw_id") String rwId
    );

    @GET("user?select=*")
    Call<List<User>> getUserByRwId(
            @Query("rw_id") String rwId,
            @Query("role") String role
    );
}