package com.example.finance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class EntryType { INCOME, EXPENSE }
// التصنيفات: الالتزامات، المتغيرة، المدخرات، الاستثمارات (+ مصادر الدخل)
enum class Category { SALARY, OTHER_INCOME, OBLIGATION, VARIABLE, SAVING, INVESTMENT }

@Entity(tableName = "entries", indices = [Index("dateMillis")])
data class MoneyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: EntryType,
    val category: Category,
    val note: String = "",
    val dateMillis: Long,
    val currency: String = "YER"
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDateMillis: Long,
    val repeatMonthly: Boolean = true,
    val remindDaysBefore: Int = 3,
    val paid: Boolean = false,
    val notes: String = ""
)

@Dao
interface FinanceDao {
    @Insert suspend fun add(e: MoneyEntry)
    @Delete suspend fun delete(e: MoneyEntry)

    @Query("SELECT * FROM entries WHERE dateMillis BETWEEN :from AND :to ORDER BY dateMillis DESC")
    fun between(from: Long, to: Long): Flow<List<MoneyEntry>>

    // بحث وفلترة متقدمة: فترة + تصنيف (اختياري) + نص في الملاحظة + نطاق مبلغ
    @Query("""SELECT * FROM entries WHERE dateMillis BETWEEN :from AND :to
              AND (:category IS NULL OR category = :category)
              AND note LIKE '%' || :q || '%'
              AND amount >= :minAmount ORDER BY dateMillis DESC""")
    fun search(from: Long, to: Long, category: Category?, q: String, minAmount: Double): Flow<List<MoneyEntry>>

    @Insert suspend fun addBill(b: Bill)
    @Update suspend fun updateBill(b: Bill)
    @Query("SELECT * FROM bills WHERE paid = 0 ORDER BY dueDateMillis") fun unpaidBills(): Flow<List<Bill>>
    @Query("SELECT * FROM bills WHERE paid = 0 AND dueDateMillis <= :limit") suspend fun dueBefore(limit: Long): List<Bill>
}

@Database(entities = [MoneyEntry::class, Bill::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao
    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: android.content.Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "finance.db").build().also { inst = it }
        }
    }
}
