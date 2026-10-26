
package ac.ke.dkut.lostfound

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val BASE_URL = "http://10.0.2.2:8000/"


    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val itemsApi: ItemsApi by lazy {
        retrofit.create(ItemsApi::class.java)
    }

    val itemMetadataApi: ItemMetadataApi by lazy {
        retrofit.create(ItemMetadataApi::class.java)
    }

    val createItemApi: CreateItemApi by lazy {
        retrofit.create(CreateItemApi::class.java)
    }
}
