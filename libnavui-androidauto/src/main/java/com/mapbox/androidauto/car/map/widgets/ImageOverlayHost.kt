package com.mapbox.androidauto.car.map.widgets

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import com.mapbox.common.Logger
import com.mapbox.maps.BuildConfig
import com.mapbox.maps.CustomLayerHost
import com.mapbox.maps.CustomLayerRenderParameters
import com.mapbox.navigation.utils.internal.logE
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

data class Margin(
    val marginLeft: Float = 0f,
    val marginTop: Float = 0f,
    val marginRight: Float = 0f,
    val marginBottom: Float = 0f,
)

open class ImageOverlayHost(
    private var bitmap: Bitmap,
    private val position: WidgetPosition = WidgetPosition.BOTTOM_LEFT,
    private var margins: Margin = Margin(),
    var shouldRender: Boolean = true
) : CustomLayerHost {
    private var width = 0
    private var height = 0

    private var program = 0
    private var vertexPositionHandle = 0
    private var texturePositionHandle = 0
    private var textureHandle = 0
    private var screenMatrixHandle = 0
    private var rotationMatrixHandle = 0
    private var translateMatrixHandle = 0
    private val textures = intArrayOf(0)

    private var vertexShader = 0
    private var fragmentShader = 0

    private lateinit var screenMatrixData: FloatArray
    private lateinit var screenMatrixBuffer: FloatBuffer

    private val rotationMatrix = FloatArray(MATRIX_SIZE).apply {
        Matrix.setIdentityM(this, 0)
    }

    private val translateMatrix = FloatArray(MATRIX_SIZE)

    private lateinit var vertexPositionData: FloatArray
    private lateinit var vertexPositionBuffer: FloatBuffer

    init {
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.setIdentityM(translateMatrix, 0)
    }

    private val texturePositionData = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )

    private fun onSizeChanged(width: Int, height: Int, margins: Margin) {
        if (this.width == width && this.height == height && this.margins == margins) return
        this.width = width
        this.height = height
        this.margins = margins
        logE(
            TAG,
            "onSizeChanged-> bitmap size: ${bitmap.width}, ${bitmap.height}; screen size: $width, $height"
        )
        val heightOffset = when (position) {
            WidgetPosition.BOTTOM_LEFT -> height.toFloat() - bitmap.height.toFloat() / 2f - margins.marginBottom
            WidgetPosition.BOTTOM_RIGHT -> height.toFloat() - bitmap.height.toFloat() / 2f - margins.marginBottom
            WidgetPosition.TOP_LEFT -> margins.marginTop + bitmap.height.toFloat() / 2f
            WidgetPosition.TOP_RIGHT -> margins.marginTop + bitmap.height.toFloat() / 2f
        }
        val widthOffset = when (position) {
            WidgetPosition.TOP_RIGHT -> width.toFloat() - bitmap.width.toFloat() / 2f - margins.marginRight
            WidgetPosition.BOTTOM_RIGHT -> width.toFloat() - bitmap.width.toFloat() / 2f - margins.marginRight
            WidgetPosition.TOP_LEFT -> margins.marginLeft + bitmap.width.toFloat() / 2f
            WidgetPosition.BOTTOM_LEFT -> margins.marginLeft + bitmap.width.toFloat() / 2f
        }

        Matrix.setIdentityM(translateMatrix, 0)
        Matrix.translateM(
            translateMatrix,
            0,
            widthOffset,
            heightOffset,
            0f
        )

        // The screen matrix
        //
        // First of all, the only coordinate system that OpenGL understands
        // put the center of the screen at the 0,0 position. The maximum value of
        // the X axis is 1 (rightmost part of the screen) and the minimum is -1
        // (leftmost part of the screen). The same thing goes for the Y axis,
        // where 1 is the top of the screen and -1 the bottom.
        //
        // However, when you're doing a 2d application you often need to think in 'pixels'
        // (or something like that). If you have a 300x300 screen, you want to see the center
        // at 150,150 not 0,0!
        //
        // The solution to this 'problem' is to multiply a matrix with your position to
        // another matrix that will convert 'your' coordinates to the one OpenGL expects.
        // There's no magic in this, only a bit of math. Try to multiply the uScreen matrix
        // to the 150,150 position in a sheet of paper and look at the results.
        //
        // IMPORTANT: When trying to calculate the matrix on paper, you should treat the
        // uScreen ROWS as COLUMNS and vice versa. This happens because OpenGL expect the
        // matrix values ordered in a more efficient way, that unfortunately is different
        // from the mathematical notation :(
        screenMatrixData = floatArrayOf(
            2f / width.toFloat(), 0f, 0f, 0f,
            0f, 2f / -height.toFloat(), 0f, 0f,
            0f, 0f, 0f, 0f,
            -1f, 1f, 0f, 1f
        )

        // initialize vertex byte buffer for shape coordinates
        // (number of coordinate values * 4 bytes per float)
        screenMatrixBuffer = screenMatrixData.toFloatBuffer()

        vertexPositionData = floatArrayOf(
            -bitmap.width.toFloat() / 2f, -bitmap.height.toFloat() / 2f,
            -bitmap.width.toFloat() / 2f, bitmap.height.toFloat() / 2f,
            bitmap.width.toFloat() / 2f, -bitmap.height.toFloat() / 2f,
            bitmap.width.toFloat() / 2f, bitmap.height.toFloat() / 2f,
        )

        // initialize vertex byte buffer for shape coordinates
        // (number of coordinate values * 4 bytes per float)
        vertexPositionBuffer = vertexPositionData.toFloatBuffer()
    }

    override fun initialize() {
        // load and compile shaders
        vertexShader = loadShader(
            GLES20.GL_VERTEX_SHADER,
            VERTEX_SHADER_CODE
        ).also { checkCompileStatus(it) }

        fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            FRAGMENT_SHADER_CODE
        ).also { checkCompileStatus(it) }

        // create empty OpenGL ES Program
        program = GLES20.glCreateProgram().also {
            checkError("glCreateProgram")
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader).also { checkError("glAttachShader") }

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader).also { checkError("glAttachShader") }

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it).also {
                checkError("glLinkProgram")
            }
        }

        // get handle to screen matrix(fragment shader's uScreen member)
        screenMatrixHandle =
            GLES20.glGetUniformLocation(program, "uScreen")
                .also { checkError("glGetAttribLocation") }

        // get handle to rotation matrix(fragment shader's uRotation member)
        rotationMatrixHandle =
            GLES20.glGetUniformLocation(program, "uRotation")
                .also { checkError("glGetAttribLocation") }

        translateMatrixHandle =
            GLES20.glGetUniformLocation(program, "uTranslate")
                .also { checkError("glGetAttribLocation") }

        // get handle to vertex shader's aPosition member
        vertexPositionHandle =
            GLES20.glGetAttribLocation(program, "aPosition")
                .also { checkError("glGetAttribLocation") }

        // get handle to texture coordinate(vertex shader's aCoordinate member)
        texturePositionHandle =
            GLES20.glGetAttribLocation(program, "aCoordinate")
                .also { checkError("glGetAttribLocation") }

        // get handle to fragment shader's vColor member
        textureHandle =
            GLES20.glGetUniformLocation(program, "vTexture")
                .also { checkError("glGetAttribLocation") }
    }

    override fun render(parameters: CustomLayerRenderParameters) {
        if (!shouldRender) return
        onSizeChanged(parameters.width.toInt(), parameters.height.toInt(), margins)
        if (program != 0) {
            // Add program to OpenGL ES environment
            GLES20.glUseProgram(program).also {
                checkError("glUseProgram")
            }
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texturePositionHandle).also {
                checkError("glBindBuffer")
            }

            GLES20.glUniformMatrix4fv(
                screenMatrixHandle,
                screenMatrixBuffer.limit() / screenMatrixData.size,
                false,
                screenMatrixBuffer
            )

            val rotationBuffer = rotationMatrix.toFloatBuffer()
            GLES20.glUniformMatrix4fv(
                rotationMatrixHandle,
                rotationBuffer.limit() / rotationMatrix.size,
                false,
                rotationBuffer
            )

            val translateBuffer = translateMatrix.toFloatBuffer()
            GLES20.glUniformMatrix4fv(
                translateMatrixHandle,
                rotationBuffer.limit() / translateMatrix.size,
                false,
                translateBuffer
            )

            createTexture()

            // Activate the first texture (GL_TEXTURE0) and bind it to our handle
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

            // Textures
            GLES20.glUniform1i(textureHandle, 0)

            // Enable a handle to the vertices
            GLES20.glEnableVertexAttribArray(vertexPositionHandle)
                .also { checkError("glEnableVertexAttribArray") }

            // Prepare the vertex coordinate data
            GLES20.glVertexAttribPointer(
                vertexPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, vertexPositionBuffer
            ).also { checkError("glVertexAttribPointer") }

            // Enable a handle to the tex position
            GLES20.glEnableVertexAttribArray(texturePositionHandle)
                .also { checkError("glEnableVertexAttribArray") }

            // Prepare the texture coordinate data
            GLES20.glVertexAttribPointer(
                texturePositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, texturePositionData.toFloatBuffer()
            ).also { checkError("glVertexAttribPointer") }

            // Draw the background
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)
                .also { checkError("glDrawArrays") }
        }
    }

    override fun contextLost() {
        Logger.w(TAG, "contextLost")
        program = 0
    }

    override fun deinitialize() {
        if (program != 0) {
            // Disable vertex array
            GLES20.glDisableVertexAttribArray(vertexPositionHandle)
            GLES20.glDisableVertexAttribArray(texturePositionHandle)
            GLES20.glDisableVertexAttribArray(textureHandle)
            GLES20.glDisableVertexAttribArray(screenMatrixHandle)
            GLES20.glDisableVertexAttribArray(rotationMatrixHandle)
            GLES20.glDisableVertexAttribArray(translateMatrixHandle)
            GLES20.glDetachShader(program, vertexShader)
            GLES20.glDetachShader(program, fragmentShader)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            GLES20.glDeleteTextures(textures.size, textures, 0)
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    private fun checkCompileStatus(shader: Int) {
        if (BuildConfig.DEBUG) {
            val isCompiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, isCompiled, 0)
            if (isCompiled[0] == GLES20.GL_FALSE) {
                val infoLog = GLES20.glGetShaderInfoLog(program)
                Logger.e(TAG, "checkCompileStatus error: $infoLog")
            }
        }
    }

    private fun createTexture() {
        if (!bitmap.isRecycled) {
            Logger.d(TAG, "createTexture")
            // generate texture
            GLES20.glGenTextures(1, textures, 0)
            // generate texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            // set the color filter is reduced pixel color closest to the coordinates of a pixel in
            // texture drawn as required
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            // set the amplification filter using texture coordinates to the nearest number of colors,
            // obtained by the weighted averaging algorithm requires pixel color drawn
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            // Set the circumferential direction S, texture coordinates taken to [1 / 2n, 1-1 / 2n].
            // Will never lead to integration and border
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            // Set the circumferential direction T, taken to texture coordinates [1 / 2n, 1-1 / 2n].
            // Will never lead to integration and border
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            // The parameters specified above, generates a 2D texture
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()
        }
    }

    fun updateBitmap(bitmap: Bitmap) {
        shouldRender = true
        this.bitmap = bitmap
        onSizeChanged(width, height, margins)
    }

    fun rotate(bearing: Float) {
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, bearing, 0f, 0f, 1f)
    }

    fun updateMargins(margins: Margin) {
        onSizeChanged(width, height, margins)
    }

    companion object {
        private const val TAG = "ImageOverlayHost"
        private const val MATRIX_SIZE = 16

        // number of coordinates per vertex in this array
        private const val COORDS_PER_VERTEX = 2
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT // 4 bytes per vertex
        private const val VERTEX_COUNT = 4 // 4 vertex in total

        private val VERTEX_SHADER_CODE = """
      uniform mat4 uScreen;
      uniform mat4 uTranslate;
      uniform mat4 uRotation;
      attribute vec2 aPosition;
      attribute vec2 aCoordinate;
      varying vec2 vTexCoord;
      void main() {
        vTexCoord = aCoordinate;
        gl_Position =  uScreen * uTranslate  * uRotation * vec4(aPosition, 0.0, 1.0);
      }
        """.trimIndent()

        private val FRAGMENT_SHADER_CODE = """
      precision mediump float;
      uniform sampler2D vTexture;
      varying vec2 vTexCoord;
      void main() {
        gl_FragColor = texture2D(vTexture, vTexCoord);
      }
        """.trimIndent()

        private fun loadShader(type: Int, shaderCode: String): Int {
            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            return GLES20.glCreateShader(type).also { shader ->

                // add the source code to the shader and compile it
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
        }

        private fun checkError(cmd: String? = null) {
            if (BuildConfig.DEBUG) {
                when (val error = GLES20.glGetError()) {
                    GLES20.GL_NO_ERROR -> {
                        Logger.d(TAG, "$cmd -> no error")
                    }
                    GLES20.GL_INVALID_ENUM -> Logger.e(TAG, "$cmd -> error in gl: GL_INVALID_ENUM")
                    GLES20.GL_INVALID_VALUE -> Logger.e(
                        TAG,
                        "$cmd -> error in gl: GL_INVALID_VALUE"
                    )
                    GLES20.GL_INVALID_OPERATION -> Logger.e(
                        TAG,
                        "$cmd -> error in gl: GL_INVALID_OPERATION"
                    )
                    GLES20.GL_INVALID_FRAMEBUFFER_OPERATION -> Logger.e(
                        TAG,
                        "$cmd -> error in gl: GL_INVALID_FRAMEBUFFER_OPERATION"
                    )
                    GLES20.GL_OUT_OF_MEMORY -> Logger.e(
                        TAG,
                        "$cmd -> error in gl: GL_OUT_OF_MEMORY"
                    )
                    else -> Logger.e(TAG, "$cmd -> error in gl: $error")
                }
            }
        }
    }
}

private const val BYTES_PER_FLOAT = 4

private fun FloatArray.toFloatBuffer(): FloatBuffer {
    return ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT).run {
        // use the device hardware's native byte order
        order(ByteOrder.nativeOrder())

        // create a floating point buffer from the ByteBuffer
        asFloatBuffer().apply {
            // add the coordinates to the FloatBuffer
            put(this@toFloatBuffer)
            // set the buffer to read the first coordinate
            rewind()
        }
    }
}
