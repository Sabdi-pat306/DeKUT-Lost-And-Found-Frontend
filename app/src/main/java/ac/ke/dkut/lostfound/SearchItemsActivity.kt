
package ac.ke.dkut.lostfound

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.IOException

class SearchItemsActivity : AppCompatActivity() {

    private lateinit var reportTypeSpinner: Spinner
    private lateinit var searchButton: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemReportAdapter
    private var categoryNames: Map<Int, String> = emptyMap()
    private var locationNames: Map<Int, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = TokenManager.getToken(this)

        if (token.isNullOrBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_search_items)

        reportTypeSpinner = findViewById(R.id.reportTypeSpinner)
        searchButton = findViewById(R.id.searchButton)
        loadingProgress = findViewById(R.id.loadingProgress)
        emptyMessage = findViewById(R.id.emptyMessage)
        itemsRecyclerView = findViewById(R.id.itemsRecyclerView)

        val options = listOf("Lost Items", "Found Items")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        reportTypeSpinner.adapter = spinnerAdapter

        itemAdapter = ItemReportAdapter(
            emptyList(),
            categoryNames,
            locationNames
        )
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = itemAdapter

        searchButton.setOnClickListener {
            val reportType =
                if (reportTypeSpinner.selectedItemPosition == 0) {
                    "lost"
                } else {
                    "found"
                }

            loadItems(reportType)
        }

        // Load lost items when the screen first opens.
        lifecycleScope.launch {
            val token = TokenManager.getToken(this@SearchItemsActivity)

            if (!token.isNullOrBlank()) {
                try {
                    loadMetadata()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SearchItemsActivity,
                        "Could not load categories and locations",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            loadItems("lost")
        }
    }


    private suspend fun loadMetadata() {
        try {
            val categoriesResponse = RetrofitClient.itemMetadataApi.getCategories()
            val locationsResponse = RetrofitClient.itemMetadataApi.getLocations()

            if (categoriesResponse.isSuccessful &&
                locationsResponse.isSuccessful
            ) {
                categoryNames = (categoriesResponse.body() ?: emptyList())
                    .associate { it.category_id to it.category_name }

                locationNames = (locationsResponse.body() ?: emptyList())
                    .associate { it.location_id to it.location_name }

                itemAdapter.updateMetadata(categoryNames, locationNames)
            }
        } catch (e: Exception) {
            // Keep searching even if metadata is temporarily unavailable.
        }
    }


    private fun loadItems(reportType: String) {
        val token = TokenManager.getToken(this)

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        searchButton.isEnabled = false
        loadingProgress.visibility = View.VISIBLE
        emptyMessage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.itemsApi.getItems(
                    authorization = "Bearer $token",
                    reportType = reportType
                )

                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    itemAdapter.updateItems(items)

                    emptyMessage.visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE

                    emptyMessage.text = "No $reportType items found."
                } else {
                    Toast.makeText(
                        this@SearchItemsActivity,
                        "Could not load items. HTTP ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()

                    if (response.code() == 401 || response.code() == 403) {
                        TokenManager.clearToken(this@SearchItemsActivity)
                        startActivity(
                            Intent(
                                this@SearchItemsActivity,
                                MainActivity::class.java
                            )
                        )
                        finish()
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(
                    this@SearchItemsActivity,
                    "Cannot connect to the server. Check that your backend is running.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SearchItemsActivity,
                    "An unexpected error occurred while loading items.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingProgress.visibility = View.GONE
                searchButton.isEnabled = true
            }
        }
    }


    private fun testCategoryLoading() {
        val token = TokenManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.itemMetadataApi.getCategories(

                )

                if (response.isSuccessful) {
                    val result = response.body().orEmpty()

                    categoryNames = result.associate {
                        it.category_id to it.category_name
                    }

                    itemAdapter.updateMetadata(categoryNames, locationNames)

                    Toast.makeText(
                        this@SearchItemsActivity,
                        "Loaded ${result.size} categories",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@SearchItemsActivity,
                        "Category request failed: HTTP ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SearchItemsActivity,
                    "Category error: ${e.javaClass.simpleName}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
