/*
 * This class uses portion of the OkHttp library with code reformatted to match project code formatting standards.
 * - String.canonicalize
 * - Char.parseHexDigit
 * - String.isPercentEncoded
 * - Buffer.writeCanonicalized
 *
 * source: https://github.com/square/okhttp/blob/b4904c65bdf38fd20ca0aacb23b474c50e5c1d06/okhttp/src/jvmMain/kotlin/okhttp3/HttpUrl.kt#L1770
 */
package com.mapbox.navigation.voice.api

/* ktlint-disable */
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

internal object UrlUtils {

    private val HEX_DIGITS =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    private const val PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#"
    private const val FORM_ENCODE_SET = " !\"#$&'()+,/:;<=>?@[\\]^`{|}~"

    fun encodePathSegment(pathSegment: String): String =
        pathSegment.canonicalize(encodeSet = PATH_SEGMENT_ENCODE_SET)

    /**
     * Returns a substring of `input` on the range `[pos..limit)` with the following
     * transformations:
     *
     *  * Tabs, newlines, form feeds and carriage returns are skipped.
     *
     *  * In queries, ' ' is encoded to '+' and '+' is encoded to "%2B".
     *
     *  * Characters in `encodeSet` are percent-encoded.
     *
     *  * Control characters and non-ASCII characters are percent-encoded.
     *
     *  * All other characters are copied without transformation.
     *
     * @param alreadyEncoded true to leave '%' as-is; false to convert it to '%25'.
     * @param strict true to encode '%' if it is not the prefix of a valid percent encoding.
     * @param plusIsSpace true to encode '+' as "%2B" if it is not already encoded.
     * @param unicodeAllowed true to leave non-ASCII codepoint unencoded.
     * @param charset which charset to use, null equals UTF-8.
     */
    internal fun String.canonicalize(
        pos: Int = 0,
        limit: Int = length,
        encodeSet: String,
        alreadyEncoded: Boolean = false,
        strict: Boolean = false,
        plusIsSpace: Boolean = false,
        unicodeAllowed: Boolean = false,
        charset: Charset? = null
    ): String {
        var codePoint: Int
        var i = pos
        while (i < limit) {
            codePoint = codePointAt(i)
            if (codePoint < 0x20 ||
                codePoint == 0x7f ||
                codePoint >= 0x80 && !unicodeAllowed ||
                codePoint.toChar() in encodeSet ||
                codePoint == '%'.code &&
                (!alreadyEncoded || strict && !isPercentEncoded(i, limit)) ||
                codePoint == '+'.code && plusIsSpace
            ) {
                // Slow path: the character at i requires encoding!
                val out = Buffer()
                out.writeUtf8(this, pos, i)
                out.writeCanonicalized(
                    input = this,
                    pos = i,
                    limit = limit,
                    encodeSet = encodeSet,
                    alreadyEncoded = alreadyEncoded,
                    strict = strict,
                    plusIsSpace = plusIsSpace,
                    unicodeAllowed = unicodeAllowed,
                    charset = charset
                )
                return out.readUtf8()
            }
            i += Character.charCount(codePoint)
        }

        // Fast path: no characters in [pos..limit) required encoding.
        return substring(pos, limit)
    }

    private fun Char.parseHexDigit(): Int = when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> -1
    }

    private fun String.isPercentEncoded(pos: Int, limit: Int): Boolean {
        return pos + 2 < limit &&
            this[pos] == '%' &&
            this[pos + 1].parseHexDigit() != -1 &&
            this[pos + 2].parseHexDigit() != -1
    }

    private fun Buffer.writeCanonicalized(
        input: String,
        pos: Int,
        limit: Int,
        encodeSet: String,
        alreadyEncoded: Boolean,
        strict: Boolean,
        plusIsSpace: Boolean,
        unicodeAllowed: Boolean,
        charset: Charset?
    ) {
        var encodedCharBuffer: Buffer? = null // Lazily allocated.
        var codePoint: Int
        var i = pos
        while (i < limit) {
            codePoint = input.codePointAt(i)
            if (alreadyEncoded && (
                codePoint == '\t'.code || codePoint == '\n'.code ||
                    codePoint == '\u000c'.code || codePoint == '\r'.code
                )
            ) {
                // Skip this character.
            } else if (codePoint == ' '.code && encodeSet === FORM_ENCODE_SET) {
                // Encode ' ' as '+'.
                writeUtf8("+")
            } else if (codePoint == '+'.code && plusIsSpace) {
                // Encode '+' as '%2B' since we permit ' ' to be encoded as either '+' or '%20'.
                writeUtf8(if (alreadyEncoded) "+" else "%2B")
            } else if (codePoint < 0x20 ||
                codePoint == 0x7f ||
                codePoint >= 0x80 && !unicodeAllowed ||
                codePoint.toChar() in encodeSet ||
                codePoint == '%'.code &&
                (!alreadyEncoded || strict && !input.isPercentEncoded(i, limit))
            ) {
                // Percent encode this character.
                if (encodedCharBuffer == null) {
                    encodedCharBuffer = Buffer()
                }

                if (charset == null || charset == UTF_8) {
                    encodedCharBuffer.writeUtf8CodePoint(codePoint)
                } else {
                    encodedCharBuffer.writeString(input, i, i + Character.charCount(codePoint), charset)
                }

                while (!encodedCharBuffer.exhausted()) {
                    val b = encodedCharBuffer.readByte().toInt() and 0xff
                    writeByte('%'.code)
                    writeByte(HEX_DIGITS[b shr 4 and 0xf].code)
                    writeByte(HEX_DIGITS[b and 0xf].code)
                }
            } else {
                // This character doesn't need encoding. Just copy it over.
                writeUtf8CodePoint(codePoint)
            }
            i += Character.charCount(codePoint)
        }
    }
}
/* ktlint-enable */
