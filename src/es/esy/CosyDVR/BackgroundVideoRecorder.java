package es.esy.CosyDVR;

import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.AudioManager;
import android.os.Environment;
import android.text.format.DateFormat;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.os.Message;
import android.os.SystemClock;
import android.os.Bundle;

import java.util.Date;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.String;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.net.UnknownHostException;

import android.os.PowerManager;
import android.preference.PreferenceManager;

public class BackgroundVideoRecorder extends Service implements
		SurfaceHolder.Callback, MediaRecorder.OnInfoListener, LocationListener {
	// CONSTANTS-OPTIONS
	public long MAX_TEMP_FOLDER_SIZE = 10000000;
	public long MIN_FREE_SPACE = 1000000;
	public int MAX_VIDEO_DURATION = 600000;
	public int VIDEO_WIDTH = 1280;// 1920;
	public int VIDEO_HEIGHT = 720;// 1080;
	public int VIDEO_FRAME_RATE = 30;
	public int TIME_LAPSE_FACTOR = 1;
	public int MAX_VIDEO_BIT_RATE = 5000000;
	// public int MAX_VIDEO_BIT_RATE = 256000; //=for streaming;
	public int REFRESH_TIME = 1000;
	public String VIDEO_FILE_EXT = ".mp4";
	public String SRT_FILE_EXT = ".srt";
	public String GPX_FILE_EXT = ".gpx";
	// public int AUDIO_SOURCE = CAMERA;
	public String SD_CARD_PATH = Environment.getExternalStorageDirectory()
			.getAbsolutePath();
	//~ public String BASE_FOLDER = "/CosyDVR";
	public String BASE_FOLDER = "/Android/data/es.esy.CosyDVR/files"; //possible fix for KitKat
	public String FAV_FOLDER = "/fav/";
	public String TEMP_FOLDER = "/temp/";
/*for KitKat we can use something like:
* final File[] dirs = context.getExternalFilesDirs(null); //null means default type
* //find a dir that has most of the space and save using StatFs
*/
	public boolean AUTOSTART = false;
	public boolean USEGPS = true;
        public boolean REVERSE_ORIENTATION = false;

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();
	private WindowManager windowManager = null;
	private SurfaceView surfaceView = null;
	private Camera camera = null;
	private MediaRecorder mediaRecorder = null;
	private boolean isrecording = false;
	private int isfavorite = 0; // 0-no 1=permanentfav 2=favonce
	private int focusmode = 0;
	private int scenemode = 0;
	private int flashmode = 0;
	private int timelapsemode = 0;
	private int zoomfactor = 0;
	private String currentfile = null;

	private SurfaceHolder mSurfaceHolder = null;
	private PowerManager.WakeLock mWakeLock = null;
	private Timer mTimer = null;
	private TimerTask mTimerTask = null;

	public TextView mTextView = null;
	public TextView mSpeedView = null;
	public TextView mBatteryView = null;
	public long mSrtCounter = 0;
	public Handler mHandler = null;

	private File mSrtFile = null;
	private File mGpxFile = null;
	private OutputStreamWriter mSrtWriter = null;
	private OutputStreamWriter mGpxWriter = null;
	private long mSrtBegin = 0;
	private long mNewFileBegin = 0;

	private LocationManager mLocationManager = null;
	private Location mLocation = null;
	private long mPrevTim = 0;

	// private List<String> mFocusModes;
	private String[] mFocusModes = { Parameters.FOCUS_MODE_INFINITY,
			Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, Parameters.FOCUS_MODE_AUTO,
			Parameters.FOCUS_MODE_MACRO, Parameters.FOCUS_MODE_EDOF, };

	private String[] mSceneModes = { Parameters.SCENE_MODE_AUTO,
			Parameters.SCENE_MODE_NIGHT, };


	private String[] mFlashModes = { Parameters.FLASH_MODE_OFF,
			Parameters.FLASH_MODE_TORCH, };

	// some troubles with video files @SuppressLint("HandlerLeak")
	private final class HandlerExtension extends Handler {
		public void handleMessage(Message msg) {
			if (!isrecording) {
				return;
			}
			/* every second update for debug purposes only
			 * Intent intent = new Intent();
			 * intent.setAction("es.esy.CosyDVR.updateinterface");
			 * sendBroadcast(intent);
			 */
			
			String srt = new String();
			String gpx = new String();
			Date datetime = new Date();
			long tick = (mSrtBegin - mNewFileBegin)/TIME_LAPSE_FACTOR; // relative srt text begin/
													// i.e. prev tick time
			int hour = (int) (tick / (1000 * 60 * 60));
			int min = (int) (tick % (1000 * 60 * 60) / (1000 * 60));
			int sec = (int) (tick % (1000 * 60) / (1000));
			int mil = (int) (tick % (1000));
			srt = srt
					+ String.format("%d\n%02d:%02d:%02d,%03d --> ",
							mSrtCounter, hour, min, sec, mil);

			mSrtBegin = SystemClock.elapsedRealtime();
			tick = (mSrtBegin - mNewFileBegin)/TIME_LAPSE_FACTOR; // relative srt text end. i.e.
												// this tick time
			hour = (int) (tick / (1000 * 60 * 60));
			min = (int) (tick % (1000 * 60 * 60) / (1000 * 60));
			sec = (int) (tick % (1000 * 60) / (1000));
			mil = (int) (tick % (1000));
			srt = srt
					+ String.format("%02d:%02d:%02d,%03d\n", hour, min, sec,
							mil);
			srt = srt
					+ DateFormat.format("yyyy-MM-dd_kk-mm-ss",
							datetime.getTime()).toString() + "\n";

                        if (USEGPS) {
                                // Get the location manager
			        // Criteria criteria = new Criteria();
			        // String bestProvider = mLocationManager.getBestProvider(criteria,
		        	// false);
		        	// Location location =
		        	// mLocationManager.getLastKnownLocation(bestProvider);
			        try {
			                mLocation = mLocationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			        } catch (Exception e) {
			        	mLocation = null;
			        }
                        }

			double lat = -1, lon = -1, alt = -1;
			float spd = 0, acc = -1;
			int sat = 0, bat = 0;
			long tim = 0;

			if (USEGPS && mLocation != null) {
				/*
				 * if (mPrevLocation == null) { mPrevLocation = mLocation; //for
				 * null to not occur during operation }
				 */
				lat = mLocation.getLatitude();
				lon = mLocation.getLongitude();
				tim = mLocation.getTime() / 1000; // millisec to sec
				alt = mLocation.getAltitude();
				acc = mLocation.getAccuracy();
				spd = mLocation.getSpeed() * 3.6f; // by GPS
				/*
				 * if(mLocation.getTime() != mPrevLocation.getTime() &&
				 * (mLocation.getTime()-3000) < mPrevLocation.getTime()){ spd =
				 * 3.6f * 1000 * mLocation.distanceTo(mPrevLocation) /
				 * (mLocation.getTime() - mPrevLocation.getTime()); }
				 */
				sat = mLocation.getExtras().getInt("satellites");
				// mPrevLocation = mLocation;
			}

			srt = srt
					+ String.format(
							"lat:%1.6f,lon:%1.6f,alt:%1.0f,spd:%1.1fkm/h\nacc:%01.1fm,sat:%d,tim:%d\n\n",
							lat, lon, alt, spd, acc, sat, tim);
			gpx = gpx
					+ String.format("<trkpt lon=\"%1.8f\" lat=\"%1.8f\">\n",
							lon, lat).replace(",", ".");
			gpx = gpx + String.format("<ele>%1.0f</ele>\n", alt);
			gpx = gpx
					+ "<time>"
					+ DateFormat.format("yyyy-MM-dd", datetime.getTime())
							.toString()
					+ "T"
					+ DateFormat.format("kk:mm:ss", datetime.getTime())
							.toString() + "Z</time>\n";
			gpx = gpx + "</trkpt>\n";
			if (mPrevTim == 0 && tim != mPrevTim) {
				mPrevTim = tim;
			}
			try {
				if (isrecording) {
					mSrtWriter.write(srt);
					if (tim != mPrevTim && lat > 0) {
						mGpxWriter.write(gpx);
					}
				}
			} catch (IOException e) {
			}
			mTextView.setText(srt);
			if (tim != mPrevTim) {
				mSpeedView.setText(String.format("%1.1f", spd));
				mPrevTim = tim;
			} else {
                                if (USEGPS && mLocationManager != null && !mLocationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					mSpeedView.setText(getString(R.string.gps_off));
				} else {
					mSpeedView.setText("---");
				}
			}
			if(TIME_LAPSE_FACTOR > 1){
				mSpeedView.append("("+TIME_LAPSE_FACTOR+"x)");
			}
			if (sat < 3) {
				mSpeedView.setTextColor(Color.parseColor("#A0A0A0")); // gray
			} else if (sat < 6) {
				mSpeedView.setTextColor(Color.parseColor("#FF0000")); // red
			} else if (sat < 9) {
				mSpeedView.setTextColor(Color.parseColor("#FFC800")); // yellow
			} else {
				mSpeedView.setTextColor(Color.parseColor("#0b9800")); // green
			}
			bat = getBatteryLevel(getApplicationContext());
			if(bat>=0){
				mBatteryView.setText(String.format("%d%%",bat));
			} else {
				mBatteryView.setText("");
			}
		}
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		BackgroundVideoRecorder getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return BackgroundVideoRecorder.this;
		}
	}

	@Override
	public void onCreate() {
		// read first time shared preferences
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		AUTOSTART = sharedPref.getBoolean("autostart_recording", false);
		USEGPS = sharedPref.getBoolean("use_gps", true);
                REVERSE_ORIENTATION = sharedPref.getBoolean("reverse_landscape", false);
		/*
		 * MAX_VIDEO_BIT_RATE =
		 * Integer.parseInt(sharedPref.getString("video_bitrate", "5000000"));
		 * VIDEO_WIDTH = Integer.parseInt(sharedPref.getString("video_width",
		 * "1280")); VIDEO_HEIGHT =
		 * Integer.parseInt(sharedPref.getString("video_height", "720"));
		 * MAX_VIDEO_DURATION =
		 * Integer.parseInt(sharedPref.getString("video_duration", "600000"));
		 * MAX_TEMP_FOLDER_SIZE =
		 * Integer.parseInt(sharedPref.getString("max_temp_folder_size",
		 * "600000")); MIN_FREE_SPACE =
		 * Integer.parseInt(sharedPref.getString("min_free_space", "600000"));
		 */
		SD_CARD_PATH = sharedPref.getString("sd_card_path", Environment
				.getExternalStorageDirectory().getAbsolutePath());

		// Start foreground service to avoid unexpected kill

		Intent myIntent = new Intent(this, CosyDVR.class);
		PendingIntent pendingIntent = PendingIntent.getActivity( this, 0,
				myIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		  
		Notification notification = new Notification.Builder(this)
				.setContentTitle("CosyDVR Background Recorder Service")
				.setContentText("") .setSmallIcon(R.drawable.cosydvricon)
				.setContentIntent(pendingIntent) .build(); 
		startForeground(1, notification);
		 
		// Create new SurfaceView, set its size to 1x1, move it to the top left
		// corner and set this service as a callback
		windowManager = (WindowManager) this
				.getSystemService(Context.WINDOW_SERVICE);
		surfaceView = new SurfaceView(this);
		LayoutParams layoutParams = new WindowManager.LayoutParams(
				// WindowManager.LayoutParams.WRAP_CONTENT,
				// WindowManager.LayoutParams.WRAP_CONTENT,
				1, 1, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);

		layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
		windowManager.addView(surfaceView, layoutParams);

		mTextView = new TextView(this);
		layoutParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		windowManager.addView(mTextView, layoutParams);
		mTextView.setTextColor(Color.parseColor("#FFFFFF"));
		mTextView.setShadowLayer(5, 0, 0, Color.parseColor("#000000"));
		mTextView.setText("--");

		mSpeedView = new TextView(this);
		layoutParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
		windowManager.addView(mSpeedView, layoutParams);
		mSpeedView.setTextColor(Color.parseColor("#A0A0A0"));
		mSpeedView.setShadowLayer(5, 0, 0, Color.parseColor("#000000"));
		mSpeedView.setTextSize(56);
		mSpeedView.setText("---");

		mBatteryView = new TextView(this);
		layoutParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		layoutParams.gravity = Gravity.CENTER;
		windowManager.addView(mBatteryView, layoutParams);
		mBatteryView.setTextColor(Color.parseColor("#FFFFFF"));
		mBatteryView.setShadowLayer(5, 0, 0, Color.parseColor("#000000"));
		mBatteryView.setTextSize(80);
		mBatteryView.setText("");

		surfaceView.getHolder().addCallback(this);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"CosyDVRWakeLock");

		mHandler = new HandlerExtension();

		startGps();

	}

	// Method called right after Surface created (initializing and starting
	// MediaRecorder)
	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		if (AUTOSTART) {
			StartRecording();
		}
	}

	public int getFocusMode() {
		return focusmode;
	}

	public int isFavorite() {
		return isfavorite;
	}

	public boolean isRecording() {
		return isrecording;
	}

	public void StopRecording() {
		if (isrecording) {
			Stop();
			ResetReleaseLock();
			mTimer.cancel();
			mTimer.purge();
			mTimer = null;
			mTimerTask.cancel();
			mTimerTask = null;
			try {
				mGpxWriter.write("</trkseg>\n" + "</trk>\n" + "</gpx>"); // GPX
																			// footer

				mSrtWriter.flush();
				mSrtWriter.close();
				mGpxWriter.flush();
				mGpxWriter.close();
			} catch (IOException e) {
			}
			;

			if (currentfile != null && isfavorite != 0) {
				File tmpfile = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER // "/CosyDVR/temp/"
						+ currentfile + VIDEO_FILE_EXT);
				File favfile = new File(SD_CARD_PATH + BASE_FOLDER + FAV_FOLDER // "/CosyDVR/fav/"
						+ currentfile + VIDEO_FILE_EXT);
				tmpfile.renameTo(favfile);
				tmpfile = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER // "/CosyDVR/temp/"
						+ currentfile + SRT_FILE_EXT);
				favfile = new File(SD_CARD_PATH + BASE_FOLDER + FAV_FOLDER // "/CosyDVR/fav/" 
						+ currentfile + SRT_FILE_EXT);
				tmpfile.renameTo(favfile);
				tmpfile = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER // "/CosyDVR/temp/"
						+ currentfile + GPX_FILE_EXT);
				favfile = new File(SD_CARD_PATH + BASE_FOLDER + FAV_FOLDER // "/CosyDVR/fav/" 
						+ currentfile + GPX_FILE_EXT);
				tmpfile.renameTo(favfile);
				if (isfavorite == 2) {
					isfavorite = 0;
				}
			}
			isrecording = false;
		}
	}

	public void UpdateLayoutInterface() {
		Intent intent = new Intent();
		intent.setAction("es.esy.CosyDVR.updateinterface");
		sendBroadcast(intent); 
	}
	
	public void RestartRecording() {
		AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		manager.setStreamSolo(AudioManager.STREAM_SYSTEM, true);
		manager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
		StopRecording();
		StartRecording();
		manager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		manager.setStreamSolo(AudioManager.STREAM_SYSTEM, false);
	}

	public void StartRecording() {
		/* Rereading preferences */
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		MAX_VIDEO_BIT_RATE = Integer.parseInt(sharedPref.getString(
				"video_bitrate", "5000000"));
		VIDEO_WIDTH = Integer.parseInt(sharedPref.getString("video_width",
				"1280"));
		VIDEO_HEIGHT = Integer.parseInt(sharedPref.getString("video_height",
				"720"));
		VIDEO_FRAME_RATE = Integer.parseInt(sharedPref.getString("video_frame_rate",
				"30"));
		TIME_LAPSE_FACTOR = (timelapsemode==0) ? 1: Integer.parseInt(sharedPref.getString("time_lapse_factor",
				"1"));
		MAX_VIDEO_DURATION = Integer.parseInt(sharedPref.getString(
				"video_duration", "600000"));
		MAX_TEMP_FOLDER_SIZE = Integer.parseInt(sharedPref.getString(
				"max_temp_folder_size", "600000"));
		MIN_FREE_SPACE = Integer.parseInt(sharedPref.getString(
				"min_free_space", "600000"));
		SD_CARD_PATH = sharedPref.getString("sd_card_path", Environment
				.getExternalStorageDirectory().getAbsolutePath());

		// create temp and fav folders
		File mFolder = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER); //"/CosyDVR/temp/");
		if (!mFolder.exists()) {
			mFolder.mkdirs();
		}
		mFolder = new File(SD_CARD_PATH + BASE_FOLDER + FAV_FOLDER); //"/CosyDVR/fav/");
		if (!mFolder.exists()) {
			mFolder.mkdirs();
		}

		//first of all make sure we have enough free space
		freeSpace();

		/* start */
		OpenUnlockPrepareStart();
		applyCameraParameters();
		/*
		 * debug Parameters parameters = camera.getParameters();
		 * //parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO); if
		 * (parameters.getSupportedFocusModes().contains(Parameters.
		 * FOCUS_MODE_INFINITY)) {
		 * parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY); }
		 * 
		 * //parameters.setFocusMode(Parameters.FOCUS_MODE_FIXED); if(!isnight){
		 * if(parameters.getSupportedSceneModes() != null &&
		 * parameters.getSupportedSceneModes
		 * ().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
		 * parameters.setSceneMode(Parameters.SCENE_MODE_AUTO); } } else {
		 * if(parameters.getSupportedSceneModes() != null &&
		 * parameters.getSupportedSceneModes
		 * ().contains(Camera.Parameters.SCENE_MODE_NIGHT)) {
		 * parameters.setSceneMode(Parameters.SCENE_MODE_NIGHT); } }
		 * 
		 * camera.setParameters(parameters);
		 */
		mSrtCounter = 0;
		mSrtFile = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER //"/CosyDVR/temp/"
				 + currentfile + SRT_FILE_EXT);
		mSrtFile.setWritable(true);
		mGpxFile = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER //"/CosyDVR/temp/" 
				+ currentfile + GPX_FILE_EXT);
		mGpxFile.setWritable(true);
		try {
			mSrtWriter = new FileWriter(mSrtFile);
			mGpxWriter = new FileWriter(mGpxFile);
			mGpxWriter
					.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
							+ "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"CosyDVR\">\n"
							+ "<trk>\n" + "<trkseg>\n"); // header
		} catch (IOException e) {
		};
        
		mNewFileBegin = SystemClock.elapsedRealtime();
		mSrtBegin = mNewFileBegin;

		mTimer = new Timer();
		mTimerTask = new TimerTask() {
			@Override
			public void run() {
				// What you want to do goes here
				mSrtCounter++;
				mHandler.obtainMessage(1).sendToTarget();
			}
		};
		mTimer.scheduleAtFixedRate(mTimerTask, 0, REFRESH_TIME * TIME_LAPSE_FACTOR);
		UpdateLayoutInterface();
	}

	private void Stop() {
		if (isrecording) {
			mediaRecorder.stop();
		}
	}

	private void ResetReleaseLock() {
		if (isrecording) {
			mediaRecorder.reset();
			mediaRecorder.release();

			camera.lock();
			camera.release();
			mWakeLock.release();
		}
	}

	private void OpenUnlockPrepareStart() {
		if (!isrecording) {
			mWakeLock.acquire();
			try {
				camera = Camera.open(/* CameraInfo.CAMERA_FACING_BACK */);
                if(REVERSE_ORIENTATION) {
                    camera.setDisplayOrientation(180);
                } else {
                    camera.setDisplayOrientation(0);
                }
				mediaRecorder = new MediaRecorder();
                camera.unlock();

				// mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
				mediaRecorder.setCamera(camera);

				// Step 2: Set sources
				mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
				mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

				// Step 3: Set a CamcorderProfile (requires API Level 8 or
				// higher)
				// mediaRecorder.setProfile(CamcorderProfile
				// .get(CamcorderProfile.QUALITY_HIGH));
				mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
				mediaRecorder.setAudioEncoder(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH).audioCodec);// MediaRecorder.AudioEncoder.HE_AAC
				mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

				mediaRecorder.setVideoEncodingBitRate(this.MAX_VIDEO_BIT_RATE);
				mediaRecorder.setVideoSize(this.VIDEO_WIDTH, this.VIDEO_HEIGHT);// 640x480,800x480
				mediaRecorder.setVideoFrameRate(this.VIDEO_FRAME_RATE);
				if(this.TIME_LAPSE_FACTOR > 1) {
					mediaRecorder.setCaptureRate(1.0 * this.VIDEO_FRAME_RATE / this.TIME_LAPSE_FACTOR);
				}
				currentfile = DateFormat.format("yyyy-MM-dd_kk-mm-ss",
						new Date().getTime()).toString();
				// if we write to file
				mediaRecorder.setOutputFile(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER //"/CosyDVR/temp/"
						+ currentfile + VIDEO_FILE_EXT);
				// if we stream
				/*
				 * String hostname =
				 * "rtmp://a.rtmp.youtube.com/stetsuk.80gq-20ea-tet3-2hfb"; int
				 * port = 1234; Socket socket; try { socket = new
				 * Socket(InetAddress.getByName(hostname), port);
				 * ParcelFileDescriptor pfd =
				 * ParcelFileDescriptor.fromSocket(socket);
				 * mediaRecorder.setOutputFile(pfd.getFileDescriptor()); } catch
				 * (UnknownHostException e1) { // TODO Auto-generated catch
				 * block e1.printStackTrace(); } catch (IOException e1) { //
				 * TODO Auto-generated catch block e1.printStackTrace(); }
				 */

				/*
				 * mediaRecorder.setAudioChannels(CamcorderProfile.get(
				 * CamcorderProfile.QUALITY_HIGH).audioChannels);
				 * mediaRecorder.setAudioSamplingRate
				 * (CamcorderProfile.get(CamcorderProfile
				 * .QUALITY_HIGH).audioSampleRate);
				 * mediaRecorder.setAudioEncodingBitRate
				 * (CamcorderProfile.get(CamcorderProfile
				 * .QUALITY_HIGH).audioBitRate);
				 */

				// Step 5: Set the preview output
				mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
				// Step 6: Duration and listener
				mediaRecorder.setMaxDuration(this.MAX_VIDEO_DURATION);
				mediaRecorder.setMaxFileSize(0); // 0 - no limit
				mediaRecorder.setOnInfoListener(this);
                if(REVERSE_ORIENTATION) {
                    mediaRecorder.setOrientationHint(180);
                } else {
                    mediaRecorder.setOrientationHint(0);
                }

				mediaRecorder.prepare();
				mediaRecorder.start();
				isrecording = true;
			} catch (Exception e) {
				isrecording = true;
				ResetReleaseLock();
			}
		}
	}

	public void freeSpace() {
		File dir = new File(SD_CARD_PATH + BASE_FOLDER + TEMP_FOLDER); //"/CosyDVR/temp/");
		File[] filelist = dir.listFiles();
		Arrays.sort(filelist, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f2.lastModified()).compareTo(
						f1.lastModified());
			}
		});
		long totalSize = 0; // in kB
		int i;
		for (i = 0; i < filelist.length; i++) {
			totalSize += filelist[i].length() / 1024;
		}
		i = filelist.length - 1;
		// if(Build.VERSION.SDK_INT >= 11) {
		while (i > 0
				&& (totalSize > this.MAX_TEMP_FOLDER_SIZE
				|| dir.getFreeSpace() < this.MIN_FREE_SPACE)) {
			totalSize -= filelist[i].length() / 1024;
			filelist[i].delete();
			i--;
		}
		// } else {
		// while (i > 0 && totalSize > this.MAX_TEMP_FOLDER_SIZE) {
		// totalSize -= filelist[i].length()/1024;
		// filelist[i].delete();
		// i--;
		// }
		// }
	}

	public void autoFocus() {
		if (mFocusModes[focusmode] == Parameters.FOCUS_MODE_AUTO
				|| mFocusModes[focusmode] == Parameters.FOCUS_MODE_MACRO) {
			camera.autoFocus(null);
		}
	}
	
	public void applyCameraParameters() {
		if (camera != null) {
			Parameters parameters = camera.getParameters();
			if(parameters.getSupportedFocusModes().contains(mFocusModes[focusmode])) {
				parameters.setFocusMode(mFocusModes[focusmode]);
			}
			if(parameters.getSupportedSceneModes() != null
                && parameters.getSupportedSceneModes().contains(mSceneModes[scenemode])) {
				parameters.setSceneMode(mSceneModes[scenemode]);
			}
			if(parameters.getSupportedFlashModes() != null 
                && parameters.getSupportedFlashModes().contains(mFlashModes[flashmode])) {
				parameters.setFlashMode(mFlashModes[flashmode]);
			}
            if (parameters.isZoomSupported()) {
                parameters.setZoom(zoomfactor);
                camera.setParameters(parameters);
            }
			camera.setParameters(parameters);
		}
	}

	public void toggleFocus() {
		if (camera != null) {
			Parameters parameters = camera.getParameters();
			do {
				focusmode = (focusmode + 1) % mFocusModes.length;
			} while (!parameters.getSupportedFocusModes().contains(
					mFocusModes[focusmode])); // SKIP unsupported modes
			applyCameraParameters();
		}
	}

	public void toggleNight() {
		if (camera != null) {
			Parameters parameters = camera.getParameters();
			do {
				scenemode = (scenemode + 1) % mSceneModes.length;
			} while (!parameters.getSupportedSceneModes().contains(
					mSceneModes[scenemode])); // SKIP unsupported modes
			applyCameraParameters();
		}
	}

	public void toggleFlash() {
		if (camera != null) {
			Parameters parameters = camera.getParameters();
			do {
				flashmode = (flashmode + 1) % mFlashModes.length;
			} while (!parameters.getSupportedFlashModes().contains(
					mFlashModes[flashmode])); // SKIP unsupported modes
			applyCameraParameters();
		}
	}

	public void toggleTimeLapse() {
		if (camera != null) {
			timelapsemode = (timelapsemode+1)%2;
			RestartRecording();
		}
	}

	public void setZoom(float mval) {
		if (camera != null) {
			Parameters parameters = camera.getParameters();
			if (parameters.isZoomSupported()) {
                zoomfactor = (int) (parameters.getMaxZoom() * (mval - 4) / 10.0);
				parameters.setZoom(zoomfactor);
				camera.setParameters(parameters);
			}
		}

	}

	public void toggleFavorite() {
		isfavorite = (isfavorite + 1) % 3;
	}

	public void toggleRecording() {
		if (isrecording) {
			StopRecording();
		} else {
			StartRecording();
		}
	}

	public void ChangeSurface(int width, int height) {
		if (this.VIDEO_WIDTH / this.VIDEO_HEIGHT > width / height) {
			height = (int) (width * this.VIDEO_HEIGHT / this.VIDEO_WIDTH);
		} else {
			width = (int) (height * this.VIDEO_WIDTH / this.VIDEO_HEIGHT); //debug
		}
		LayoutParams layoutParams = new WindowManager.LayoutParams(
				// WindowManager.LayoutParams.WRAP_CONTENT,
				// WindowManager.LayoutParams.WRAP_CONTENT,
				width, height, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		if (width == 1) {
			layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		} else {
			layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
		}
		windowManager.updateViewLayout(surfaceView, layoutParams);
		if (width > 1) {
			mTextView.setVisibility(TextView.VISIBLE);
			mSpeedView.setVisibility(TextView.VISIBLE);
			mBatteryView.setVisibility(TextView.VISIBLE);
		} else {
			mTextView.setVisibility(TextView.INVISIBLE);
			mSpeedView.setVisibility(TextView.INVISIBLE);
			mBatteryView.setVisibility(TextView.INVISIBLE);
		}
	}

	// Stop isrecording and remove SurfaceView
	@Override
	public void onDestroy() {
		StopRecording();

		stopGps();
		windowManager.removeView(surfaceView);
		windowManager.removeView(mTextView);
		windowManager.removeView(mSpeedView);
		windowManager.removeView(mBatteryView);
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
			int width, int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
			this.RestartRecording();
		}
	}

	public void onLocationChanged(Location location) {
		// Doing something with the position...
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	private void startGps() {
                if (!USEGPS) return;
		if (mLocationManager == null)
			mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (mLocationManager != null) {
			try {
				mLocationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 250 /* ms */, 0 /* m */,
						(LocationListener) this); // mintime,mindistance
				// if ( !mLocationManager.isProviderEnabled(
				// LocationManager.GPS_PROVIDER ) )
				// Toast.makeText(getApplicationContext(),
				// getString(R.string.gps_disabled), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				Log.e("CosyDVR", "exception: " + e.getMessage());
				Log.e("CosyDVR", "exception: " + e.toString());
			}
		}
	}

	private void stopGps() {
		if (mLocationManager != null) {
			mLocationManager.removeUpdates((LocationListener) this);
		}
		mLocationManager = null;
	}

	private int getBatteryLevel(Context context) {
		int batteryLevel = 0;
	    try {
	        IntentFilter ifilter = new IntentFilter(
	                Intent.ACTION_BATTERY_CHANGED);
	        Intent batteryStatus = context.registerReceiver(null, ifilter);
	        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	        if (status == BatteryManager.BATTERY_STATUS_CHARGING
	        		|| status == BatteryManager.BATTERY_STATUS_FULL) {
	        	return -1;
	        }

	        batteryLevel = (int) (100 * level / (float) scale);

	        if (batteryLevel < 0) {
	            batteryLevel = 0;

	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return batteryLevel;
	}
}
