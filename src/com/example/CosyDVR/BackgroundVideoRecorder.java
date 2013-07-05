package com.example.CosyDVR;

import android.app.Service;
//import android.app.Notification;
//import android.support.v4.app.NotificationCompat.Builder;
import android.content.Context;
import android.content.Intent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.Gravity;
import android.view.SurfaceView;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.AudioManager;
import android.os.Environment;
import android.text.format.DateFormat;
//import android.os.Build;
import android.os.IBinder;
import android.os.Binder;
import java.util.Date;
import java.util.Arrays;
import java.util.Comparator;
import java.io.File;
import java.lang.String;
import android.os.PowerManager;



public class BackgroundVideoRecorder extends Service implements SurfaceHolder.Callback, MediaRecorder.OnInfoListener {
//CONSTANTS-OPTIONS
	public long MAX_TEMP_FOLDER_SIZE = 10000000;
	public long MIN_FREE_SPACE = 1000000;
	public int MAX_VIDEO_DURATION = 600000;
	public int VIDEO_WIDTH = 1280;
	public int VIDEO_HEIGHT= 720;
	public int MAX_VIDEO_BIT_RATE = 5000000;
	//public int AUDIO_SOURCE = CAMERA;
	
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private boolean isrecording = false;
    private boolean isflashon = false;
    private int isfavorite = 0;
    private String currentfile = null;
    //private String previousfile = null;
    private SurfaceHolder mSurfaceHolder = null;
    private PowerManager.WakeLock WakeLock;


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
    	BackgroundVideoRecorder getService() {
            // Return this instance of LocalService so clients can call public methods
            return BackgroundVideoRecorder.this;
        }
    }
 
    @Override
    public void onCreate() {
        // Start foreground service to avoid unexpected kill
/*        Notification notification = new Notification.Builder(this)
            .setContentTitle("Background Video Recorder")
            .setContentText("")
            .setSmallIcon(R.drawable.cosydvricon)
            .build();
        startForeground(1234, notification);
*/
        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
         surfaceView = new SurfaceView(this);
        LayoutParams layoutParams = new WindowManager.LayoutParams(
        	//WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        	1,1,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        WakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CosyDVRWakeLock");

    }
    
    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	mSurfaceHolder = surfaceHolder;
    }

    public int isFavorite(){
    	return isfavorite;
    }
    
    public boolean isRecording(){
    	return isrecording;
    }

    public void StopRecording() {
    	StopResetReleaseLock();
    }
    public void Restartrecording() {
    	AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    	manager.setStreamSolo(AudioManager.STREAM_SYSTEM,true);
    	manager.setStreamMute(AudioManager.STREAM_SYSTEM,true);
    	StopResetReleaseLock();
    	OpenUnlockPrepareStart();
    	freeSpace();
    	manager.setStreamMute(AudioManager.STREAM_SYSTEM,false);
    	manager.setStreamSolo(AudioManager.STREAM_SYSTEM,false);
    }
    
	public void StartRecording() {
		OpenUnlockPrepareStart();
	}

	private void StopResetReleaseLock() {
		if(isrecording) {
			mediaRecorder.stop();
		    mediaRecorder.reset();
		    if(currentfile != null && isfavorite != 0) {
		    	File tmpfile = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/temp/" + currentfile);
		    	File favfile = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/fav/" + currentfile);
		    	tmpfile.renameTo(favfile);
		    	if(isfavorite == 1) {
		    		isfavorite = 0;
		    	}
		    }
		    mediaRecorder.release();
		
		    camera.lock();
		    camera.release();
		    WakeLock.release();
		    isrecording = false;
		}
	}

	private void OpenUnlockPrepareStart() {
		if(!isrecording) {
			WakeLock.acquire();
		    camera = Camera.open();
		    mediaRecorder = new MediaRecorder();
		    camera.unlock();
		
		    mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		    mediaRecorder.setCamera(camera);
	
		    mediaRecorder.setMaxDuration(this.MAX_VIDEO_DURATION);
		    mediaRecorder.setMaxFileSize(0); // 0 - no limit
		  //mediaRecorder.setOnErrorListener
		    mediaRecorder.setOnInfoListener(this);
	
	
		    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		
		    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		
		    mediaRecorder.setAudioEncoder(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioCodec);//MediaRecorder.AudioEncoder.HE_AAC
		    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		
		    currentfile = DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) + ".mp4";
		    mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/CosyDVR/temp/" + currentfile);
		
		    mediaRecorder.setAudioChannels(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioChannels);
		    mediaRecorder.setAudioSamplingRate(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioSampleRate);
		    mediaRecorder.setAudioEncodingBitRate(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioBitRate);
		
		    mediaRecorder.setVideoEncodingBitRate(this.MAX_VIDEO_BIT_RATE);
		    mediaRecorder.setVideoSize(this.VIDEO_WIDTH,this.VIDEO_HEIGHT);// 640x480,800x480
		    mediaRecorder.setVideoFrameRate(30);
		
		    
		    try { mediaRecorder.prepare(); } catch (Exception e) {}
		    mediaRecorder.start();
		    isrecording = true;
		}
	}
	
	public void freeSpace() {
		File dir = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/temp/");
		File[] filelist = dir.listFiles();
		Arrays.sort(filelist, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.valueOf(f2.lastModified()).compareTo(
                        f1.lastModified());
            }
        });
			long totalSize = 0; //in kB
			int i;
		for(i = 0; i < filelist.length; i++){
			totalSize += filelist[i].length()/1024;
		}
		i = filelist.length - 1;
		while (i > 0 && (totalSize > this.MAX_TEMP_FOLDER_SIZE || dir.getFreeSpace() < this.MIN_FREE_SPACE)) {
			totalSize -= filelist[i].length()/1024;
			filelist[i].delete();
			i--;
		}
	}

    public void toggleFavorite() {
    	isfavorite = (isfavorite + 1) % 3;
    }

    public void toggleRecording() {
    	if(isrecording){
    		StopRecording();
    	} else {
    		StartRecording();
    	}
    }

    public void toggleFlash() {
    	if(camera != null){
    		Parameters parameters = camera.getParameters();
	    	if(isflashon){
	    		parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
	    		isflashon = false;
	    	} else {
	    		parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
	            isflashon = true;
	    	}
	    	camera.setParameters(parameters);
    	}
    }

    public void ChangeSurface(int width, int height){
    	if(this.VIDEO_WIDTH/this.VIDEO_HEIGHT > width/height) {
    		height = (int) (width * this.VIDEO_HEIGHT / this.VIDEO_WIDTH);
    	}
    	else {
    		width = (int) (height * this.VIDEO_WIDTH / this.VIDEO_HEIGHT);
    	}
        LayoutParams layoutParams = new WindowManager.LayoutParams(
            	//WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            	width,height,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            );
        if (width == 1) {
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        } else {
        	layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        }
		windowManager.updateViewLayout(surfaceView, layoutParams);
	}

    // Stop isrecording and remove SurfaceView
    @Override
    public void onDestroy() {
    	StopResetReleaseLock();

        windowManager.removeView(surfaceView);

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {                     
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            this.Restartrecording();
        }          
    }
}
