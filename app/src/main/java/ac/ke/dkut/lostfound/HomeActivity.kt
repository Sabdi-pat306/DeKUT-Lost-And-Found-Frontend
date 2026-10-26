package ac.ke.dkut.lostfound

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Require a saved login token.
        if (TokenManager.getToken(this).isNullOrBlank()) {
            openLogin()
            return
        }

        setContentView(R.layout.activity_home)

        findViewById<Button>(R.id.logoutButton)
            .setOnClickListener {
                TokenManager.clearToken(this)
                openLogin()
            }

        findViewById<Button>(R.id.reportLostButton).setOnClickListener {
            startActivity(
                Intent(this, ReportLostActivity::class.java)
            )
        }


        findViewById<Button>(R.id.reportFoundButton).setOnClickListener {
            startActivity(
                Intent(this, ReportFoundActivity::class.java)
            )
        }


        findViewById<Button>(R.id.searchItemsButton).setOnClickListener {
            startActivity(
                Intent(this, SearchItemsActivity::class.java)
            )
        }

        findViewById<Button>(R.id.myClaimsButton)
            .setOnClickListener {
                showComingSoon()
            }
    }

    private fun showComingSoon() {
        Toast.makeText(
            this,
            "This feature will be connected next.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openLogin() {
        startActivity(
            Intent(this, MainActivity::class.java)
        )
        finish()
    }
}
