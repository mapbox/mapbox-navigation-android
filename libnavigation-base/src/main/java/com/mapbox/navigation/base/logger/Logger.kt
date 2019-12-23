package com.mapbox.navigation.base.logger

import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag

interface Logger {
    /**
     * Send a verbose log message and log the exception.
     *
     * @param tag is [Tag] used to identify the source of a log message. It usually identifies
     * the class or activity where the log call occurs.
     * @param msg is [Message] you would like logged.
     * @param tr An exception to log
     */
    fun v(tag: Tag? = null, msg: Message, tr: Throwable? = null)

    /**
     * Send a debug log message and log the exception.
     *
     * @param tag is [Tag] used to identify the source of a log message. It usually identifies
     * the class or activity where the log call occurs.
     * @param msg is [Message] you would like logged.
     * @param tr An exception to log
     */
    fun d(tag: Tag? = null, msg: Message, tr: Throwable? = null)

    /**
     * Send an info log message and log the exception.
     *
     * @param tag is [Tag] used to identify the source of a log message. It usually identifies
     * the class or activity where the log call occurs.
     * @param msg is [Message] you would like logged.
     * @param tr An exception to log
     */
    fun i(tag: Tag? = null, msg: Message, tr: Throwable? = null)

    /**
     * Send a warning log message and log the exception.
     *
     * @param tag is [Tag] used to identify the source of a log message. It usually identifies
     * the class or activity where the log call occurs.
     * @param msg is [Message] you would like logged.
     * @param tr An exception to log
     */
    fun w(tag: Tag? = null, msg: Message, tr: Throwable? = null)

    /**
     * Send an error log message and log the exception.
     *
     * @param tag is [Tag] used to identify the source of a log message. It usually identifies
     * the class or activity where the log call occurs.
     * @param msg is [Message] you would like logged.
     * @param tr An exception to log
     */
    fun e(tag: Tag? = null, msg: Message, tr: Throwable? = null)
}
