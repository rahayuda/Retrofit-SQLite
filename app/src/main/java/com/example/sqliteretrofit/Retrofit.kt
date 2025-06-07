package com.example.sqliteretrofit

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class User(val id: String?, val name: String, val email: String)

interface ApiService {
    @GET("users")
    fun getUsers(): Call<List<User>>

    @POST("users")
    fun insertUser(@Body user: User): Call<User>

    @PUT("users/{id}")
    fun updateUser(@Path("id") id: String, @Body user: User): Call<Void>

    @DELETE("users/{id}")
    fun deleteUser(@Path("id") id: String): Call<Void>
}

object RetrofitClient {
    private const val BASE_URL = "https://cougar-accurate-mentally.ngrok-free.app/api/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
