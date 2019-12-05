package com.mapbox.navigation.base.logger

interface Logger {
    /**
     * Send a verbose log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun v(msg: String, tag: String? = null, tr: Throwable? = null)

    /**
     * Send a debug log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun d(msg: String, tag: String? = null, tr: Throwable? = null)

    /**
     * Send an info log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun i(msg: String, tag: String? = null, tr: Throwable? = null)

    /**
     * Send a warning log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun w(msg: String, tag: String? = null, tr: Throwable? = null)

    /**
     * Send an error log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    fun e(msg: String, tag: String? = null, tr: Throwable? = null)
}
