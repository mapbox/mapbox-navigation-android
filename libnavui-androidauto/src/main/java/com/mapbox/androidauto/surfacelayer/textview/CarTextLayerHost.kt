package com.mapbox.androidauto.surfacelayer.textview

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import com.mapbox.androidauto.logAndroidAutoFailure
import com.mapbox.maps.CustomLayerHost
import com.mapbox.maps.CustomLayerRenderParameters
import java.util.concurrent.ConcurrentLinkedQueue

class CarTextLayerHost : CustomLayerHost {
    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var projectionMatrixHandle = 0
    private var modelMatrixHandle = 0
    private var textureHandle = 0
    private var texCoordHandle = 0
    private var vertexShader = 0
    private var fragmentShader = 0
    private var textures = IntArray(1)

    val mapScene = CarScene2d()

    private val bitmapQueue = ConcurrentLinkedQueue<Bitmap>()

    /**
     * Thread-safe function to refresh the texture bitmap.
     * One of the upcoming render calls will load the bitmap to the GPU.
     *
     * @param bitmap used to render text, `null` will clear the text
     */
    fun offerBitmap(bitmap: Bitmap?) {
        mapScene.model.updateModelMatrix(bitmap)
        bitmapQueue.offer(bitmap ?: emptyBitmap)
    }

    override fun initialize() {
        val maxAttrib = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, maxAttrib, 0)

        // load and compile shaders
        vertexShader = loadVertexShader()
        fragmentShader = loadFragmentShader()

        // Setup the OpenGL ES Program
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // Create handles for the fragment and vertex shaders
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uFillColor")
        modelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix")

        // Create handles for the texture
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glGenTextures(1, textures, 0)
    }

    override fun render(parameters: CustomLayerRenderParameters) {
        if (program == 0) return

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(program)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, positionHandle)

        // Enable a handle to the vertices
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Prepare the coordinate data
        GLES20.glVertexAttribPointer(
            positionHandle, mapScene.model.dimensions,
            GLES20.GL_FLOAT, false,
            mapScene.model.stride, mapScene.model.vertices
        )

        // Set color for drawing the background
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        // Apply the projection transformation
        GLES20.glUniformMatrix4fv(
            projectionMatrixHandle, 1, false, mapScene.mvpMatrix, 0
        )

        // Apply the projection transformation
        GLES20.glUniformMatrix4fv(
            modelMatrixHandle, 1, false, mapScene.model.modelMatrix, 0
        )

        // Activate the texture and use the latest bitmap
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glUniform1i(textureHandle, 0)
        updateGlTextImage()

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle, 2,
            GLES20.GL_FLOAT, false,
            0, mapScene.model.textureCords
        )

        // Draw the background
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mapScene.model.length)
    }

    private fun updateGlTextImage() {
        bitmapQueue.poll()?.let {
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, it, 0)
            if (it != emptyBitmap) {
                it.recycle()
            }
        }
    }

    override fun contextLost() {
        logAndroidAutoFailure("contextLost")
        program = 0
    }

    override fun deinitialize() {
        if (program != 0) {
            // Disable vertex array
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDetachShader(program, vertexShader)
            GLES20.glDetachShader(program, fragmentShader)
            GLES20.glDeleteTextures(1, textures, 0)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    companion object {
        // Set color with red, green, blue and alpha (opacity) values
        var color = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)

        private val VERTEX_SHADER_CODE = """
      uniform mat4 uModelMatrix;
      uniform mat4 uProjectionMatrix;
      
      attribute vec2 aPosition;
      attribute vec2 aTexCoord;
      
      varying vec2 vTexCoord;
      
      void main() {
        mat4 mvp_matrix = uProjectionMatrix * uModelMatrix;
        gl_Position = mvp_matrix * vec4(aPosition, 0, 1.0);
        vTexCoord = aTexCoord;
      }
        """.trimIndent()

        private val FRAGMENT_SHADER_CODE = """
      precision mediump float;
      
      varying vec2 vTexCoord;
      
      uniform vec4 uFillColor;
      uniform sampler2D uTexture;
      
      void main() {
        vec4 texture_color = texture2D(uTexture, vTexCoord);
        vec4 background = uFillColor * (1.0-texture_color.a);
        gl_FragColor = background + texture_color;
      }
        """.trimIndent()

        private val emptyBitmap: Bitmap by lazy {
            Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).also {
                it.eraseColor(Color.TRANSPARENT)
            }
        }

        private fun loadVertexShader(): Int =
            loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)

        private fun loadFragmentShader(): Int =
            loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        private fun loadShader(type: Int, shaderCode: String): Int {
            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            return GLES20.glCreateShader(type).also { shader ->
                // add the source code to the shader and compile it
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
        }
    }
}
