package com.mapbox.navigation.examples.manifesta.support

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.examples.manifesta.RouteVaultApi
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object RouteVaultSupport {

    private fun getFirestore(): FirebaseFirestore {
        val fsSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        return FirebaseFirestore.getInstance().also {
            it.firestoreSettings = fsSettings
        }
    }

    fun getCollectionRef(collectionName: String): CollectionReference =
        getFirestore().collection(collectionName)

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

    suspend fun deleteEntity(entityId: String, collectionName: String, organizationId: String): Expected<Throwable, String> = suspendCancellableCoroutine { continuation ->
        getCollectionRef(collectionName)
            .document(organizationId)
            .collection(RouteVaultApi.ROUTES_COLLECTION)
            .document(entityId)
            .delete()
            .addOnFailureListener { throwable ->  continuation.resume(ExpectedFactory.createError(throwable)) }
            .addOnSuccessListener { continuation.resume(ExpectedFactory.createValue(entityId)) }
    }

    suspend fun storeEntity(entityAsMap :Map<String, Any>, entityId: String, collectionName: String, organizationId: String): Expected<Throwable, Unit> {
        return suspendCoroutine { continuation ->
            getCollectionRef(collectionName)
                .document(organizationId)
                .collection(RouteVaultApi.ROUTES_COLLECTION)
                .document(entityId)
                .set(entityAsMap)
                .addOnFailureListener { continuation.resume(ExpectedFactory.createError(it)) }
                .addOnSuccessListener { continuation.resume(ExpectedFactory.createValue(Unit)) }
        }
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

    fun StoredRouteEntity.toDirectionsRoute(): Expected<Throwable, DirectionsRoute> {
        return try {
            ExpectedFactory.createValue(DirectionsRoute.fromJson(this.routeAsJson))
        } catch (ex: Exception) {
            ExpectedFactory.createError(ex)
        }
    }
}
