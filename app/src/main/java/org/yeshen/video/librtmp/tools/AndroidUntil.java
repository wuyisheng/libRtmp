package org.yeshen.video.librtmp.tools;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;

import org.yeshen.video.librtmp.afix.Cameras.CameraMessage;
import org.yeshen.video.librtmp.exception.CameraDisabledException;
import org.yeshen.video.librtmp.exception.CameraNotSupportException;
import org.yeshen.video.librtmp.exception.NoCameraException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/*********************************************************************
 * Created by yeshen on 2017/05/25.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/

@SuppressWarnings("deprecation")
public class AndroidUntil {

    public final static int FOCUS_WIDTH = 80;
    public final static int FOCUS_HEIGHT = 80;

    public static final String SHARDE_NULL_VERTEX = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform   mat4 uPosMtx;\n" +
            "uniform   mat4 uTexMtx;\n" +
            "varying   vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "  gl_Position = uPosMtx * position;\n" +
            "  textureCoordinate   = (uTexMtx * inputTextureCoordinate).xy;\n" +
            "}";

    public static final String SHARDE_NULL_FRAGMENT = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    vec4 tc = texture2D(sTexture, textureCoordinate);\n" +
            "    gl_FragColor = vec4(tc.r, tc.g, tc.b, 1.0);\n" +
            "}";

    public static List<CameraMessage> getAllCamerasData(boolean isBackFirst) {
        ArrayList<CameraMessage> cameraDatas = new ArrayList<>();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                CameraMessage cameraData = new CameraMessage(i, CameraMessage.FACING_FRONT);
                if(isBackFirst) {
                    cameraDatas.add(cameraData);
                } else {
                    cameraDatas.add(0, cameraData);
                }
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                CameraMessage cameraData = new CameraMessage(i, CameraMessage.FACING_BACK);
                if(isBackFirst) {
                    cameraDatas.add(0, cameraData);
                } else {
                    cameraDatas.add(cameraData);
                }
            }
        }
        return cameraDatas;
    }

    public static void initCameraParams(Camera camera, CameraMessage cameraData, boolean isTouchMode)
            throws CameraNotSupportException {
        boolean isLandscape = (Options.getInstance().camera.orientation != Options.Orientation.PORTRAIT);
        int cameraWidth = Math.max(Options.getInstance().camera.height, Options.getInstance().camera.width);
        int cameraHeight = Math.min(Options.getInstance().camera.height, Options.getInstance().camera.width);
        Camera.Parameters parameters = camera.getParameters();
        setPreviewFormat(camera, parameters);
        setPreviewFps(camera, Options.getInstance().camera.fps, parameters);
        setPreviewSize(camera, cameraData, cameraWidth, cameraHeight, parameters);
        cameraData.hasLight = supportFlash(camera);
        setOrientation(cameraData, isLandscape, camera);
        setFocusMode(camera, cameraData, isTouchMode);
    }

    public static void setPreviewFormat(Camera camera, Camera.Parameters parameters) throws CameraNotSupportException{
        //设置预览回调的图片格式
        try {
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        } catch (Exception e) {
            throw new CameraNotSupportException();
        }
    }

    public static void setPreviewFps(Camera camera, int fps, Camera.Parameters parameters) {
        //设置摄像头预览帧率
        try {
            parameters.setPreviewFrameRate(fps);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int[] range = adaptPreviewFps(fps, parameters.getSupportedPreviewFpsRange());

        try {
            parameters.setPreviewFpsRange(range[0], range[1]);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int[] adaptPreviewFps(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    public static void setPreviewSize(Camera camera, CameraMessage cameraData, int width, int height,
                                      Camera.Parameters parameters) throws CameraNotSupportException {
        Camera.Size size = getOptimalPreviewSize(camera, width, height);
        if(size == null) {
            throw new CameraNotSupportException();
        }else {
            cameraData.cameraWidth = size.width;
            cameraData.cameraHeight = size.height;
        }
        //设置预览大小
        Lg.d("Camera Width: " + size.width + "    Height: " + size.height);
        try {
            parameters.setPreviewSize(cameraData.cameraWidth, cameraData.cameraHeight);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setOrientation(CameraMessage cameraData, boolean isLandscape, Camera camera) {
        int orientation = getDisplayOrientation(cameraData.cameraID);
        if(isLandscape) {
            orientation = orientation - 90;
        }
        camera.setDisplayOrientation(orientation);
    }

    private static void setFocusMode(Camera camera, CameraMessage cameraData, boolean isTouchMode) {
        cameraData.supportTouchFocus = supportTouchFocus(camera);
        if(!cameraData.supportTouchFocus) {
            setAutoFocusMode(camera);
        } else {
            if(!isTouchMode) {
                cameraData.touchFocusMode = false;
                setAutoFocusMode(camera);
            }else {
                cameraData.touchFocusMode = true;
            }
        }
    }

    public static boolean supportTouchFocus(Camera camera) {
        if(camera != null) {
            return (camera.getParameters().getMaxNumFocusAreas() != 0);
        }
        return false;
    }

    public static void setAutoFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setTouchFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Camera.Size getOptimalPreviewSize(Camera camera, int width, int height) {
        Camera.Size optimalSize = null;
        double minHeightDiff = Double.MAX_VALUE;
        double minWidthDiff = Double.MAX_VALUE;
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return null;
        //找到宽度差距最小的
        for(Camera.Size size:sizes){
            if (Math.abs(size.width - width) < minWidthDiff) {
                minWidthDiff = Math.abs(size.width - width);
            }
        }
        //在宽度差距最小的里面，找到高度差距最小的
        for(Camera.Size size:sizes){
            if(Math.abs(size.width - width) == minWidthDiff) {
                if(Math.abs(size.height - height) < minHeightDiff) {
                    optimalSize = size;
                    minHeightDiff = Math.abs(size.height - height);
                }
            }
        }
        return optimalSize;
    }

    public static int getDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation + 360) % 360;
        }
        return result;
    }

    public static void checkCameraService(Context context)
            throws CameraDisabledException, NoCameraException {
        // Check if device policy has disabled the camera.
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
        List<CameraMessage> cameraDatas = getAllCamerasData(false);
        if(cameraDatas.size() == 0) {
            throw new NoCameraException();
        }
    }

    public static boolean supportFlash(Camera camera){
        Camera.Parameters params = camera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if(flashModes == null) {
            return false;
        }
        for(String flashMode : flashModes) {
            if(Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                return true;
            }
        }
        return false;
    }

    //GL
    public static FloatBuffer createSquareVtx() {
        final float vtx[] = {
                // XYZ, UV
                -1f,  1f, 0f, 0f, 1f,
                -1f, -1f, 0f, 0f, 0f,
                1f,   1f, 0f, 1f, 1f,
                1f,  -1f, 0f, 1f, 0f,
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(vtx);
        fb.position(0);
        return fb;
    }

    public static FloatBuffer createVertexBuffer() {
        final float vtx[] = {
                // XYZ
                -1f,  1f, 0f,
                -1f, -1f, 0f,
                1f,   1f, 0f,
                1f,  -1f, 0f,
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(vtx);
        fb.position(0);
        return fb;
    }

    public static FloatBuffer createTexCoordBuffer() {
        final float vtx[] = {
                // UV
                0f, 1f,
                0f, 0f,
                1f, 1f,
                1f, 0f,
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(vtx);
        fb.position(0);
        return fb;
    }

    public static float[] createIdentityMtx() {
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        return m;
    }

    public static int createProgram(){
        final String vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n";
        final String fragmentShader =
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2  textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";
        return createProgram(vertexShader, fragmentShader);
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER,   vertexSource);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Lg.e("Could not link program:");
            Lg.e( GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        //
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Lg.e( "Could not compile shader(TYPE=" + shaderType + "):");
            Lg.e( GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        //
        return shader;
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Lg.e( op + ": glGetError: 0x" + Integer.toHexString(error));
            throw new RuntimeException("glGetError encountered (see log)");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void checkEglError(String op) {
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Lg.e( op + ": eglGetError: 0x" + Integer.toHexString(error));
            throw new RuntimeException("eglGetError encountered (see log)");
        }
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no match was
     * found.
     */
    @TargetApi(18)
    public static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    @TargetApi(18)
    public static MediaCodec getAudioMediaCodec(){
        MediaFormat format = MediaFormat.createAudioFormat(
                Options.getInstance().audio.mime,
                Options.getInstance().audio.frequency,
                Options.getInstance().audio.channelCount);
        if(Options.getInstance().audio.mime.equals(Options.DEFAULT_MIME)) {
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, Options.getInstance().audio.aacProfile);
        }
        format.setInteger(MediaFormat.KEY_BIT_RATE, Options.getInstance().audio.maxBps * 1024);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, Options.getInstance().audio.frequency);
        int maxInputSize = getRecordBufferSize();
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, Options.getInstance().audio.channelCount);

        MediaCodec mediaCodec = null;
        try {
            mediaCodec = MediaCodec.createEncoderByType(Options.getInstance().audio.mime);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
        }
        return mediaCodec;
    }

    @TargetApi(21)
    public static MediaCodec getVideoMediaCodec() {
        int videoWidth = getVideoSize(Options.getInstance().video.width);
        int videoHeight = getVideoSize(Options.getInstance().video.height);
        MediaFormat format = MediaFormat.createVideoFormat(Options.getInstance().video.mime, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, Options.getInstance().video.maxBps* 1024);
        int fps = Options.getInstance().video.fps;
        //设置摄像头预览帧率
//        if(BlackListHelper.deviceInFpsBlacklisted()) {
//            SopCastLog.d(SopCastConstant.TAG, "Device in fps setting black list, so set mediacodec fps 15");
//            fps = 15;
//        }
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Options.getInstance().video.ifi);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        MediaCodec mediaCodec = null;

        try {
            mediaCodec = MediaCodec.createEncoderByType(Options.getInstance().video.mime);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch (Exception e) {
            e.printStackTrace();
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
        }
        return mediaCodec;
    }

    // We avoid the device-specific limitations on width and height by using values that
    // are multiples of 16, which all tested devices seem to be able to handle.
    public static int getVideoSize(int size) {
        int multiple = (int)Math.ceil(size/16.0);
        return multiple*16;
    }

    public static String getFileContextFromAssets(Context context, String fileName) {
        String fileContent = "";
        try {
            InputStream is = context.getAssets().open(fileName);
            fileContent = inputStream2String(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileContent;
    }

    public static String inputStream2String(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    public static boolean checkMicSupport() {
        boolean result;
        int recordBufferSize = getRecordBufferSize();
        byte[] mRecordBuffer = new byte[recordBufferSize];
        AudioRecord audioRecord = getAudioRecord();
        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int readLen = audioRecord.read(mRecordBuffer, 0, recordBufferSize);
        result = readLen >= 0;
        try {
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getRecordBufferSize() {
        int frequency = Options.getInstance().audio.frequency;
        int audioEncoding = Options.getInstance().audio.encoding;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        if(Options.getInstance().audio.channelCount == 2) {
            channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        }
        return AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
    }

    @TargetApi(18)
    public static AudioRecord getAudioRecord() {
        int frequency = Options.getInstance().audio.frequency;
        int audioEncoding = Options.getInstance().audio.encoding;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        if(Options.getInstance().audio.channelCount == 2) {
            channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        }
        int audioSource = MediaRecorder.AudioSource.MIC;
        if(Options.getInstance().audio.aec) {
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        }
        return new AudioRecord(audioSource, frequency,
                channelConfiguration, audioEncoding, getRecordBufferSize());
    }

}
