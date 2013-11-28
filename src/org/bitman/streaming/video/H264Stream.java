package org.bitman.streaming.video;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.bitman.streaming.rtp.H264Packetizer;

import android.content.SharedPreferences;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;

/**
 * A class for streaming H.264 from the camera of an android device using RTP. 
 * Call {@link #setDestinationAddress(java.net.InetAddress)}, {@link #setDestinationPorts(int)}, 
 * {@link #setVideoSize(int, int)}, {@link #setVideoFramerate(int)} and {@link #setVideoEncodingBitrate(int)} and you're good to go.
 * You can then call {@link #start()}.
 * Call {@link #stop()} to stop the stream.
 */
public class H264Stream extends VideoStream {

	private SharedPreferences mSettings = null;

	/**
	 * Constructs the H.264 stream.
	 * Uses CAMERA_FACING_BACK by default.
	 * @throws IOException
	 */
	public H264Stream() throws IOException {
		this(CameraInfo.CAMERA_FACING_BACK);
	}

	/**
	 * Constructs the H.264 stream.
	 * @param cameraId Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 * @throws IOException
	 */
	public H264Stream(int cameraId) throws IOException {
		super(cameraId);
		setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mPacketizer = new H264Packetizer();
	}

	/**
	 * Some data (SPS and PPS params) needs to be stored when {@link #generateSessionDescription()} is called 
	 * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
	 */
	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
	}

	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 * Will fail if called when streaming.
	 */
	public synchronized  String generateSessionDescription() throws IllegalStateException, IOException {
		String[] s = mSettings.getString("h264"+mQuality.framerate+","+mQuality.resX+","+mQuality.resY, "").split(",");
		
		return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
		"a=rtpmap:96 H264/90000\r\n" +
		"a=fmtp:96 packetization-mode=1;profile-level-id="+s[0]+";sprop-parameter-sets="+s[1]+","+s[2]+";\r\n";
	}


}
