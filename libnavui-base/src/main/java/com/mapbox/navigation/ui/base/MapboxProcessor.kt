package com.mapbox.navigation.ui.base

interface MapboxProcessor<in A : MapboxAction, out R : MapboxResult> {

    fun process(action: A): R
}
