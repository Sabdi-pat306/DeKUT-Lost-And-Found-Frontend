
package ac.ke.dkut.lostfound

import android.content.Context

object TokenManager {

    private const val PREFS_NAME = "dekut_auth"
    private const val TOKEN_KEY = "access_token"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(TOKEN_KEY)
            .apply()
    }
}
