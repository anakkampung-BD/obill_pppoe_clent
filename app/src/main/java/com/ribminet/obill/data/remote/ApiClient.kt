package com.ribminet.obill.data.remote

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun create(tokenStore: TokenStore): CustomerApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")
                // Jangan timpa Content-Type multipart (upload foto pengaduan, dll.)
                val body = original.body
                if (body == null || body !is MultipartBody) {
                    builder.header("Content-Type", "application/json")
                }
                if (ApiConfig.APP_KEY.isNotBlank()) {
                    builder.header("X-Customer-App-Key", ApiConfig.APP_KEY)
                }
                tokenStore.token?.let { token ->
                    if (token.isNotBlank()) builder.header("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CustomerApi::class.java)
    }
}
