package com.klimaatmobiel.data.network

import com.klimaatmobiel.domain.Group
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// launch backend as http -- line 39 properties "applicationUrl": "http://localhost:5000"
private const val BASE_URL = "http://10.0.2.2:5000/api/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .baseUrl(BASE_URL)
    .build()


interface KlimaatmobielApiService {
    // API CALLS HERE


    @GET("group/project/{groupCode}")
    fun getFullGroup(@Path("groupCode") groupCode: String):
            Deferred<Group>


}

object KlimaatmobielApi {
    val retrofitService: KlimaatmobielApiService by lazy {
        retrofit.create(KlimaatmobielApiService::class.java)
    }
}
