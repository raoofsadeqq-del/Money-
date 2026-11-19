package com.example.finance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.*
import com.example.finance.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

data class UiState(val month: YearMonth = YearMonth.now(), val entries: List<MoneyEntry> = emptyList(),
                   val summary: Summary = Summary(0.0, 0.0, 0.0, emptyMap()),
                   val insights: List<Insight> = emptyList(), val bills: List<Bill> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val month = MutableStateFlow(YearMonth.now())

    private fun start(m: YearMonth) = m.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun end(m: YearMonth) = m.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

    val state: StateFlow<UiState> = month.flatMapLatest { m ->
        combine(dao.between(start(m), end(m)), dao.between(start(m.minusMonths(1)), end(m.minusMonths(1))), dao.unpaidBills()) { cur, prev, bills ->
            UiState(m, cur, InsightsEngine.summarize(cur), InsightsEngine.analyze(cur, prev), bills)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun shift(delta: Long) { month.value = month.value.plusMonths(delta) }
    fun add(amount: Double, type: EntryType, cat: Category, note: String) = viewModelScope.launch {
        dao.add(MoneyEntry(amount = amount, type = type, category = cat, note = note, dateMillis = System.currentTimeMillis()))
    }
    fun addBill(title: String, amount: Double, due: Long) = viewModelScope.launch { dao.addBill(Bill(title = title, amount = amount, dueDateMillis = due)) }
}
