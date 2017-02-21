package uiip.dji.pcapi.com.handlers;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.camera.DJICamera;
import uiip.dji.pcapi.com.Logger;
import uiip.dji.pcapi.com.PcApiApplication;
import uiip.dji.pcapi.com.media.DJIVideoStreamDecoder;
import uiip.dji.pcapi.com.media.NativeHelper;

import static dji.midware.data.forbid.DJIFlyForbidController.DataSwitchEvent.DJI;

/**
 * Created by dji on 16.01.2017.
 */

class VideoYuvWriterStrategy extends HandleStrategy
                             implements DJIVideoStreamDecoder.IYuvDataListener{

    public VideoYuvWriterStrategy(final Socket client){
        super(client);

        DJICamera camera = PcApiApplication.getCameraInstance();
        if (camera != null) {
            DJIVideoStreamDecoder.getInstance().setYuvDataListener(this);
            camera.setDJICameraReceivedVideoDataCallback(
                    new DJICamera.CameraReceivedVideoDataCallback() {
                        @Override
                        public void onResult(byte[] bytes, int length) {
                            //Logger.log("Available bytes: " + bytes.length);
                            //Logger.log("Size: " + length);
                            //Logger.log(Arrays.toString(bytes));
                            try {
                                DJIVideoStreamDecoder.getInstance().parse(bytes, length);
                            }
                            catch(Exception e){
                                Logger.log("CameraCallback: " + e.getMessage());
                            }
                        }
                    });
        }
    }

    @Override
    protected void readMessage(BufferedReader reader) {
        throw new UnsupportedOperationException("Unexpected input");
    }

    @Override
    protected void writeMessage(OutputStream ostream) {

    }

    @Override
    protected boolean isNeedWrite() {
        return false;
    }

    private void writeInt(OutputStream out, int i) throws IOException{
        out.write((byte)( i >> 24 ));
        out.write((byte)( (i << 8) >> 24 ));
        out.write((byte)( (i << 16) >> 24 ));
        out.write((byte)( (i << 24) >> 24 ));
    }

    @Override
    public void onYuvDataReceived(byte[] yuvFrame, int width, int height) {
        int frameIndex = DJIVideoStreamDecoder.getInstance().frameIndex;
        Logger.log("Getted frame with index "+frameIndex);

        try {
            YuvImage yuvImage = new YuvImage(yuvFrame, ImageFormat.NV21 ,width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            byte[] jpeg = out.toByteArray();

            Logger.log("Size: " + jpeg.length);

            writeInt(client.getOutputStream(), frameIndex);
            writeInt(client.getOutputStream(), jpeg.length);
            client.getOutputStream().write(jpeg);
            client.getOutputStream().flush();
        }
        catch (RuntimeException e){
            Logger.log("YuvWriter: " + e.getMessage() + " w: " + width + " h: " + height
                    + " a: " + yuvFrame.length);
        }
        catch (IOException e) {
            Logger.log("YuvWriter: " + e.getMessage());
        }
    }

    @Override
    protected void doJob() {
        DJIVideoStreamDecoder.getInstance().setYuvDataListener(this);
        startRecord();
    }

    @Override
    protected void interrupt() {
        stopRecord();
        DJIVideoStreamDecoder.getInstance().setYuvDataListener(null);
    }

    private void switchCameraMode(DJICameraSettingsDef.CameraMode cameraMode){

        DJICamera camera = PcApiApplication.getCameraInstance();
        if (camera != null) {
            camera.setCameraMode(cameraMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        //Logger.showToast("Switch Camera Mode Succeeded");
                        Logger.log("Switch Camera Mode Succeeded");
                    } else {
                        //Logger.showToast(error.getDescription());
                        Logger.log(error.getDescription());
                    }
                }
            });
        }

    }

    // Method for starting recording
    private void startRecord(){
        switchCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);

        final DJICamera camera = PcApiApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new DJICommonCallbacks.DJICompletionCallback(){
                @Override
                public void onResult(DJIError error)
                {
                    if (error == null) {
                        //Logger.showToast("Record video: success");
                        Logger.log("Record video: success");
                    }else {
                        //Logger.showToast(error.getDescription());
                        Logger.log(error.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        DJICamera camera = PcApiApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new DJICommonCallbacks.DJICompletionCallback(){

                @Override
                public void onResult(DJIError error)
                {
                    if(error == null) {
                        //Logger.showToast("Stop recording: success");
                        Logger.log("Stop recording: success");
                    }else {
                        //Logger.showToast(error.getDescription());
                        Logger.log(error.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }
}
