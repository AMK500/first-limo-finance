package com.firstlimo.finance.data.remote

import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.data.local.DebtTransactionEntity
import com.firstlimo.finance.data.model.FinancialTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // تفعيل الحفظ الأوفلاين السحابي للعمل دون إنترنت
        try {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (_: Exception) {}
    }
) {

    // ==========================================
    // 1. المعاملات المالية (Financial Transactions)
    // ==========================================

    fun listenToUserTransactions(userId: String): Flow<List<FinancialTransaction>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collection = firestore.collection("users")
            .document(userId)
            .collection("transactions")

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { FinancialTransaction.fromMap(it) }
                }
                trySend(transactions)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun fetchAllTransactions(userId: String): List<FinancialTransaction> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { FinancialTransaction.fromMap(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun uploadTransaction(userId: String, transaction: FinancialTransaction) {
        if (userId.isBlank()) return
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transaction.id)

        docRef.set(transaction.toMap(), SetOptions.merge()).await()
    }

    suspend fun uploadBatchTransactions(userId: String, transactions: List<FinancialTransaction>) {
        if (userId.isBlank() || transactions.isEmpty()) return
        val batch = firestore.batch()
        val userTransactionsRef = firestore.collection("users")
            .document(userId)
            .collection("transactions")

        for (tx in transactions) {
            val docRef = userTransactionsRef.document(tx.id)
            batch.set(docRef, tx.toMap(), SetOptions.merge())
        }
        batch.commit().await()
    }

    // ==========================================
    // 2. العملاء في دفتر الديون (Customers)
    // ==========================================

    fun listenToUserCustomers(userId: String): Flow<List<CustomerEntity>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collection = firestore.collection("users")
            .document(userId)
            .collection("customers")

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val customers = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    try {
                        CustomerEntity(
                            id = data["id"] as? String ?: doc.id,
                            userId = data["userId"] as? String ?: userId,
                            name = data["name"] as? String ?: "",
                            phone = data["phone"] as? String,
                            address = data["address"] as? String,
                            notes = data["notes"] as? String,
                            isSystem = (data["isSystem"] as? Boolean) ?: false,
                            isPinned = (data["isPinned"] as? Boolean) ?: false,
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            isDeleted = (data["isDeleted"] as? Boolean) ?: false
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                trySend(customers)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun fetchAllCustomers(userId: String): List<CustomerEntity> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("customers")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    CustomerEntity(
                        id = data["id"] as? String ?: doc.id,
                        userId = data["userId"] as? String ?: userId,
                        name = data["name"] as? String ?: "",
                        phone = data["phone"] as? String,
                        address = data["address"] as? String,
                        notes = data["notes"] as? String,
                        isSystem = (data["isSystem"] as? Boolean) ?: false,
                        isPinned = (data["isPinned"] as? Boolean) ?: false,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isDeleted = (data["isDeleted"] as? Boolean) ?: false
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun uploadCustomer(userId: String, customer: CustomerEntity) {
        if (userId.isBlank()) return
        val map = mapOf(
            "id" to customer.id,
            "userId" to userId,
            "name" to customer.name,
            "phone" to customer.phone,
            "address" to customer.address,
            "notes" to customer.notes,
            "isSystem" to customer.isSystem,
            "isPinned" to customer.isPinned,
            "createdAt" to customer.createdAt,
            "updatedAt" to customer.updatedAt,
            "isDeleted" to customer.isDeleted
        )
        firestore.collection("users")
            .document(userId)
            .collection("customers")
            .document(customer.id)
            .set(map, SetOptions.merge())
            .await()
    }

    suspend fun uploadBatchCustomers(userId: String, customers: List<CustomerEntity>) {
        if (userId.isBlank() || customers.isEmpty()) return
        val batch = firestore.batch()
        val ref = firestore.collection("users").document(userId).collection("customers")

        for (c in customers) {
            val map = mapOf(
                "id" to c.id,
                "userId" to userId,
                "name" to c.name,
                "phone" to c.phone,
                "address" to c.address,
                "notes" to c.notes,
                "isSystem" to c.isSystem,
                "isPinned" to c.isPinned,
                "createdAt" to c.createdAt,
                "updatedAt" to c.updatedAt,
                "isDeleted" to c.isDeleted
            )
            batch.set(ref.document(c.id), map, SetOptions.merge())
        }
        batch.commit().await()
    }

    // ==========================================
    // 3. معاملات الديون (Debt Transactions)
    // ==========================================

    fun listenToUserDebtTransactions(userId: String): Flow<List<DebtTransactionEntity>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collection = firestore.collection("users")
            .document(userId)
            .collection("debt_transactions")

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val transactions = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    try {
                        DebtTransactionEntity(
                            id = data["id"] as? String ?: doc.id,
                            userId = data["userId"] as? String ?: userId,
                            customerId = data["customerId"] as? String ?: "",
                            type = data["type"] as? String ?: "TOOK",
                            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                            dateString = data["dateString"] as? String ?: "",
                            timeString = data["timeString"] as? String ?: "",
                            notes = data["notes"] as? String ?: "",
                            receiptImagePath = data["receiptImagePath"] as? String,
                            linkedFinancialTransactionId = data["linkedFinancialTransactionId"] as? String,
                            dueDateString = data["dueDateString"] as? String,
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            isDeleted = (data["isDeleted"] as? Boolean) ?: false
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                trySend(transactions)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun fetchAllDebtTransactions(userId: String): List<DebtTransactionEntity> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("debt_transactions")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    DebtTransactionEntity(
                        id = data["id"] as? String ?: doc.id,
                        userId = data["userId"] as? String ?: userId,
                        customerId = data["customerId"] as? String ?: "",
                        type = data["type"] as? String ?: "TOOK",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        dateString = data["dateString"] as? String ?: "",
                        timeString = data["timeString"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        receiptImagePath = data["receiptImagePath"] as? String,
                        linkedFinancialTransactionId = data["linkedFinancialTransactionId"] as? String,
                        dueDateString = data["dueDateString"] as? String,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isDeleted = (data["isDeleted"] as? Boolean) ?: false
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun uploadDebtTransaction(userId: String, tx: DebtTransactionEntity) {
        if (userId.isBlank()) return
        val map = mapOf(
            "id" to tx.id,
            "userId" to userId,
            "customerId" to tx.customerId,
            "type" to tx.type,
            "amount" to tx.amount,
            "dateString" to tx.dateString,
            "timeString" to tx.timeString,
            "notes" to tx.notes,
            "receiptImagePath" to tx.receiptImagePath,
            "linkedFinancialTransactionId" to tx.linkedFinancialTransactionId,
            "dueDateString" to tx.dueDateString,
            "createdAt" to tx.createdAt,
            "updatedAt" to tx.updatedAt,
            "isDeleted" to tx.isDeleted
        )
        firestore.collection("users")
            .document(userId)
            .collection("debt_transactions")
            .document(tx.id)
            .set(map, SetOptions.merge())
            .await()
    }

    suspend fun uploadBatchDebtTransactions(userId: String, transactions: List<DebtTransactionEntity>) {
        if (userId.isBlank() || transactions.isEmpty()) return
        val batch = firestore.batch()
        val ref = firestore.collection("users").document(userId).collection("debt_transactions")

        for (tx in transactions) {
            val map = mapOf(
                "id" to tx.id,
                "userId" to userId,
                "customerId" to tx.customerId,
                "type" to tx.type,
                "amount" to tx.amount,
                "dateString" to tx.dateString,
                "timeString" to tx.timeString,
                "notes" to tx.notes,
                "receiptImagePath" to tx.receiptImagePath,
                "linkedFinancialTransactionId" to tx.linkedFinancialTransactionId,
                "dueDateString" to tx.dueDateString,
                "createdAt" to tx.createdAt,
                "updatedAt" to tx.updatedAt,
                "isDeleted" to tx.isDeleted
            )
            batch.set(ref.document(tx.id), map, SetOptions.merge())
        }
        batch.commit().await()
    }
}
