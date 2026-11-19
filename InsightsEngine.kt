package com.example.finance.domain

import com.example.finance.data.*

data class Summary(val income: Double, val spent: Double, val saved: Double,
                   val byCategory: Map<Category, Double>) {
    val net get() = income - spent - saved
    val savingsRate get() = if (income > 0) saved / income else 0.0
}

sealed interface Insight {
    data class LowSavings(val ratePct: Int) : Insight
    data class Overspending(val gap: Double) : Insight
    data class VariableSpike(val pct: Int) : Insight
    data class HeavyObligations(val pct: Int) : Insight
    data object OnTrack : Insight
}

object InsightsEngine {
    fun summarize(list: List<MoneyEntry>): Summary {
        val income = list.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
        val exp = list.filter { it.type == EntryType.EXPENSE }
        val saved = exp.filter { it.category == Category.SAVING || it.category == Category.INVESTMENT }.sumOf { it.amount }
        val spent = exp.sumOf { it.amount } - saved
        return Summary(income, spent, saved, exp.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } })
    }

    // قواعد شفافة قابلة للتفسير (Rule-based) — يمكن لاحقًا استبدالها/دعمها بنموذج تعلم آلي
    fun analyze(cur: List<MoneyEntry>, prev: List<MoneyEntry>): List<Insight> {
        val c = summarize(cur); val p = summarize(prev)
        val out = mutableListOf<Insight>()
        if (c.income > 0 && c.savingsRate < 0.10) out += Insight.LowSavings((c.savingsRate * 100).toInt())
        if (c.spent + c.saved > c.income && c.income > 0) out += Insight.Overspending(c.spent + c.saved - c.income)
        val v = c.byCategory[Category.VARIABLE] ?: 0.0
        val pv = p.byCategory[Category.VARIABLE] ?: 0.0
        if (pv > 0 && v > pv * 1.2) out += Insight.VariableSpike(((v / pv - 1) * 100).toInt())
        val ob = c.byCategory[Category.OBLIGATION] ?: 0.0
        if (c.income > 0 && ob / c.income > 0.5) out += Insight.HeavyObligations((ob / c.income * 100).toInt())
        if (out.isEmpty() && c.income > 0) out += Insight.OnTrack
        return out
    }
}
