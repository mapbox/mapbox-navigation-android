package com.mapbox.navigation.mapgpt.core.userinput

import com.mapbox.navigation.mapgpt.core.Middleware

/**
 * Use to provide a custom implementation of [UserInputOwner].
 */
interface UserInputOwnerMiddleware : UserInputOwner, Middleware<UserInputMiddlewareContext> {
    val provider: UserInputProvider
}
