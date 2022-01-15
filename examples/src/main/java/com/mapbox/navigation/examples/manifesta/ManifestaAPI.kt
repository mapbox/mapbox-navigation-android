package com.mapbox.navigation.examples.manifesta

import com.google.firebase.firestore.DocumentReference
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.examples.manifesta.model.domain.LocationCollection
import com.mapbox.navigation.examples.manifesta.model.entity.LocationCollectionEntity
import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaLocation
import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaUser
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.MANIFESTA_COLLECTION_LOCATIONS
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.MANIFESTA_COLLECTION_LOCATION_COLLECTIONS
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.MANIFESTA_COLLECTION_USERS

import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.getCollectionRef
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.getDocumentFromCloudStore
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.getDocumentReference
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.getFirestore
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.mapList
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.queryCloudStore
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.toEntity
import com.mapbox.navigation.examples.manifesta.support.ManifestaSupport.userBasedQueryFactory
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers.locationCollectionToMap
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers.mapToLocationCollection
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers.mapToPlace
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers.mapToUser
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers.placeToMap
import com.mapbox.navigation.examples.manifesta.support.ManifestaTransformers.userToMap

import com.mapbox.navigation.utils.internal.parallelMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface ManifestaAPI {

    suspend fun storeLocationCollection(userId: String, locationCollection: LocationCollection): Expected<Throwable, Unit> {
        val docReferences = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
        val locationEntityDocs = locationCollection.locations
            .map(::placeToMap)
            .map {
                getDocumentReference(MANIFESTA_COLLECTION_LOCATIONS, it, ManifestaTransformers.MANIFESTA_FIELD_ID)
            }
        val locationCollectionEntityDoc = locationCollection.toEntity().run {
            val entityAsMap = locationCollectionToMap(this)
            getDocumentReference(
                MANIFESTA_COLLECTION_LOCATION_COLLECTIONS,
                entityAsMap,
                ManifestaTransformers.MANIFESTA_FIELD_ID
            )
        }
        // getUser(userId).value?.let {
        //     it.copy(locationCollections = it.locationCollections.plus(locationCollection.id)).userToMap().run {
        //         getDocumentReference(MANIFESTA_COLLECTION_USERS, this, ManifestaTransformers.MANIFESTA_FIELD_ID)
        //     }.apply {
        //         docReferences.add(this)
        //     }
        // }

        return storeEntitiesBatch(docReferences.plus(locationEntityDocs).plus(locationCollectionEntityDoc))
    }

    suspend fun getLocationCollectionsShallow(): Expected<Throwable, List<LocationCollectionEntity>> {
        val collectionQuery = getCollectionRef(MANIFESTA_COLLECTION_LOCATION_COLLECTIONS)
        return queryCloudStore(collectionQuery).mapValue {
            it.mapList(::mapToLocationCollection)
        }
    }

    suspend fun getLocations(locationIds: List<String>): List<ManifestaLocation> {
        return locationIds.map {
            getCollectionRef(MANIFESTA_COLLECTION_LOCATIONS).document(it)
        }.map {
            getDocumentFromCloudStore(it)
        }.map {
            it.getValueOrElse { mapOf() }
        }.filter {
            it.isNotEmpty()
        }.map(::mapToPlace)
    }

//     suspend fun getAllLocationCollections(): Expected<Throwable, List<LocationCollectionEntity>> = coroutineScope {
//         val collectionQuery = getCollectionRef(MANIFESTA_COLLECTION_LOCATION_COLLECTIONS)
//
//         val expected = queryCloudStore(collectionQuery)
//         if (expected.isError) {
//             expected
//         } else {
//             val collectionEntity = expected.mapValue {
//                 collectionMapper(it, ::mapToLocationCollection, listOf())
//             }.fold(
//                 { listOf() },{ it }
//             )
//
//
//             val deferredLocations = this.async {
//                 collectionEntity.map { entity ->
//                     entity.locations
//                 }.map { locationIdList ->
//                     documentIdsToDocumentReferences(locationIdList, MANIFESTA_COLLECTION_LOCATIONS, listOf())
//                 }.map { documentReferences ->
//                     getDocumentsFromCloudStore(documentReferences, listOf())
//                 }.map { expectedItems ->
//                     valuesFromExpectedItems(expectedItems, listOf())
//                 }
//             }
//             deferredLocations.await()
//
//
//         }
//
// //////
//         queryCloudStore(collectionQuery).fold(
//             {
//                 ExpectedFactory.createError(it)
//             },
//             {
//                 val collectionEntity = collectionMapper(it, ::mapToLocationCollection, listOf())
//
//
//                 collectionEntity.map { entity ->
//                     entity.locations
//                 }.map { locationIdList ->
//                     documentIdsToDocumentReferences(locationIdList, MANIFESTA_COLLECTION_LOCATIONS, listOf())
//                 }.map { documentReferences ->
//                     this.async {
//                         getDocumentsFromCloudStore(documentReferences, listOf())
//                     }
//                 }
//
//
//
//
//                 ExpectedFactory.createValue(collectionMapper(it, ::mapToLocationCollection, listOf()))
//             }
//         )
//     }

    suspend fun getAllUsers(): Expected<Throwable, List<ManifestaUser>> {
        val query = getCollectionRef(MANIFESTA_COLLECTION_USERS)
        return queryCloudStore(query).fold({
            ExpectedFactory.createError(it)
        },{
            ExpectedFactory.createValue(it.mapList(::mapToUser))
        })
    }

    suspend fun storeUser(user: ManifestaUser): Expected<Throwable, ManifestaUser> {
        return storeEntity(user.userToMap(), user.id, MANIFESTA_COLLECTION_USERS).map(
            { it },{ user }
        )
    }

    // shortcut to get a user or create one if a user with the specified isn't found.
    private suspend fun getUser(userId: String): Expected<Throwable, ManifestaUser> = coroutineScope {
        val query = userBasedQueryFactory(MANIFESTA_COLLECTION_USERS, userId)
        queryCloudStore(query).map(
            { ExpectedFactory.createError<Throwable, ManifestaUser>(it) },
            { it.mapList(::mapToUser) }
        ).fold( {
            this.async { it }
        },{
            when(it.isNotEmpty()) {
                true -> this.async {
                    ExpectedFactory.createValue<Throwable, ManifestaUser>(it.first())
                }
                false -> this.async {
                    storeUser(ManifestaUser(userId, "unknown", listOf()))
                }
            }
        }).await()
    }

    private suspend fun storeEntitiesBatch(docs: List<Pair<DocumentReference, Map<String, Any>>>): Expected<Throwable, Unit> {
        return suspendCoroutine { continuation ->
            getFirestore().batch().let { batch ->
                docs.forEach { batch.set(it.first, it.second) }
                batch.commit()
                    .addOnFailureListener { continuation.resume(ExpectedFactory.createError(it)) }
                    .addOnSuccessListener { continuation.resume(ExpectedFactory.createValue(Unit)) }
            }
        }
    }

    private suspend fun storeEntity(entityAsMap :Map<String, Any>, entityId: String, collectionName: String): Expected<Throwable, Unit> {
        return suspendCoroutine { continuation ->
            getCollectionRef(collectionName)
                .document(entityId)
                .set(entityAsMap)
                .addOnFailureListener { continuation.resume(ExpectedFactory.createError(it)) }
                .addOnSuccessListener { continuation.resume(ExpectedFactory.createValue(Unit)) }
        }
    }
}
