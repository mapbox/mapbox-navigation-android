package com.mapbox.navigation.base.logger.model

/**
 * Wrapper class for loggers tag.
 *
 * @param tag used to identify the source of a log message.  It usually identifies
 * the class or activity where the log call occurs.
 */
data class Tag(val tag: String)
