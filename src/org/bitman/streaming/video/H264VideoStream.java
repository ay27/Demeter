package org.bitman.streaming.video;

import java.io.IOException;

import org.bitman.streaming.MediaStream;
import org.bitman.streaming.rtp.H264Packetizer;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;


public class H264VideoStream extends MediaStream {

	private final static String TAG = "H264VideoStream";

	private VideoQuality mQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
	private SurfaceHolder.Callback mSurfaceHolderCallback = null;
	private SurfaceHolder mSurfaceHolder = null;
	private int mVideoEncoder, mCameraId = 0;
	private Camera mCamera;
	private boolean mCameraOpenedManually = true;
	private boolean mSurfaceReady = false;
	private boolean mUnlocked = false;
	private SharedPreferences mSettings = null;

	/** 
	 * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 */
	public H264VideoStream(int camera) {
		super();
		setCamera(camera);
		mVideoEncoder = MediaRecorder.VideoEncoder.H264;
		try {
			mPacketizer = new H264Packetizer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets a Surface to show a preview of recorded media (video). 
	 * You can call this method at any time and changes will take effect next time you call {@link #start()}.
	 */
	public synchronized void setPreviewDisplay(SurfaceHolder surfaceHolder) {
		if (mSurfaceHolderCallback != null && mSurfaceHolder != null) {
			mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
		}
		if (surfaceHolder != null) {
			mSurfaceHolderCallback = new Callback() {
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					mSurfaceReady = false;
					if (H264VideoStream.this.mStreaming) {
						H264VideoStream.this.stop();
						Log.d(TAG,"Surface destroyed: video streaming stopped.");
					}
					if (mCamera != null) stopPreview();
				}
				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					mSurfaceReady = true;
				}
				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
					Log.d(TAG,"Surface Changed !");
				}
			};
			mSurfaceHolder = surfaceHolder;
			mSurfaceHolder.addCallback(mSurfaceHolderCallback);
			mSurfaceReady = true;
		}
	}

	/** 
	 * Modifies the quality of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * @param videoQuality Quality of the stream
	 */
	public void setVideoQuality(VideoQuality videoQuality) {
		mQuality = videoQuality;
	}
	
	/** 
	 * Returns the quality of the stream.  
	 */
	public VideoQuality getVideoQuality() {
		return mQuality;
	}	


	/** Stops the stream. */
	public synchronized void stop() {
		super.stop();
		lockCamera();
		if (!mCameraOpenedManually) {
			stopPreview();
		}
		mCameraOpenedManually = true;
	}
	
	/**
	 * Some data (SPS and PPS params) needs to be stored when {@link #generateSessionDescription()} is called 
	 * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
	 */
	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
		Log.i(TAG, "mSettings is setted!");
	}

	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 * Will fail if called when streaming.
	 */
	public synchronized  String generateSessionDescription() throws IllegalStateException, IOException {
		String[] s = mSettings.getString("h264"+mQuality.framerate+","+mQuality.resX+","+mQuality.resY, "").split(",");
		Log.i(TAG, "s.length = "+s.length);
		return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
		"a=rtpmap:96 H264/90000\r\n" +
		"a=fmtp:96 packetization-mode=1;profile-level-id="+s[0]+";sprop-parameter-sets="+s[1]+","+s[2]+";\r\n";
	}
	
	
	
	/**
	 * Encoding of the audio/video is done by a MediaRecorder.
	 */
	protected void encodeWithMediaRecorder() throws IOException {

		// We need a local socket to forward data output by the camera to the packetizer
		createSockets();

		// Opens the camera if needed
		if (mCamera == null) {
			mCameraOpenedManually = false;
			// Will start the preview if not already started !
			Log.d(TAG,"Preview must be started to record video !");
			startPreview();
		}

		unlockCamera();
	
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX,mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);

		mMediaRecorder.setOutputFile(mSender.getFileDescriptor());

		mMediaRecorder.prepare();
		mMediaRecorder.start();

		try {
			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setInputStream(mReceiver.getInputStream());
			mPacketizer.start();
			mStreaming = true;
		} catch (Exception e) {
			stop();
			throw new IOException("Something happened with the local sockets :/ Start failed !");
		}

	}
	
	

	private synchronized void startPreview() throws RuntimeException, IOException {

		if (mSurfaceHolder == null || mSurfaceHolder.getSurface() == null || !mSurfaceReady)
			throw new IllegalStateException("Invalid surface holder !");

		if (mCamera == null) {
			mCamera = Camera.open(mCameraId);
			mCamera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					// On some phones when trying to use the camera facing front the media server will die
					// Whether or not this callback may be called really depends on the phone
					if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
						// In this case the application must release the camera and instantiate a new one
						Log.e(TAG,"Media server died !");
						// We don't know in what thread we are so stop needs to be synchronized
						mCameraOpenedManually = false;
						stop();
					} else {
						Log.e(TAG,"Error unknown with the camera: "+error);
					}	
				}
			});

			Parameters parameters = mCamera.getParameters();

			try {
			mCamera.setParameters(parameters);
			mCamera.setDisplayOrientation(mQuality.orientation);
			mCamera.setPreviewDisplay(mSurfaceHolder);
			if (mCameraOpenedManually) mCamera.startPreview();
			
			} catch (RuntimeException e) {
				stopPreview();
				throw e;
			} catch (IOException e) {
				stopPreview();
				throw e;
			}

		}

	}

	private synchronized void stopPreview() {

		if (mStreaming) super.stop();

		if (mCamera != null) {
			lockCamera();
			if (!mCameraOpenedManually) mCamera.stopPreview();
			try {
				mCamera.release();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
			}
			mCamera = null;
			mUnlocked = false;
		}		
	}
	
	/**
	 * Sets the camera that will be used to capture video.
	 * You can call this method at any time and changes will take effect next time you start the stream.
	 * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 */
	private void setCamera(int camera) {
		CameraInfo cameraInfo = new CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i=0;i<numberOfCameras;i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == camera) {
				this.mCameraId = i;
				break;
			}
		}
	}
	
	private void lockCamera() {
		if (mUnlocked) {
			try {
				mCamera.reconnect();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage());
			}
			mUnlocked = false;
		}
	}

	private void unlockCamera() {
		if (!mUnlocked) {
			try {	
				mCamera.unlock();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage());
			}
			mUnlocked = true;
		}
	}
	
}
