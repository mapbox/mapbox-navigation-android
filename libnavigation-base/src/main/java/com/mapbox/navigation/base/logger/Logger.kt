package com.mapbox.navigation.base.logger

import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag

interface Logger {
    /**
     * Send a verbose log message and log the exception.
     *
     * @param msg is [Message] you would like logged.
     * @param tag is [Tag] used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun v(msg: Message, tag: Tag? = null, tr: Throwable? = null)

    /**
     * Send a debug log message and log the exception.
     *
     * @param msg is [Message] you would like logged.
     * @param tag is [Tag] used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun d(msg: Message, tag: Tag? = null, tr: Throwable? = null)

    /**
     * Send an info log message and log the exception.
     *
     * @param msg is [Message] you would like logged.
     * @param tag is [Tag] used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun i(msg: Message, tag: Tag? = null, tr: Throwable? = null)

    /**
     * Send a warning log message and log the exception.
     *
     * @param msg is [Message] you would like logged.
     * @param tag is [Tag] used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun w(msg: Message, tag: Tag? = null, tr: Throwable? = null)

    /**
     * Send an error log message and log the exception.
     *
     * @param msg is [Message] you would like logged.
     * @param tag is [Tag] used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun e(msg: Message, tag: Tag? = null, tr: Throwable? = null)
}
