package com.banktracker.data

import androidx.room.*

data class MonthlyTotal(val date: String, val total: Long)
data class CategoryTotal(val category: String, val total: Long)
data class DailyTotal(val date: String, val total: Long)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tx: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun getSinceSync(from: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE type = 'DEBIT' AND timestamp >= :from ORDER BY timestamp DESC")
    fun getExpensesSinceSync(from: Long): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :from AND timestamp <= :to")
    fun getTotalExpense(from: Long, to: Long): Long?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND timestamp >= :from AND timestamp <= :to")
    fun getTotalIncome(from: Long, to: Long): Long?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :from AND timestamp <= :to")
    fun getCount(from: Long, to: Long): Int

    @Query("""
        SELECT date, SUM(amount) as total 
        FROM transactions 
        WHERE type = 'DEBIT' AND timestamp >= :from AND timestamp <= :to
        GROUP BY date ORDER BY date ASC
    """)
    fun getDailyExpense(from: Long, to: Long): List<DailyTotal>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE type = 'DEBIT' AND timestamp >= :from AND timestamp <= :to
        GROUP BY category ORDER BY total DESC
    """)
    fun getCategoryExpense(from: Long, to: Long): List<CategoryTotal>
}
