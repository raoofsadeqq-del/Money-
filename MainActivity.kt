package com.example.finance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.finance.R
import com.example.finance.data.*
import com.example.finance.domain.Insight
import com.example.finance.work.BillReminderWorker

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillReminderWorker.schedule(this)
        setContent { MaterialTheme { Dashboard(vm) } }
    }
}

@Composable
fun Dashboard(vm: MainViewModel) {
    val s by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton({ showAdd = true }) { Text("+") } }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ vm.shift(-1) }) { Text("‹") }
                    Text(s.month.toString(), style = MaterialTheme.typography.titleLarge)
                    TextButton({ vm.shift(1) }) { Text("›") }
                }
            }
            item { SummaryCard(s) }
            item { CategoryChart(s.summary.byCategory) }
            item { Text(stringResource(R.string.insights), style = MaterialTheme.typography.titleMedium) }
            items(s.insights) { InsightCard(it) }
            item { Text(stringResource(R.string.upcoming_bills), style = MaterialTheme.typography.titleMedium) }
            items(s.bills) { Text("${it.title} — ${it.amount}") }
        }
    }
    if (showAdd) AddEntryDialog({ showAdd = false }) { a, t, c, n -> vm.add(a, t, c, n); showAdd = false }
}

@Composable
fun SummaryCard(s: UiState) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
        Text("${stringResource(R.string.income)}: ${"%,.0f".format(s.summary.income)}")
        Text("${stringResource(R.string.spent)}: ${"%,.0f".format(s.summary.spent)}")
        Text("${stringResource(R.string.saved)}: ${"%,.0f".format(s.summary.saved)}")
        Text("${stringResource(R.string.net)}: ${"%,.0f".format(s.summary.net)}", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun CategoryChart(data: Map<Category, Double>) {
    val max = (data.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val colors = listOf(Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00), Color(0xFF8E24AA))
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width / (data.size.coerceAtLeast(1) * 2)
        data.entries.forEachIndexed { i, (_, v) ->
            val h = (v / max).toFloat() * size.height
            drawRect(colors[i % colors.size], Offset(i * 2 * w + w / 2, size.height - h), Size(w, h))
        }
    }
}

@Composable
fun InsightCard(i: Insight) {
    val msg = when (i) {
        is Insight.LowSavings -> stringResource(R.string.ins_low_savings, i.ratePct)
        is Insight.Overspending -> stringResource(R.string.ins_overspend, "%,.0f".format(i.gap))
        is Insight.VariableSpike -> stringResource(R.string.ins_variable, i.pct)
        is Insight.HeavyObligations -> stringResource(R.string.ins_obligations, i.pct)
        Insight.OnTrack -> stringResource(R.string.ins_ok)
    }
    Card(Modifier.fillMaxWidth()) { Text(msg, Modifier.padding(12.dp)) }
}

@Composable
fun AddEntryDialog(onDismiss: () -> Unit, onSave: (Double, EntryType, Category, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Category.VARIABLE) }
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                val type = if (cat == Category.SALARY || cat == Category.OTHER_INCOME) EntryType.INCOME else EntryType.EXPENSE
                amount.toDoubleOrNull()?.let { onSave(it, type, cat, note) }
            }) { Text(stringResource(R.string.save)) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount)) })
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) })
                Category.entries.chunked(3).forEach { row ->
                    Row { row.forEach { c -> FilterChip(cat == c, { cat = c }, { Text(c.name.take(8)) }) ; Spacer(Modifier.width(4.dp)) } }
                }
            }
        })
}
