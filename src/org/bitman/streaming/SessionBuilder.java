package org.bitman.streaming;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.bitman.demeter.http.HttpServer;
import org.bitman.streaming.video.VideoQuality;
import org.bitman.streaming.video.H264VideoStream;

import android.R.integer;
import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Call {@link #getInstance()} to get access to the SessionBuilder.
 */
public class SessionBuilder {

	public final static String TAG = "SessionBuilder";

	/** Can be used with {@link #setVideoEncoder}. */
	public final static int VIDEO_NONE = 0;

	/** Can be used with {@link #setVideoEncoder}. */
	public final static int VIDEO_H264 = 1;

	// Default configuration
	private VideoQuality mVideoQuality = new VideoQuality();
	private Context mContext;
	private int mVideoEncoder = VIDEO_H264; 
	private int mCamera = CameraInfo.CAMERA_FACING_BACK;
	private int mTimeToLive = 64;
	private boolean mFlash = false;
	private SurfaceHolder mSurfaceHolder = null;
	private InetAddress mOrigin = null;
	private InetAddress mDestination = null;

	// Removes the default public constructor
	private SessionBuilder() {}

	// The SessionManager implements the singleton pattern
	private static volatile SessionBuilder sInstance = null; 

	/**
	 * Returns a reference to the {@link SessionBuilder}.
	 * @return The reference to the {@link SessionBuilder}
	 */
	public final static SessionBuilder getInstance() {
		if (sInstance == null) {
			synchronized (SessionBuilder.class) {
				if (sInstance == null) {
					SessionBuilder.sInstance = new SessionBuilder();
				}
			}
		}
		return sInstance;
	}	

	/**
	 * Creates a new {@link Session}.
	 * @return The new Session
	 * @throws IOException 
	 */
	public Session build() throws IOException {
		Session session;

		session = new Session();
		session.setContext(mContext);
		session.setOrigin(mOrigin);
		session.setDestination(mDestination);
		session.setTimeToLive(mTimeToLive);

		switch (mVideoEncoder) {

		case VIDEO_H264:
			H264VideoStream stream = new H264VideoStream(mCamera);
			if (mContext!=null) 
				stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
			session.addVideoTrack(stream);
			break;
		}

		if (session.getVideoTrack()!=null) {
			H264VideoStream video = session.getVideoTrack();
			video.setVideoQuality(VideoQuality.merge(mVideoQuality,video.getVideoQuality()));
			video.setPreviewDisplay(mSurfaceHolder);

			//video.setDestinationPorts(getUsefulPort());
			video.setDestinationPorts(0);
		}

		return session;

	}
	

	/** 
	 * Access to the context is needed for the H264Stream class to store some stuff in the SharedPreferences.
	 * Note that you should pass the Application context, not the context of an Activity.
	 **/
	public SessionBuilder setContext(Context context) {
		mContext = context;
		return this;
	}

	/** Sets the destination of the session. */
	public SessionBuilder setDestination(InetAddress destination) {
		mDestination = destination;
		return this; 
	}

	/** Sets the origin of the session. It appears in the SDP of the session. */
	public SessionBuilder setOrigin(InetAddress origin) {
		mOrigin = origin;
		return this;
	}

	/** Sets the video stream quality. */
	public SessionBuilder setVideoQuality(VideoQuality quality) {
		mVideoQuality = VideoQuality.merge(quality, mVideoQuality);
		return this;
	}

	/** Sets the default video encoder. */
	public SessionBuilder setVideoEncoder(int encoder) {
		mVideoEncoder = encoder;
		return this;
	}

	public SessionBuilder setFlashEnabled(boolean enabled) {
		mFlash = enabled;
		return this;
	}

	public SessionBuilder setCamera(int camera) {
		mCamera = camera;
		return this;
	}

	public SessionBuilder setTimeToLive(int ttl) {
		mTimeToLive = ttl;
		return this;
	}

	/** 
	 * Sets the Surface required by MediaRecorder to record video. 
	 * @param surfaceHolder A SurfaceHolder wrapping a valid surface
	 **/
	public SessionBuilder setSurfaceHolder(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		return this;
	}

	/** Returns the context set with {@link #setContext(Context)}*/
	public Context getContext() {
		return mContext;	
	}

	/** Returns the destination ip address set with {@link #setDestination(InetAddress)}. */
	public InetAddress getDestination() {
		return mDestination;
	}

	/** Returns the origin ip address set with {@link #setOrigin(InetAddress)}. */
	public InetAddress getOrigin() {
		return mOrigin;
	}

	/** Returns the id of the {@link android.hardware.Camera} set with {@link #setCamera(int)}. */
	public int getCamera() {
		return mCamera;
	}

	/** Returns the video encoder set with {@link #setVideoEncoder(int)}. */
	public int getVideoEncoder() {
		return mVideoEncoder;
	}

	/** Returns the VideoQuality set with {@link #setVideoQuality(VideoQuality)}. */
	public VideoQuality getVideoQuality() {
		return mVideoQuality;
	}

	/** Returns the flash state set with {@link #setFlashEnabled(boolean)}. */
	public boolean getFlashState() {
		return mFlash;
	}

	/** Returns the SurfaceHolder set with {@link #setSurfaceHolder(SurfaceHolder)}. */
	public SurfaceHolder getSurfaceHolder() {
		return mSurfaceHolder;
	}

	/** Returns the time to live set with {@link #setTimeToLive(int)}. */
	public int getTimeToLive() {
		return mTimeToLive;
	}

	/** Returns a new {@link SessionBuilder} with the same configuration. */
	public SessionBuilder clone() {
		return new SessionBuilder()
		.setDestination(mDestination)
		.setOrigin(mOrigin)
		.setSurfaceHolder(mSurfaceHolder)
		.setVideoQuality(mVideoQuality)
		.setVideoEncoder(mVideoEncoder)
		.setFlashEnabled(mFlash)
		.setCamera(mCamera)
		.setTimeToLive(mTimeToLive)
		.setContext(mContext);
	}

}