
package ac.ke.dkut.lostfound

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    const val BASE_URL = "http://10.0.2.2:8000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
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
}
