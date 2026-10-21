package ac.ke.dkut.lostfound

data class LoginRequest(
    val university_email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val access_token: String,
    val token_type: String
)