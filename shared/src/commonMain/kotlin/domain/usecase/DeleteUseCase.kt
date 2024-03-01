package domain.usecase

import domain.repository.FinanceRepository
import model.FinanceEnum
import presentation.viewmodel.EditSideEffects

class DeleteUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend fun delete(params: Params): EditSideEffects =
        financeRepository.delete(
            financeEnum = params.financeEnum,
            id = params.id,
            monthKey = params.monthKey
        )

    data class Params(
        val financeEnum: FinanceEnum,
        val id: Long,
        val monthKey: String
    )
}
