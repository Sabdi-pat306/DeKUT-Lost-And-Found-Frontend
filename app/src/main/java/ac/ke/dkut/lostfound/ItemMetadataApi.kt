
package ac.ke.dkut.lostfound

import retrofit2.Response
import retrofit2.http.GET

interface ItemMetadataApi {

    @GET("api/v1/item-metadata/categories")
    suspend fun getCategories(): Response<List<ItemCategory>>

    @GET("api/v1/item-metadata/locations")
    suspend fun getLocations(): Response<List<CampusLocation>>
}
