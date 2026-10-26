
package ac.ke.dkut.lostfound

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CreateItemApi {

    @Multipart
    @POST("api/v1/items/")
    suspend fun createItem(
        @Header("Authorization") authorization: String,
        @Part("report_type") reportType: RequestBody,
        @Part("item_description") description: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part("location_id") locationId: RequestBody,
        @Part("date_lost_found") dateLostFound: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<ItemReport>
}
