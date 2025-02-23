package domain.model

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItem(
    val name: String,
    val icon: ImageVector,
    val onItemClicked: () -> Unit,
)
