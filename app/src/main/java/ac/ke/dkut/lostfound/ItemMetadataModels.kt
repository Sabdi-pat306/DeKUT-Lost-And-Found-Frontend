package ac.ke.dkut.lostfound

data class ItemCategory(
    val category_id: Int,
    val category_name: String,
    val description: String
)

data class CampusLocation(
    val location_id: Int,
    val location_name: String,
    val description: String
)
