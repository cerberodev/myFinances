package domain.usecase

import app.cash.turbine.test
import core.sealed.GenericState
import data.repository.FakeFinanceRepositoryImpl
import data.repository.source.database.monthIncomeDetailScreenModel
import kotlinx.coroutines.test.runTest
import utils.getCurrentMonthKey
import kotlin.test.Test
import kotlin.test.assertEquals

class GetIncomeMonthDetailUseCaseTest {
    private val fakeFinanceRepositoryImpl = FakeFinanceRepositoryImpl()
    private val getIncomeMonthDetailUseCase = GetIncomeMonthDetailUseCase(fakeFinanceRepositoryImpl)

    @Test
    fun `Get Income Month Detail success`() =
        runTest {
            val expected = monthIncomeDetailScreenModel
            getIncomeMonthDetailUseCase(GetIncomeMonthDetailUseCase.Params(monthKey = getCurrentMonthKey()))
                .test {
                    assertEquals(expected, (awaitItem() as GenericState.Success).data)
                    awaitComplete()
                }
        }
}
