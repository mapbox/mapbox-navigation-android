/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core.accounts

import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator

internal object TokenGeneratorWrapper {
    fun getSKUTokenIfValid(skuIdentifier: SKUIdentifier): String? {
        return TokenGenerator.getSKUTokenIfValid(skuIdentifier)
    }
}
