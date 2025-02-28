package presentation.viewmodel.editexpense

import domain.model.CategoryEnum
import utils.toMoneyFormat

data class EditExpenseScreenState(
    val category: CategoryEnum = CategoryEnum.FOOD,
    val amountField: String = 0.0.toMoneyFormat(),
    val amount: Double = 0.0,
    val showDateError: Boolean = false,
    val showNoteError: Boolean = false,
    val showError: Boolean = false,
    val note: String = "",
    val date: String = "",
    val dateInMillis: Long = 0L,
    val initialDataLoaded: Boolean = false,
    val showLoading: Boolean = false,
    val id: Long = 0L,
    val monthKey: String = "",
)
