package org.bitman.demeter;

import java.util.Locale;

import org.bitman.streaming.SessionBuilder;
import org.bitman.streaming.video.VideoQuality;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;

/**
 * DemeterApplication，当application一启动时就会启用这个class，
 * 主要用于读取preferences和启动DemeterActivity
 * @author ay27
 *
 */

public class DemeterApplication extends android.app.Application {
	
	private static DemeterApplication instance;

	public final static String TAG = "DemeterApplication";
	
	/** Default quality of video streams. */
	public VideoQuality videoQuality = new VideoQuality(640,480,15,500000);

	/** By default H.264 is the video encoder. */
	public int videoEncoder = SessionBuilder.VIDEO_H264;

	/** The HttpServer will use those variables to send reports about the state of the app to the web interface. */
	public boolean applicationForeground = true;
	public Exception lastCaughtException = null;

	/** Contains an approximation of the battery level. */
	public int batteryLevel = 0;
	
	private static DemeterApplication sApplication;

	@Override
	public void onCreate() {

		sApplication = this;

		super.onCreate();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		videoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(videoEncoder)));

		// Read video quality settings from the preferences 
		videoQuality = VideoQuality.merge(
				new VideoQuality(
						settings.getInt("video_resX", 640),
						settings.getInt("video_resY", 480), 
						Integer.parseInt(settings.getString("video_framerate", "20")), 
						Integer.parseInt(settings.getString("video_bitrate", "1000"))*1000),
						videoQuality);

		SessionBuilder.getInstance() 
		.setContext(getApplicationContext())
		.setVideoEncoder(videoEncoder)
		.setVideoQuality(videoQuality);

		// Listens to changes of preferences
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

		registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		
        Locale.setDefault(Locale.CHINA);
        Configuration config = new Configuration();
        config.locale = Locale.CHINA;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        instance = this;
	}

	public static DemeterApplication getInstance() {
		return sApplication;
	}

	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("video_resX") || key.equals("video_resY")) {
				videoQuality.resX = sharedPreferences.getInt("video_resX", 640);
				videoQuality.resY = sharedPreferences.getInt("video_resY", 480);
			}

			else if (key.equals("video_framerate")) {
				videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "15"));
			}

			else if (key.equals("video_bitrate")) {
				videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "300"))*1000;
			}

			else if (key.equals("video_encoder")) {
				videoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", String.valueOf(videoEncoder)));
				SessionBuilder.getInstance().setVideoEncoder( videoEncoder );
			}

		}  
	};

	private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			batteryLevel = intent.getIntExtra("level", 0);
		}
	};
	
    /**
     * @return the main context of the Application
     */
    public static Context getAppContext()
    {
        return instance;
    }

    /**
     * @return the main resources from the Application
     */
    public static Resources getAppResources()
    {
        return instance.getResources();
    }

}
