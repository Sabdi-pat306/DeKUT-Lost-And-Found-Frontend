package ac.ke.dkut.lostfound

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            when {
                email.isEmpty() -> {
                    emailInput.error = "Please enter your university email"
                    emailInput.requestFocus()
                }

                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email).matches() -> {
                    emailInput.error = "Enter a valid email address"
                    emailInput.requestFocus()
                }

                password.isEmpty() -> {
                    passwordInput.error = "Please enter your password"
                    passwordInput.requestFocus()
                }

                else -> {
                    performLogin(email, password)
                }
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.isEnabled = false
        loginButton.text = "Signing in..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.login(
                    LoginRequest(
                        university_email = email,
                        password = password
                    )
                )

                if (response.isSuccessful) {
                    val result = response.body()


                    if (result != null && result.access_token.isNotBlank()) {
                        TokenManager.saveToken(
                            this@MainActivity,
                            result.access_token
                        )

                        Toast.makeText(
                            this@MainActivity,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = android.content.Intent(
                            this@MainActivity,
                            HomeActivity::class.java
                        )
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "The server returned an invalid response.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this@MainActivity,
                        if (response.code() == 401) {
                            "Invalid credentials or inactive account."
                        } else {
                            "Login failed. HTTP ${response.code()}."
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: IOException) {
                Toast.makeText(
                    this@MainActivity,
                    "Cannot connect to the server. Check that the backend is running.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: HttpException) {
                Toast.makeText(
                    this@MainActivity,
                    "Server error. Please try again.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "An unexpected error occurred.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                loginButton.isEnabled = true
                loginButton.text = "Sign In"
            }
        }
    }
}
