package ac.ke.dkut.lostfound

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ItemsApi {

    @GET("api/v1/items/")
    suspend fun getItems(
        @Header("Authorization") authorization: String,
        @Query("report_type") reportType: String
    ): Response<List<ItemReport>>
}