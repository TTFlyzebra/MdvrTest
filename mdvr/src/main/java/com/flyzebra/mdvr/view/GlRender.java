package com.flyzebra.mdvr.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.flyzebra.mdvr.R;
import com.flyzebra.utils.GlShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is GlRender
 */
public class GlRender implements GLSurfaceView.Renderer {
    private final Context context;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer textureBuffer;
    //顶点坐标
    static float[] vertexData = {   // in counterclockwise order:
            -1f, -1f, 0.0f, // bottom left
            +1f, -1f, 0.0f, // bottom right
            -1f, +1f, 0.0f, // top left
            +1f, +1f, 0.0f,  // top right
    };
    //纹理坐标
    static float[] textureData = {   // in counterclockwise order:
            0.0f, 1.0f, 0.0f, // bottom left
            1.0f, 1.0f, 0.0f, // bottom right
            0.0f, 0.0f, 0.0f, // top left
            1.0f, 0.0f, 0.0f,  // top right
    };

    protected float[] vMatrixData = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    private int glprogram;
    protected int vPosition;
    protected int fPosition;
    protected int vMatrix;
    private int sampler_y;
    private int sampler_uv;
    private final int[] textureIds = new int[2];
    private int width = 0;
    private int height = 0;
    private ByteBuffer y;
    private ByteBuffer uv;

    private final Object objectLock = new Object();

    public GlRender(Context context) {
        this.context = context;

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        y = ByteBuffer.wrap(new byte[this.width * this.height]);
        uv = ByteBuffer.wrap(new byte[this.width * this.height / 2]);
    }

    public void pushNv12data(byte[] nv12, int size, int width, int height) {
        if (this.width != width || this.height != height) {
            setSize(width, height);
        }
        synchronized (objectLock) {
            y.put(nv12, 0, width * height);
            uv.put(nv12, width * height, width * height / 2);
            y.flip();
            uv.flip();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        String vertexShader = GlShaderUtil.readRawTextFile(context, R.raw.vertex_nv12);
        String fragmentShader = GlShaderUtil.readRawTextFile(context, R.raw.fragment_nv12);
        glprogram = GlShaderUtil.createProgram(vertexShader, fragmentShader);
        vPosition = GLES20.glGetAttribLocation(glprogram, "vPosition");
        fPosition = GLES20.glGetAttribLocation(glprogram, "fPosition");
        vMatrix = GLES20.glGetUniformLocation(glprogram, "vMatrix");
        sampler_y = GLES20.glGetUniformLocation(glprogram, "sampler_y");
        sampler_uv = GLES20.glGetUniformLocation(glprogram, "sampler_uv");
        GLES20.glGenTextures(textureIds.length, textureIds, 0);
        for (int textureId : textureIds) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        synchronized (objectLock) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);//
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, width / 2, height / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uv);
        }

        GLES20.glUseProgram(glprogram);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, vMatrixData, 0);
        GLES20.glUniform1i(sampler_y, 0);
        GLES20.glUniform1i(sampler_uv, 1);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(fPosition, 3, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(vPosition);
        GLES20.glDisableVertexAttribArray(fPosition);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
