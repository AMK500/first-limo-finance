package com.firstlimo.finance.data.remote

import com.firstlimo.finance.data.model.FinancialTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * استماع حي ولحظي لتحديثات قاعدة البيانات السحابية
     * يضمن المزامنة الفورية بين عدة هواتف
     */
    fun listenToUserTransactions(userId: String): Flow<List<FinancialTransaction>> = callbackFlow {
        if (userId.isEmpty()) {
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

    /**
     * رفع أو تحديث عملية إلى السحابة
     */
    suspend fun uploadTransaction(userId: String, transaction: FinancialTransaction) {
        if (userId.isEmpty()) return

        val docRef = firestore.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transaction.id)

        docRef.set(transaction.toMap(), SetOptions.merge()).await()
    }

    /**
     * رفع قائمة من العمليات غير المتزامنة (دفعة واحدة / Batch)
     */
    suspend fun uploadBatchTransactions(userId: String, transactions: List<FinancialTransaction>) {
        if (userId.isEmpty() || transactions.isEmpty()) return

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
}
