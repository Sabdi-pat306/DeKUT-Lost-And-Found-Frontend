package ac.ke.dkut.lostfound

data class ItemReport(
    val item_id: Int,
    val report_type: String,
    val item_description: String,
    val category_id: Int,
    val location_id: Int,
    val date_lost_found: String,
    val item_status: String,
    val date_reported: String,
    val image: String?,
    val date_created: String
)