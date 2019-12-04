@file:JvmName("LogPriority")

package com.mapbox.navigation.logger

import android.util.Log

/**
 * Priority constant for the println method; use Logger.v
 *
 *
 * This log level will print all logs.
 *
 */
const val VERBOSE = Log.VERBOSE

/**
 * Priority constant for the println method; use Logger.d.
 *
 *
 * This log level will print all logs except verbose.
 *
 */
const val DEBUG = Log.DEBUG

/**
 * Priority constant for the println method; use Logger.i.
 *
 *
 * This log level will print all logs except verbose and debug.
 *
 */
const val INFO = Log.INFO

/**
 * Priority constant for the println method; use Logger.w.
 *
 *
 * This log level will print only warn and error logs.
 *
 */
const val WARN = Log.WARN

/**
 * Priority constant for the println method; use Logger.e.
 *
 *
 * This log level will print only error logs.
 *
 */
const val ERROR = Log.ERROR

/**
 * Priority constant for the println method.
 *
 *
 * This log level won't print any logs.
 *
 */
const val NONE = 99
