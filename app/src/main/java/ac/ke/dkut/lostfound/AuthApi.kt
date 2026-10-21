
package ac.ke.dkut.lostfound

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/users/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}
