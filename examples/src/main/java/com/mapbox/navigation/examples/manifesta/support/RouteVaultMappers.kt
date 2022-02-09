package com.mapbox.navigation.examples.manifesta.support

import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteEntity
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteRecord

object RouteVaultMappers {


    const val ROUTE_VAULT_FIELD_ID = "id"
    private const val ROUTE_VALUE_FIELD_ALIAS = "alias"
    private const val ROUTE_VALUE_FIELD_ROUTE_JSON = "routeAsJson"

    fun StoredRouteEntity.toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().also { map ->
            map[ROUTE_VAULT_FIELD_ID] = this.id
            map[ROUTE_VALUE_FIELD_ALIAS] = this.alias
            map[ROUTE_VALUE_FIELD_ROUTE_JSON] = this.routeAsJson
        }
    }

    fun StoredRouteRecord.toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().also { map ->
            map[ROUTE_VAULT_FIELD_ID] = this.id
            map[ROUTE_VALUE_FIELD_ALIAS] = this.alias
        }
    }

    fun mapToStoredRouteRecord(itemMap: Map<String, Any>): StoredRouteRecord {
        return StoredRouteRecord(
            getMapFieldValueAsString(itemMap, ROUTE_VAULT_FIELD_ID),
            getMapFieldValueAsString(itemMap, ROUTE_VALUE_FIELD_ALIAS)
        )
    }

    fun mapToStoredRouteEntity(itemMap: Map<String, Any>): StoredRouteEntity {
        return StoredRouteEntity(
            getMapFieldValueAsString(itemMap, ROUTE_VAULT_FIELD_ID),
            getMapFieldValueAsString(itemMap, ROUTE_VALUE_FIELD_ALIAS),
            getMapFieldValueAsString(itemMap, ROUTE_VALUE_FIELD_ROUTE_JSON)
        )
    }

    private fun getMapFieldValueAsString(itemMap: Map<String, Any>, fieldName: String): String = when {
        !itemMap.containsKey(fieldName) -> ""
        itemMap[fieldName] == null -> ""
        else -> itemMap[fieldName] as String
    }
}
