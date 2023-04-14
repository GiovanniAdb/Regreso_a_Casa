package com.example.regresoacasa

import android.accessibilityservice.GestureDescription
import android.app.DownloadManager
import android.location.Location
import android.telecom.Call
import jdk.nashorn.internal.runtime.PropertyDescriptor.GET
import org.chromium.base.Callback


interface RouteService {
    @GET("/v2/directions/{profile}")
    fun getRoute(
        @Path("profile") profile: String?,
        @Query("api_key") apiKey: String?,
        @Query("start") start: String?,
        @Query("end") end: String?
    ): Call<RouteResponse?>?
}

class RouteManager {
    private val routeService: RouteService

    init {
        val httpClient: OkHttpClient.Builder = GestureDescription.Builder()
        httpClient.addInterceptor { chain ->
            val original: DownloadManager.Request = chain.request()
            val requestBuilder: DownloadManager.Request.Builder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method(original.method(), original.body())
            val request: DownloadManager.Request = requestBuilder.build()
            chain.proceed(request)
        }
        val client: OkHttpClient = httpClient.build()
        val retrofit: Retrofit = Builder()
            .baseUrl(ENDPOINT)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        routeService = retrofit.create(RouteService::class.java)
    }

    fun getRoute(start: Location, end: String?, callback: Callback<RouteResponse?>?) {
        val startString =
            java.lang.String.format("%s,%s", start.getLatitude(), start.getLongitude())
        val call: Call<RouteResponse> =
            routeService.getRoute("driving-car", API_KEY, startString, end)
        call.enqueue(callback)
    }

    companion object {
        private const val API_KEY = "YOUR_API_KEY"
        private const val ENDPOINT = "https://api.openrouteservice.org"
    }
}
