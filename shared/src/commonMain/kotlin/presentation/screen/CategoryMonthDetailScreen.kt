@file:OptIn(ExperimentalMaterialApi::class)

package presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlosgub.kotlinm.charts.ChartAnimation
import com.carlosgub.kotlinm.charts.line.LineChart
import com.carlosgub.kotlinm.charts.line.LineChartData
import com.carlosgub.kotlinm.charts.line.LineChartPoint
import com.carlosgub.kotlinm.charts.line.LineChartSeries
import core.sealed.GenericState
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import model.CategoryEnum
import model.CategoryMonthDetailArgs
import model.ExpenseScreenModel
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import moe.tlaster.precompose.navigation.Navigator
import org.koin.compose.koinInject
import presentation.viewmodel.CategoryMonthDetailViewModel
import theme.ColorPrimary
import theme.ColorSeparator
import theme.Gray600
import theme.Gray900
import theme.divider_thickness
import utils.getCategoryEnumFromName
import utils.toDayString
import utils.toMoneyFormat
import utils.toMonthString
import utils.views.Loading
import utils.views.Toolbar

@Composable
fun CategoryMonthDetailScreen(
    viewModel: CategoryMonthDetailViewModel = koinInject(),
    navigator: Navigator,
    args: CategoryMonthDetailArgs
) {
    ExpenseMonthDetailContainer(
        args,
        navigator,
        viewModel
    )
}

@Composable
private fun ExpenseMonthDetailContainer(
    args: CategoryMonthDetailArgs,
    navigator: Navigator,
    viewModel: CategoryMonthDetailViewModel
) {
    val categoryEnum = getCategoryEnumFromName(args.category)
    Scaffold(
        topBar = {
            ExpenseMonthDetailToolbar(
                category = categoryEnum.categoryName,
                onBack = {
                    navigator.goBack()
                }
            )
        }
    ) { paddingValues ->
        CategoryMonthDetailObserver(viewModel, categoryEnum, args.month)
    }
}

@Composable
private fun CategoryMonthDetailObserver(
    viewModel: CategoryMonthDetailViewModel,
    categoryEnum: CategoryEnum,
    monthKey: String
) {
    when (val uiState = viewModel.uiState.collectAsStateWithLifecycle().value) {
        is GenericState.Success -> {
            CategoryMonthDetailContent(
                header = {
                    CategoryMonthDetailHeader(
                        uiState.data.monthAmount,
                        uiState.data.daySpent,
                        categoryEnum.categoryName
                    )
                },
                body = {
                    CategoryMonthDetailBody(
                        uiState.data.expenseScreenModel
                    )
                }
            )
        }

        GenericState.Loading -> {
            CategoryMonthDetailContent(
                header = {
                    Loading()
                },
                body = {
                    Loading()
                }
            )
        }

        GenericState.Initial -> {
            viewModel.getMonthDetail(
                categoryEnum,
                monthKey
            )
        }

        else -> Unit
    }
}

@Composable
fun CategoryMonthDetailContent(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    Column {
        Column(
            modifier = Modifier.fillMaxWidth().weight(0.35f)
        ) {
            header()
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(0.65f)
        ) {
            body()
        }
    }
}

@Composable
fun CategoryMonthDetailBody(
    list: List<ExpenseScreenModel>
) {
    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        elevation = 8.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 24.dp,
                    start = 24.dp,
                    end = 24.dp
                )
        ) {
            itemsIndexed(list) { count, expense ->
                Column {
                    if (count != 0) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = divider_thickness,
                            color = ColorSeparator
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(vertical = 16.dp)

                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 16.dp)
                        ) {
                            Text(
                                text = expense.note,
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = expense.day,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(top = 4.dp),
                                color = Gray600,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = (expense.amount / 100.0).toMoneyFormat(),
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMonthDetailHeader(
    monthTotal: Int,
    daySpent: Map<LocalDateTime, Int>,
    category: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Column {
                Spacer(modifier = Modifier.weight(0.2f))
                ChartCategoryMonth(daySpent)
            }
            Column {
                Text(
                    text = (monthTotal / 100.0).toMoneyFormat(),
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                    color = Gray900,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartCategoryMonth(daySpent: Map<LocalDateTime, Int>) {
    val lineData = remember {
        LineChartData(
            series = listOf(
                LineChartSeries(
                    dataName = "Expense",
                    lineColor = ColorPrimary,
                    listOfPoints = daySpent.map { day ->
                        LineChartPoint(
                            x = day.key.toInstant(TimeZone.currentSystemDefault())
                                .toEpochMilliseconds(),
                            y = (day.value / 100.0).toFloat()
                        )
                    }
                )
            )
        )
    }
    LineChart(
        lineChartData = lineData,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
        xAxisLabel = {
            val day: LocalDateTime = Instant.fromEpochMilliseconds(it as Long)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            Text(
                fontSize = 12.sp,
                text = "${day.dayOfMonth.toDayString()}/${day.month.toMonthString()}",
                textAlign = TextAlign.Center
            )
        },
        yAxisLabel = {
        },
        overlayHeaderLabel = {
            val day: LocalDateTime = Instant.fromEpochMilliseconds(it as Long)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            Text(
                text = "${day.dayOfMonth.toDayString()}/${day.month.toMonthString()}",
                style = MaterialTheme.typography.overline
            )
        },
        overlayDataEntryLabel = { _, value ->
            Text(
                text = "$value"
            )
        },
        animation = ChartAnimation.Sequenced()
    )
}

@Composable
private fun ExpenseMonthDetailToolbar(
    category: String,
    onBack: () -> Unit
) {
    Toolbar(
        backgroundColor = Color.White,
        elevation = 0.dp,
        title = category,
        hasNavigationIcon = true,
        navigation = onBack,
        contentColor = Color.Black
    )
}
