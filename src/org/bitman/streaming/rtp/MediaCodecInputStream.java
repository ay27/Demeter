package org.bitman.streaming.rtp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.util.Log;

/**
 * An InputStream that uses data from a MediaCodec.
 * The purpose of this class is to interface existing RTP packetizers of
 * libstreaming with the new MediaCodec API. This class is not thread safe !  
 */
@SuppressLint("NewApi")
public class MediaCodecInputStream extends InputStream {

	public final String TAG = "MediaCodecInputStream"; 
	
	private MediaCodec mMediaCodec = null;
	private BufferInfo mBufferInfo = new BufferInfo();
	private ByteBuffer[] mBuffers = null;
	private ByteBuffer mBuffer = null;
	private int mIndex;
	private boolean mClosed = false;
	
	public MediaCodecInputStream(MediaCodec mediaCodec) {
		mMediaCodec = mediaCodec;
		mBuffers = mMediaCodec.getOutputBuffers();
	}
	
	@Override
	public void close() {
		mClosed = true;
	}
	
	@Override
	public int read() throws IOException {
		return 0;
	}
	
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int min;
		
		if (mClosed) throw new IOException("This InputStream was closed");
		
		if (mBuffer==null || mBufferInfo.size-mBuffer.position() <= 0) {
			mIndex = -1;
			while (!Thread.interrupted() && mIndex<0) {
				mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 20000);
				if (mIndex>=0 ){
					//Log.d(TAG,"Index: "+mIndex+" Time: "+mBufferInfo.presentationTimeUs+" size: "+mBufferInfo.size);
					mBuffer = mBuffers[mIndex];
					mBuffer.position(0);
					mMediaCodec.releaseOutputBuffer(mIndex, false);
				} else if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					mBuffers = mMediaCodec.getOutputBuffers();
				} else if (mIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
					Log.e(TAG,"BOG: "+mIndex);
				}
			}			
		}

		min = length < mBufferInfo.size - mBuffer.position() ? length : mBufferInfo.size - mBuffer.position(); 
		
		mBuffer.get(buffer, offset, min);
		
		return min;
		
	}

	public int available() {
		if (mBuffer != null) return mBufferInfo.size - mBuffer.position();
		else return 0;
	}
	
	public BufferInfo getLastBufferInfo() {
		return mBufferInfo;
	}
	
}
