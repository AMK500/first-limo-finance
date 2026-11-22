package com.firstlimo.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CustomerEntity::class, DebtTransactionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `customers` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT,
                        `address` TEXT,
                        `notes` TEXT,
                        `isSystem` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debt_transactions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `customerId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `dateString` TEXT NOT NULL,
                        `timeString` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `receiptImagePath` TEXT,
                        `linkedFinancialTransactionId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "first_limo_finance.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
