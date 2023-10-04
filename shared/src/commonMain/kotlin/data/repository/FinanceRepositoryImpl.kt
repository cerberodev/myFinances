package data.repository

import core.mapper.ResultMapper
import core.network.ResponseResult
import core.sealed.GenericState
import data.firebase.FirebaseFinance
import domain.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import model.CategoryEnum
import model.ExpenseScreenModel
import model.FinanceScreenExpenses
import model.FinanceScreenModel
import model.MonthDetailScreenModel
import model.MonthExpense
import utils.createLocalDateTime
import utils.getCategoryEnumFromName
import utils.isLeapYear
import utils.monthLength
import utils.toLocalDate
import utils.toNumberOfTwoDigits
import kotlin.math.roundToInt

class FinanceRepositoryImpl(
    private val firebaseFinance: FirebaseFinance
) : FinanceRepository {

    override suspend fun getFinance(monthKey: String): GenericState<FinanceScreenModel> =
        withContext(Dispatchers.Default) {
            val expenses = async { firebaseFinance.getAllMonthExpenses(monthKey) }.await()
            val income = async { firebaseFinance.getAllMonthIncome(monthKey) }.await()

            if (expenses is ResponseResult.Success && income is ResponseResult.Success) {
                val expenseTotal = expenses.data.sumOf { it.amount }
                val incomeTotal = income.data.sumOf { it.amount }
                val expenseList =
                    expenses.data.groupBy { it.category }
                        .map {
                            val amount = it.value.sumOf { it.amount }
                            FinanceScreenExpenses(
                                category = getCategoryEnumFromName(it.key),
                                amount = amount,
                                count = it.value.size,
                                percentage = (amount / expenseTotal.toFloat() * 100).roundToInt()
                            )
                        }.sortedByDescending { it.percentage }
                val incomeList =
                    income.data.groupBy { it.category }
                        .map {
                            val amount = it.value.sumOf { it.amount }
                            FinanceScreenExpenses(
                                category = getCategoryEnumFromName(it.key),
                                amount = amount,
                                count = it.value.size,
                                percentage = (amount / incomeTotal.toFloat() * 100).roundToInt()
                            )
                        }.sortedByDescending { it.amount }
                val monthExpense =
                    MonthExpense(
                        incomeTotal = incomeTotal / 100.0,
                        percentage = if (incomeTotal != 0) expenseTotal * 100 / incomeTotal else 100
                    )
                GenericState.Success(
                    FinanceScreenModel(
                        expenseAmount = expenseTotal,
                        expenses = expenseList,
                        income = incomeList,
                        localDateTime = createLocalDateTime(
                            year = monthKey.substring(2, 6).toInt(),
                            monthNumber = monthKey.substring(0, 2).trimStart('0').toInt()
                        ),
                        monthExpense = monthExpense
                    )
                )
            } else {
                if (expenses is ResponseResult.Error) {
                    GenericState.Error(expenses.error.message.orEmpty())
                } else {
                    GenericState.Error((income as ResponseResult.Error).error.message.orEmpty())
                }
            }
        }

    override suspend fun createExpense(
        amount: Int,
        category: String,
        note: String,
        dateInMillis: Long
    ): GenericState<Unit> =
        withContext(Dispatchers.Default) {
            ResultMapper.toGenericState(
                firebaseFinance.createExpense(
                    amount = amount,
                    category = category,
                    note = note,
                    dateInMillis = dateInMillis
                )
            )
        }

    override suspend fun createIncome(
        amount: Int,
        note: String,
        dateInMillis: Long
    ): GenericState<Unit> =
        withContext(Dispatchers.Default) {
            ResultMapper.toGenericState(
                firebaseFinance.createIncome(
                    amount = amount,
                    note = note,
                    dateInMillis = dateInMillis
                )
            )
        }


    override suspend fun editExpense(
        amount: Int,
        category: String,
        note: String,
        dateInMillis: Long,
        id: String
    ): GenericState<Unit> =
        withContext(Dispatchers.Default) {
            ResultMapper.toGenericState(
                firebaseFinance.editExpense(
                    amount = amount,
                    category = category,
                    note = note,
                    dateInMillis = dateInMillis,
                    id = id
                )
            )
        }

    override suspend fun editIncome(
        amount: Int,
        note: String,
        dateInMillis: Long,
        id: String
    ): GenericState<Unit> =
        withContext(Dispatchers.Default) {
            ResultMapper.toGenericState(
                firebaseFinance.editIncome(
                    amount = amount,
                    note = note,
                    dateInMillis = dateInMillis,
                    id = id
                )
            )
        }

    override suspend fun getCategoryMonthDetail(
        categoryEnum: CategoryEnum,
        monthKey: String
    ): GenericState<MonthDetailScreenModel> =
        withContext(Dispatchers.Default) {
            when (val result = firebaseFinance.getCategoryMonthDetail(categoryEnum, monthKey)) {
                is ResponseResult.Error -> GenericState.Error(result.error.message.orEmpty())
                is ResponseResult.Success -> {
                    val monthAmount = result.data.sumOf { it.amount }
                    val expenseScreenModelList = result.data.map { expense ->
                        val localDate: LocalDate = expense.dateInMillis.toLocalDate()
                        val localDateTime = createLocalDateTime(
                            year = localDate.year,
                            monthNumber = localDate.monthNumber,
                            dayOfMonth = localDate.dayOfMonth
                        )
                        val dayOfMonth = localDateTime.dayOfMonth.toNumberOfTwoDigits()
                        val month =
                            localDateTime.month.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        val year =
                            localDateTime.year
                        ExpenseScreenModel(
                            id = expense.id.orEmpty(),
                            amount = expense.amount,
                            userId = expense.userId,
                            note = expense.note.replaceFirstChar { it.uppercase() },
                            category = expense.category,
                            localDateTime = localDateTime,
                            date = "$dayOfMonth $month $year"
                        )
                    }
                    val date = createLocalDateTime(
                        year = monthKey.substring(2, 6).toInt(),
                        monthNumber = monthKey.substring(0, 2).trimStart('0').toInt()
                    )
                    val daySpent =
                        (1..date.monthNumber.monthLength(isLeapYear(date.year))).map { day ->
                            val dateInternal = createLocalDateTime(
                                year = monthKey.substring(2, 6).toInt(),
                                monthNumber = monthKey.substring(0, 2).trimStart('0').toInt(),
                                dayOfMonth = day
                            )
                            dateInternal to expenseScreenModelList.filter { expense ->
                                expense.localDateTime == dateInternal
                            }
                                .sumOf { it.amount }
                        }.toMap()
                    GenericState.Success(
                        MonthDetailScreenModel(
                            monthAmount = monthAmount,
                            expenseScreenModel = expenseScreenModelList,
                            daySpent = daySpent
                        )
                    )
                }
            }
        }

    override suspend fun getMonths(): GenericState<Map<Int, List<LocalDateTime>>> =
        withContext(Dispatchers.Default) {
            when (val result = firebaseFinance.getMonths()) {
                is ResponseResult.Success -> {
                    GenericState.Success(
                        result.data.map { month ->
                            createLocalDateTime(
                                year = month.year.toInt(),
                                monthNumber = month.month.trimStart('0').toInt()
                            )
                        }.groupBy {
                            it.year
                        }
                    )
                }

                is ResponseResult.Error -> GenericState.Error(result.error.message.toString())
            }
        }
}
