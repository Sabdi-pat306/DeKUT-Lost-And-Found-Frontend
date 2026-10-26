package ac.ke.dkut.lostfound

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class ReportFoundActivity : AppCompatActivity() {

    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var locationSpinner: Spinner
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var selectedImageText: TextView
    private lateinit var itemImagePreview: ImageView
    private lateinit var dateButton: Button

    private var categories: List<ItemCategory> = emptyList()
    private var locations: List<CampusLocation> = emptyList()
    private var selectedDateFound = ""
    private var selectedImageUri: Uri? = null

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

        descriptionInput = findViewById(R.id.itemDescriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        locationSpinner = findViewById(R.id.locationSpinner)
        submitButton = findViewById(R.id.submitLostItemButton)
        submitButton.text = "Submit Found Item Report"
        progressBar = findViewById(R.id.submitProgress)
        selectedImageText = findViewById(R.id.selectedImageText)
        itemImagePreview = findViewById(R.id.itemImagePreview)
        dateButton = findViewById(R.id.dateButton)

        title = "Report a Found Item"

        findViewById<TextView>(R.id.dateLabel).text = "Date found *"
        dateButton.text = "Choose date found"

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDateFound = String.format(
                        Locale.US, "%04d-%02d-%02d",
                        year, month + 1, day
                    )
                    dateButton.text = selectedDateFound
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
        submitButton.setOnClickListener { submitFoundItem() }

        lifecycleScope.launch {
            loadDropdownData(token)
        }
    }

    private suspend fun loadDropdownData(token: String) {
        try {
            val auth = "Bearer $token"
            val categoryResponse =
                RetrofitClient.itemMetadataApi.getCategories()
            val locationResponse =
                RetrofitClient.itemMetadataApi.getLocations()

            if (!categoryResponse.isSuccessful ||
                !locationResponse.isSuccessful
            ) {
                Toast.makeText(
                    this, "Could not load categories or locations",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            categories = categoryResponse.body().orEmpty()
            locations = locationResponse.body().orEmpty()

            if (categories.isEmpty() || locations.isEmpty()) return

            categorySpinner.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item,
                categories.map { it.category_name }
            ).apply {
                setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            locationSpinner.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item,
                locations.map { it.location_name }
            ).apply {
                setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            submitButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(
                this, "Could not load form: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun submitFoundItem() {
        val description = descriptionInput.text.toString().trim()

        if (description.isBlank()) {
            descriptionInput.error = "Describe the found item"
            return
        }
        if (selectedDateFound.isBlank()) {
            Toast.makeText(this, "Select the date found", Toast.LENGTH_SHORT).show()
            return
        }
        if (categories.isEmpty() || locations.isEmpty()) return

        val token = TokenManager.getToken(this) ?: return
        val category = categories[categorySpinner.selectedItemPosition]
        val location = locations[locationSpinner.selectedItemPosition]

        submitButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            var temporaryImage: File? = null

            try {
                var imagePart: MultipartBody.Part? = null
                val uri = selectedImageUri

                if (uri != null) {
                    val mime = contentResolver.getType(uri) ?: "image/jpeg"
                    val extension = when (mime) {
                        "image/png" -> ".png"
                        "image/webp" -> ".webp"
                        else -> ".jpg"
                    }

                    temporaryImage = File.createTempFile(
                        "found_item_", extension, cacheDir
                    )

                    contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Cannot read selected image" }
                        FileOutputStream(temporaryImage).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (temporaryImage.length() > 5 * 1024 * 1024) {
                        throw IllegalArgumentException("Image must be 5 MB or smaller")
                    }

                    imagePart = MultipartBody.Part.createFormData(
                        "image",
                        temporaryImage.name,
                        temporaryImage.asRequestBody(mime.toMediaTypeOrNull())
                    )
                }

                fun field(value: String) =
                    value.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.createItemApi.createItem(
                    authorization = "Bearer $token",
                    reportType = field("found"),
                    description = field(description),
                    categoryId = field(category.category_id.toString()),
                    locationId = field(location.location_id.toString()),
                    dateLostFound = field(selectedDateFound),
                    image = imagePart
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ReportFoundActivity,
                        "Found item reported successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ReportFoundActivity,
                        "Submission failed: HTTP ${response.code()} " +
                                response.errorBody()?.string().orEmpty().take(150),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ReportFoundActivity,
                    "Could not submit report: ${e.message}",
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
