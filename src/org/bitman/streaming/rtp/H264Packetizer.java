package org.bitman.streaming.rtp;

import java.io.IOException;

import android.R.integer;
import android.util.Log;

/**
 * 
 *   RFC 3984.
 *   
 *   H.264 streaming over RTP.
 *   
 *   Must be fed with an InputStream containing H.264 NAL units preceded by their length (4 bytes).
 *   The stream must start with mpeg4 or 3gpp header, it will be skipped.
 *   
 */
public class H264Packetizer extends AbstractPacketizer implements Runnable {

	public final static String TAG = "H264Packetizer";

	private final static int MAXPACKETSIZE = 1400;

	private Thread t = null;
	private int naluLength = 0;
	private long delay = 0, oldtime = 0;
	private Statistics stats = new Statistics();

	public H264Packetizer() throws IOException {
		super();
		socket.setClockFrequency(90000);
	}

	public void start() throws IOException {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {
		if (t != null) {
			t.interrupt();
			try {
				t.join(1000);
			} catch (InterruptedException e) {}
			t = null;
		}
	}

	public void run() {
		long duration = 0;
		Log.d(TAG,"H264 packetizer started !");
		stats.reset();
		// This will skip the MPEG4 header if this step fails we can't stream anything :(
		try {
			byte buffer[] = new byte[4];
			// Skip all atoms preceding mdat atom
			while (!Thread.interrupted()) {
				while (is.read() != 'm');
				is.read(buffer,0,3);
				if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
			}
		} catch (IOException e) {
			Log.e(TAG,"Couldn't skip mp4 header :/");
			return;
		}

		// We read a NAL units from the input stream and we send them
		try {
			while (!Thread.interrupted()) {

				// We measure how long it takes to receive the NAL unit from the phone
				oldtime = System.nanoTime();
				send();
				duration = System.nanoTime() - oldtime;

				delta += duration/1000000;
				if (intervalBetweenReports>0) {
					if (delta>=intervalBetweenReports) {
						// We send a Sender Report
						report.send(oldtime+duration,ts*90/1000000);
					}
				}

				stats.push(duration);
				// Computes the average duration of a NAL unit
				delay = stats.average();
				Log.i(TAG,"duration: "+duration/1000000+" delay: "+delay/1000000);

			}
		} catch (IOException e) {
		} catch (InterruptedException e) {}

		Log.d(TAG,"H264 packetizer stopped !");

	}

	/**
	 * Reads a NAL unit in the FIFO and sends it.
	 * If it is too big, we split it in FU-A units (RFC 3984).
	 */
	private byte[] header;
	private void send() throws IOException, InterruptedException {
		
		int sum = 1, len = 0, type;
		header = new byte[5];

		// Read NAL unit length (4 bytes) and NAL unit header (1 byte)
		fill(header,0,5);
		naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
		//naluLength = is.available();

		if (naluLength>100000 || naluLength<0) resync();

		// Parses the NAL unit type
		type = header[4]&0x1F;

		// Updates the timestamp
		ts += delay;

		Log.i(TAG, Long.toString(ts));
		Log.i(TAG,"- Nal unit length: " + naluLength + " delay: "+delay/1000000+" type: "+type);

		// Small NAL unit => Single NAL unit 
		if (naluLength<=MAXPACKETSIZE-rtphl-2) {
			buffer = socket.requestBuffer();
			buffer[rtphl] = header[4];
			len = fill(buffer, rtphl+1,  naluLength-1);
			socket.updateTimestamp(ts);
			socket.markNextPacket();
			super.send(naluLength+rtphl);
			Log.i(TAG,"----- Single NAL unit - len:"+len+" delay: "+delay);
		}
		// Large NAL unit => Split nal unit 
		else {

			// Set FU-A header
			header[1] = (byte) (header[4] & 0x1F);  // FU header type
			header[1] += 0x80; // Start bit
			// Set FU-A indicator
			header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
			// 原来是+，现在是|
			header[0] |= 28;

			while (sum < naluLength) {
				buffer = socket.requestBuffer();
				buffer[rtphl] = header[0];
				buffer[rtphl+1] = header[1];
				socket.updateTimestamp(ts);
				if ((len = fill(buffer, rtphl+2,  naluLength-sum > MAXPACKETSIZE-rtphl-2 ? MAXPACKETSIZE-rtphl-2 : naluLength-sum  ))<0) return; sum += len;
				// Last packet before next NAL
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl+1] += 0x40;
					socket.markNextPacket();
				}
				super.send(len+rtphl+2);
				// Switch start bit
				header[1] = (byte) (header[1] & 0x7F); 
				Log.i(TAG,"----- FU-A unit, sum:"+sum);
			}
		}
	}

	private int fill(byte[] buffer, int offset,int length) throws IOException {
		int sum = 0, len;

		while (sum<length) {
			len = is.read(buffer, offset+sum, length-sum);
			if (len<0) {
				throw new IOException("End of stream");
			}
			else sum+=len;
		}

		return sum;

	}

	private void resync() throws IOException {
		byte[] header = new byte[5];
		int type;

		Log.e(TAG,"Packetizer out of sync ! Let's try to fix that...");
		
		while (true) {

			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) is.read();

			type = header[4]&0x1F;

			if (type == 5 || type == 1) {
				naluLength = header[3]&0xFF | (header[2]&0xFF)<<8 | (header[1]&0xFF)<<16 | (header[0]&0xFF)<<24;
				if (naluLength>0 && naluLength<100000) {
					oldtime = System.nanoTime();
					Log.e(TAG,"A NAL unit may have been found in the bit stream !");
					break;
				}
			}

		}

	}

}