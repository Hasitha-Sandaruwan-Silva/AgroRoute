package com.agroroute.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic Firestore CRUD wrapper that provides reusable read/write/delete operations
 * for any collection, converting Firestore callbacks into Kotlin coroutines and Flows.
 */
@Singleton
class FirestoreService @Inject constructor(
    @PublishedApi
    internal val firestore: FirebaseFirestore
) {

    /**
     * Returns a [Flow] that emits a list of [T] whenever the collection changes.
     * Network errors are caught and re-thrown so callers can wrap with [safeCall].
     */
    inline fun <reified T : Any> collectionFlow(collection: String): Flow<List<T>> =
        callbackFlow {
            val listener = firestore.collection(collection)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents?.mapNotNull { it.toObject(T::class.java) }
                        ?: emptyList()
                    trySend(items)
                }
            awaitClose { listener.remove() }
        }

    /**
     * Returns a [Flow] emitting a single document [T] (or null) on each change.
     */
    inline fun <reified T : Any> documentFlow(collection: String, docId: String): Flow<T?> =
        callbackFlow {
            val listener = firestore.collection(collection).document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(T::class.java))
                }
            awaitClose { listener.remove() }
        }

    /**
     * Returns documents in [collection] where field [field] equals [value] as a [Flow].
     */
    inline fun <reified T : Any> queryFlow(
        collection: String,
        field: String,
        value: Any
    ): Flow<List<T>> = callbackFlow {
        val listener = firestore.collection(collection)
            .whereEqualTo(field, value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.toObject(T::class.java) }
                    ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /** Writes (or merges) [data] to [collection]/[docId]. Suspends until acknowledged. */
    suspend fun <T : Any> set(collection: String, docId: String, data: T) {
        firestore.collection(collection).document(docId)
            .set(data, SetOptions.merge())
            .await()
    }

    /** Deletes the document at [collection]/[docId]. Suspends until acknowledged. */
    suspend fun delete(collection: String, docId: String) {
        firestore.collection(collection).document(docId).delete().await()
    }

    /** Reads a single document once (not reactive). Returns null if not found. */
    suspend inline fun <reified T : Any> getOnce(collection: String, docId: String): T? =
        firestore.collection(collection).document(docId).get().await()
            .toObject(T::class.java)
}
