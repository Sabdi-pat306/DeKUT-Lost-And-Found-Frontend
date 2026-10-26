
package ac.ke.dkut.lostfound

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Locale

import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.launch




class ReportLostActivity : AppCompatActivity() {

    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var locationSpinner: Spinner
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private var selectedDateLost: String = ""

    private var categories: List<ItemCategory> = emptyList()
    private var locations: List<CampusLocation> = emptyList()


    private var selectedImageUri: Uri? = null

    private lateinit var selectedImageText: TextView
    private lateinit var itemImagePreview: ImageView

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                itemImagePreview.setImageURI(uri)
                itemImagePreview.visibility = View.VISIBLE
                selectedImageText.text = "Photo selected"
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = TokenManager.getToken(this)
        if (token.isNullOrBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_report_lost)
        selectedImageText = findViewById(R.id.selectedImageText)
        itemImagePreview = findViewById(R.id.itemImagePreview)

        descriptionInput = findViewById(R.id.itemDescriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        locationSpinner = findViewById(R.id.locationSpinner)
        submitButton = findViewById(R.id.submitLostItemButton)
        progressBar = findViewById(R.id.submitProgress)

        submitButton.setOnClickListener {
            submitLostItem()
        }




        findViewById<Button>(R.id.dateButton).setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDateLost = String.format(
                        Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        dayOfMonth
                    )

                    findViewById<Button>(R.id.dateButton).text =
                        selectedDateLost
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        findViewById<Button>(R.id.chooseImageButton).setOnClickListener {
            imagePicker.launch("image/*")
        }

        submitButton.isEnabled = false

        lifecycleScope.launch {
            loadDropdownData(token)
        }

    }

    private suspend fun loadDropdownData(token: String) {
        try {
            val authorization = "Bearer $token"

            val categoryResponse =
                RetrofitClient.itemMetadataApi.getCategories()

            val locationResponse =
                RetrofitClient.itemMetadataApi.getLocations()

            if (!categoryResponse.isSuccessful ||
                !locationResponse.isSuccessful
            ) {
                Toast.makeText(
                    this,
                    "Could not load categories or locations.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            categories = categoryResponse.body().orEmpty()
            locations = locationResponse.body().orEmpty()

            if (categories.isEmpty() || locations.isEmpty()) {
                Toast.makeText(
                    this,
                    "No categories or locations were returned.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            categorySpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                categories.map { it.category_name }
            ).apply {
                setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            locationSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                locations.map { it.location_name }
            ).apply {
                setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            submitButton.isEnabled = true

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to load form data: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun submitLostItem() {
        val description = descriptionInput.text.toString().trim()
        val imageUri = selectedImageUri

        if (description.isBlank()) {
            descriptionInput.error = "Please describe the lost item"
            descriptionInput.requestFocus()
            return
        }

        if (selectedDateLost.isBlank()) {
            Toast.makeText(this, "Please select the date lost", Toast.LENGTH_SHORT).show()
            return
        }

        if (categories.isEmpty() || locations.isEmpty()) {
            Toast.makeText(this, "Categories and locations are not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val token = TokenManager.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val category = categories[categorySpinner.selectedItemPosition]
        val location = locations[locationSpinner.selectedItemPosition]

        submitButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            var temporaryImage: File? = null

            try {
                var imagePart: MultipartBody.Part? = null

                if (imageUri != null) {
                    val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"

                    val extension = when (mimeType) {
                        "image/png" -> ".png"
                        "image/webp" -> ".webp"
                        else -> ".jpg"
                    }

                    temporaryImage = File.createTempFile(
                        "lost_item_",
                        extension,
                        cacheDir
                    )

                    contentResolver.openInputStream(imageUri).use { input ->
                        requireNotNull(input) { "Could not open the selected image" }

                        FileOutputStream(temporaryImage).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (temporaryImage.length() > 5 * 1024 * 1024) {
                        throw IllegalArgumentException(
                            "Image must be 5 MB or smaller"
                        )
                    }

                    val imageRequestBody = temporaryImage.asRequestBody(
                        mimeType.toMediaTypeOrNull()
                    )

                    imagePart = MultipartBody.Part.createFormData(
                        "image",
                        temporaryImage.name,
                        imageRequestBody
                    )
                }

                val response = RetrofitClient.createItemApi.createItem(
                    authorization = "Bearer $token",
                    reportType = "lost".toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                    categoryId = category!!.category_id.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull()),
                    locationId = location!!.location_id.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull()),
                    dateLostFound = selectedDateLost
                        .toRequestBody("text/plain".toMediaTypeOrNull()),
                    image = imagePart
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ReportLostActivity,
                        "Lost item reported successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()

                    Toast.makeText(
                        this@ReportLostActivity,
                        "Submission failed (HTTP ${response.code()}): " +
                                errorBody.take(180),
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ReportLostActivity,
                    "Could not submit report: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                temporaryImage?.delete()
                progressBar.visibility = View.GONE
                submitButton.isEnabled = true
            }
        }
    }

}
