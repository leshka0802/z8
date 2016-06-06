package org.zenframework.z8.server.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.mozilla.universalchardet.UniversalDetector;
import org.zenframework.z8.server.engine.Z8Context;
import org.zenframework.z8.server.logs.Trace;
import org.zenframework.z8.server.types.encoding;

public class IOUtils {
	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

	final static public int DefaultBufferSize = 1024 * 1024;
	final static public String DefaultCharset = encoding.UTF8.toString();

	private IOUtils() {/* hide constructor */
	}

	static public void copy(InputStream input, File file) throws IOException {
		copy(input, file, true);
	}

	static public void copy(InputStream input, File file, boolean autoClose) throws IOException {
		try {
			file.getParentFile().mkdirs();
			copy(input, new FileOutputStream(file), autoClose);
		} finally {
			if(autoClose)
				closeQuietly(input);
		}
	}

	static public void copy(InputStream input, OutputStream output) throws IOException {
		copy(input, output, true);
	}

	static public void copy(InputStream input, OutputStream output, boolean autoClose) throws IOException {
		try {
			int count = 0;
			byte[] buffer = new byte[DefaultBufferSize];

			while((count = input.read(buffer)) != -1) {
				if(count > 0)
					output.write(buffer, 0, count);
			}

			if(autoClose) {
				input.close();
				output.close();
				autoClose = false;
			}
		} finally {
			if(autoClose) {
				closeQuietly(input);
				closeQuietly(output);
			}
		}
	}

	static public long copyLarge(InputStream input, OutputStream output) throws IOException {
		return copyLarge(input, output, true);
	}

    static public long copyLarge(InputStream input, OutputStream output, boolean autoClose) throws IOException {
    	try {
    		byte[] buffer = new byte[DefaultBufferSize];
	        long count = 0;
	        int read = 0;
	        while((read = input.read(buffer)) != -1) {
	            output.write(buffer, 0, read);
	            count += read;
	        }
			
	        if(autoClose) {
				input.close();
				output.close();
				autoClose = false;
			}

	        return count;
		} finally {
			if(autoClose) {
				closeQuietly(input);
				closeQuietly(output);
			}
		}
    }
	
	static public long copyLarge(InputStream input, OutputStream output, long length) throws IOException {
		return copyLarge(input, output, length, true);
	}

	static public long copyLarge(InputStream input, OutputStream output, long length, boolean autoClose) throws IOException {
		try {
			byte[] buffer = new byte[DefaultBufferSize];

			if(length == 0)
				return 0;

			final int bufferLength = buffer.length;

			int bytesToRead = bufferLength;
			if(length > 0 && length < bufferLength)
				bytesToRead = (int)length;

			int read;
			long totalRead = 0;

			while(bytesToRead > 0 && (read = input.read(buffer, 0, bytesToRead)) != -1) {
				output.write(buffer, 0, read);
				totalRead += read;
				if(length > 0)
					bytesToRead = (int)Math.min(length - totalRead, bufferLength);
			}

			if(autoClose) {
				input.close();
				output.close();
				autoClose = false;
			}

			return totalRead;
		} finally {
			if(autoClose) {
				closeQuietly(input);
				closeQuietly(output);
			}
		}
	}

	static public String readText(InputStream in) throws IOException {
		return readText(in, Charset.defaultCharset());
	}

	static public String readText(InputStream in, Charset charset) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(in, out);
		return new String(out.toByteArray(), charset);
	}

	static public String readText(URL resource) throws IOException {
		return readText(resource.openStream());
	}
	
	static public void closeQuietly(Closeable closable) {
		try {
			if(closable != null)
				closable.close();
		} catch(Throwable e) {
		}
	}

	static public byte[] zip(byte[] bytes) {
		if(bytes == null)
			return null;
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		Deflater deflater = new Deflater();
		deflater.setInput(bytes);
		deflater.finish();

		byte[] buffer = new byte[32768];
		while(!deflater.finished()) {
			int count = deflater.deflate(buffer);
			outputStream.write(buffer, 0, count);
		}

		return outputStream.toByteArray();
	}

	static public byte[] unzip(byte[] bytes) {
		if(bytes == null)
			return null;
		
		Inflater inflater = new Inflater();
		inflater.setInput(bytes);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[32768];

		while(!inflater.finished()) {
			try {
				int count = inflater.inflate(buffer);
				outputStream.write(buffer, 0, count);
			} catch(DataFormatException e) {
				throw new RuntimeException(e);
			}
		}

		return outputStream.toByteArray();
	}
	
	static public boolean moveFolder(File src, File dest, boolean trace) {
		try {
			dest.getParentFile().mkdirs();
			ProcessBuilder cmd;
			String charset;
			if(OS_NAME.startsWith("win")) {
				cmd = new ProcessBuilder("cmd", "/C", "move", "/Y", src.getAbsolutePath(), dest.getAbsolutePath());
				charset = "cp866";
			} else if(OS_NAME.startsWith("linux")) {
				cmd = new ProcessBuilder("mv", src.getAbsolutePath(), dest.getAbsolutePath());
				charset = "UTF-8";
			} else {
				if(trace) {
					Trace.logError("Move '" + src + "' to '" + dest + "' failed", new UnsupportedOperationException("Unsupported OS '" + OS_NAME + "'"));
				}
				return false;
			}
			Process proc = cmd.start();
			if(trace) {
				InputStream in = proc.getErrorStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				copy(in, out);
				Trace.logEvent("Moving '" + src + "' to '" + dest + "':\r\n" + new String(out.toByteArray(), charset));
			} else
				proc.waitFor();

			return dest.exists();
		} catch(Exception e) {
			Trace.logError("Move '" + src + "' to '" + dest + "' failed", e);
			return false;
		}
	}

	static public String determineEncoding(File file, String defaultCharset) {
		UniversalDetector detector = new UniversalDetector(null);
		// Reset detector before using
		detector.reset();
		// Buffer
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			byte[] buf = new byte[1024];
			for(int n = in.read(buf); n >= 0 && !detector.isDone(); n = in.read(buf)) {
				detector.handleData(buf, 0, n);
			}
			detector.dataEnd();
			return detector.getDetectedCharset() != null ? detector.getDetectedCharset() : defaultCharset;
		} catch(Exception e) {
			Trace.logError("Can't detect encoding of '" + file.getAbsolutePath() + "'", e);
			return defaultCharset;
		} finally {
			detector.reset();
			if(in != null) {
				try {
					in.close();
				} catch(IOException e) {
				}
			}
		}
	}
	
	public static String getRelativePath(File path) throws IOException {
		int baseLen = Z8Context.getWorkingPath().getCanonicalPath().length() + 1;
		return path.getCanonicalPath().substring(baseLen);
	}

}
