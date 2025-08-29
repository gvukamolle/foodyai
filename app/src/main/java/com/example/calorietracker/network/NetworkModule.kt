package com.example.calorietracker.network

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Protocol
import okhttp3.EventListener
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Handshake
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }

    @Provides
    @Singleton
    fun provideOkHttpClient(logger: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .addNetworkInterceptor { chain ->
                val original = chain.request()
                val url = original.url
                val builder = original.newBuilder()
                if (url.host == "hook.us2.make.com") {
                    builder.header("Connection", "close")
                    builder.header("Accept-Encoding", "identity")
                    builder.header("User-Agent", "calorietracker/1.0 (OkHttp)")
                }
                chain.proceed(builder.build())
            }
            .eventListenerFactory {
                object : EventListener() {
                    override fun dnsStart(call: Call, domainName: String) {
                        Log.d("NetworkEL", "dnsStart: $domainName")
                    }
                    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
                        Log.d("NetworkEL", "dnsEnd: $domainName -> ${inetAddressList.joinToString { it.hostAddress }}")
                    }
                    override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
                        Log.d("NetworkEL", "connectStart: ${inetSocketAddress.address.hostAddress}:${inetSocketAddress.port}")
                    }
                    override fun secureConnectStart(call: Call) {
                        Log.d("NetworkEL", "secureConnectStart")
                    }
                    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
                        Log.d("NetworkEL", "secureConnectEnd: tls=${handshake?.tlsVersion} cipher=${handshake?.cipherSuite}")
                    }
                    override fun connectEnd(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: Protocol?) {
                        Log.d("NetworkEL", "connectEnd: protocol=$protocol")
                    }
                    override fun requestHeadersStart(call: Call) {
                        Log.d("NetworkEL", "requestHeadersStart")
                    }
                    override fun requestHeadersEnd(call: Call, request: Request) {
                        Log.d("NetworkEL", "requestHeadersEnd: ${request.method} ${request.url}")
                    }
                    override fun requestBodyStart(call: Call) {
                        Log.d("NetworkEL", "requestBodyStart")
                    }
                    override fun requestBodyEnd(call: Call, byteCount: Long) {
                        Log.d("NetworkEL", "requestBodyEnd: bytes=$byteCount")
                    }
                    override fun responseHeadersStart(call: Call) {
                        Log.d("NetworkEL", "responseHeadersStart")
                    }
                    override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
                        Log.d("NetworkEL", "responseHeadersEnd: code=${response.code}")
                    }
                    override fun callFailed(call: Call, ioe: java.io.IOException) {
                        Log.e("NetworkEL", "callFailed: ${ioe.message}", ioe)
                    }
                }
            }
            .callTimeout(70, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d("Network", "Отправка запроса: ${request.url}")
                try {
                    val response = chain.proceed(request)
                    Log.d("Network", "Ответ: ${response.code}")
                    response
                } catch (e: Exception) {
                    Log.e("Network", "Ошибка запроса: ${e.message}", e)
                    throw e
                }
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(MakeService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideMakeService(retrofit: Retrofit): MakeService =
        retrofit.create(MakeService::class.java)
    }