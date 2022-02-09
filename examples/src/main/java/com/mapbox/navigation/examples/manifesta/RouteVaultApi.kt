package com.mapbox.navigation.examples.manifesta

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteEntity
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteRecord
import com.mapbox.navigation.examples.manifesta.support.RouteVaultMappers.ROUTE_VAULT_FIELD_ID
import com.mapbox.navigation.examples.manifesta.support.RouteVaultMappers.mapToStoredRouteEntity
import com.mapbox.navigation.examples.manifesta.support.RouteVaultMappers.mapToStoredRouteRecord
import com.mapbox.navigation.examples.manifesta.support.RouteVaultMappers.toMap
import com.mapbox.navigation.examples.manifesta.support.RouteVaultSupport.deleteEntity
import com.mapbox.navigation.examples.manifesta.support.RouteVaultSupport.getCollectionRef
import com.mapbox.navigation.examples.manifesta.support.RouteVaultSupport.mapList
import com.mapbox.navigation.examples.manifesta.support.RouteVaultSupport.queryCloudStore
import com.mapbox.navigation.examples.manifesta.support.RouteVaultSupport.storeEntity

class RouteVaultApi(private val organizationId: String) {

    companion object {
        const val ROUTE_VAULT_COLLECTION = "routeVault"
        const val ROUTES_COLLECTION = "routes"
    }


    suspend fun storeRoute(routeEntity: StoredRouteEntity): Expected<Throwable, StoredRouteEntity> {
        return storeEntity(
            routeEntity.toMap(),
            routeEntity.id,
            ROUTE_VAULT_COLLECTION,
            organizationId
        ).mapValue { routeEntity }
    }

    suspend fun getRoutes(): Expected<Throwable, List<StoredRouteRecord>> {
        val query = getRoutesCollection()
        return queryCloudStore(query).fold(
            {
                ExpectedFactory.createError(it)
            },{
                ExpectedFactory.createValue(it.mapList(::mapToStoredRouteRecord))
            }
        )
    }

    suspend fun getRoute(routeRecordId: String): Expected<Throwable, StoredRouteEntity> {
        val query = getRoutesCollection().whereEqualTo(ROUTE_VAULT_FIELD_ID, routeRecordId)
        return queryCloudStore(query)
            .mapValue { it.mapList(::mapToStoredRouteEntity) }
            .fold({
                ExpectedFactory.createError(it)
            },{
                when (it.isEmpty()) {
                    true -> ExpectedFactory.createError(NoSuchElementException())
                    false -> ExpectedFactory.createValue(it.first())
                }
            })
    }

    suspend fun deleteRoute(routeRecordId: String): Expected<Throwable, String> =
        deleteEntity(routeRecordId, ROUTE_VAULT_COLLECTION, organizationId)

    private fun getRoutesCollection() =
        getCollectionRef(ROUTE_VAULT_COLLECTION)
            .document(organizationId)
            .collection(ROUTES_COLLECTION)

}
