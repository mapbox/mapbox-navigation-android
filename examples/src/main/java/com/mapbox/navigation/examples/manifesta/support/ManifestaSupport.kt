package com.mapbox.navigation.examples.manifesta.support

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.examples.manifesta.model.domain.LocationCollection
import com.mapbox.navigation.examples.manifesta.model.entity.LocationCollectionEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ManifestaSupport {

    const val MANIFESTA_FIELD_USER_ID = "userId"
    const val MANIFESTA_COLLECTION_USERS = "users"
    const val MANIFESTA_COLLECTION_LOCATIONS = "locations"
    const val MANIFESTA_COLLECTION_LOCATION_COLLECTIONS = "locationCollections"

    fun getFirestore(): FirebaseFirestore {
        val fsSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        return FirebaseFirestore.getInstance().also {
            it.firestoreSettings = fsSettings
        }
    }

    fun getCollectionRef(collectionName: String): CollectionReference = getFirestore().collection(collectionName)

    fun userBasedQueryFactory(collectionName: String, userId: String): Query {
        val coll = getFirestore().collection(collectionName)
        return coll.whereEqualTo(MANIFESTA_FIELD_USER_ID, userId)
    }

    suspend fun queryCloudStore(query: Query): Expected<Throwable, List<Map<String, Any>>> {
        return suspendCoroutine { continuation ->
            query.get()
                .addOnFailureListener { throwable -> continuation.resume(ExpectedFactory.createError(throwable)) }
                .addOnSuccessListener {
                    val resultOfQuery: List<Map<String, Any>> = it.documents.map { docSnap ->
                        docSnap.data ?: mapOf()
                    }
                    continuation.resume(ExpectedFactory.createValue(resultOfQuery))
                }
        }
    }

    suspend fun getDocumentFromCloudStore(docReference: DocumentReference): Expected<Throwable, Map<String, Any>> {
        return suspendCoroutine { continuation ->
            docReference.get()
                .addOnFailureListener { throwable -> continuation.resume(ExpectedFactory.createError(throwable)) }
                .addOnSuccessListener {
                    continuation.resume(ExpectedFactory.createValue(it.data ?: mapOf()))
                }
        }
    }

    // tailrec suspend fun getDocumentsFromCloudStore(
    //     items: List<DocumentReference>,
    //     resultList: List<Expected<Throwable, Map<String, Any>>>
    // ): List<Expected<Throwable, Map<String, Any>>> = when (items.isEmpty()) {
    //     true -> resultList
    //     false -> {
    //         val firstItem = items.first()
    //         val result = getDocumentFromCloudStore(firstItem)
    //         getDocumentsFromCloudStore(items.drop(1), resultList.plus(result))
    //     }
    // }

    // tailrec fun <A, B> collectionMapper(items: List<Map<A, Any>>, mapper: (Map<A, Any>) -> B, resultList: List<B>): List<B> =
    //     when (items.isEmpty()) {
    //         true -> resultList
    //         false -> {
    //             val firstItem = items.first()
    //             val res = mapper.invoke(firstItem)
    //             collectionMapper(items.drop(1), mapper, resultList.plus(res))
    //         }
    //     }

    // tailrec fun documentIdsToDocumentReferences(
    //     items: List<String>,
    //     collectionName: String,
    //     resultList: List<DocumentReference>
    // ): List<DocumentReference> =
    //     when (items.isEmpty()) {
    //         true -> resultList
    //         false -> {
    //             val firstItem = items.first()
    //             val query = getCollectionRef(collectionName).document(firstItem)
    //             documentIdsToDocumentReferences(items.drop(1), collectionName, resultList.plus(query))
    //         }
    //     }

    // tailrec fun <E, V> valuesFromExpectedItems(
    //     items: List<Expected<E, V>>,
    //     resultList: List<V>
    // ): List<V> =
    //     when (items.isEmpty()) {
    //         true -> resultList
    //         false -> {
    //             val firstItem = items.first()
    //             if (firstItem.isValue && firstItem.value != null) {
    //                 valuesFromExpectedItems(items.drop(1),  resultList.plus(firstItem.value!!))
    //             } else {
    //                 valuesFromExpectedItems(items.drop(1),  resultList)
    //             }
    //         }
    //     }

    suspend fun deleteEntity(entityId: String, collectionName: String): Expected<Throwable, String> = suspendCancellableCoroutine { continuation ->
        getCollectionRef(collectionName)
            .document(entityId)
            .delete()
            .addOnFailureListener { throwable ->  continuation.resume(ExpectedFactory.createError(throwable)) }
            .addOnSuccessListener { continuation.resume(ExpectedFactory.createValue(entityId)) }
    }

    fun LocationCollection.toEntity(): LocationCollectionEntity {
        return LocationCollectionEntity(
            this.id,
            this.name,
            this.locations.map { it.id }
        )
    }

    fun getDocumentReference(
        collectionId: String,
        entityAsMap: Map<String, Any>,
        entityIdField: String
    ): Pair<DocumentReference, Map<String, Any>> {
        return Pair(
            getFirestore()
                .collection(collectionId)
                .document(entityAsMap[entityIdField] as String
                ),
            entityAsMap
        )
    }

    fun <A, B> List<A>.mapList(f:(A) -> B): List<B> = mapListOperator(this, listOf(), f)

    private tailrec fun <A, B> mapListOperator(items: List<A>, resultList: List<B>, f:(A) -> B): List<B> {
        return when (items.isEmpty()) {
            true -> resultList
            false -> {
                val firstItem = items.first()
                val result = f(firstItem)
                mapListOperator(items.drop(1), resultList.plus(result), f)
            }
        }
    }
}


