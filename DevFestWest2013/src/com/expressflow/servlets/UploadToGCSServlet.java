package com.expressflow.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;

public class UploadToGCSServlet extends HttpServlet {

	private static final long serialVersionUID = 2709050671747436824L;

	private static final String GCS_PROPS_FILE_PATH = "/WEB-INF/gcs_settings.properties";

	private static final Logger log = Logger.getLogger(UploadToGCSServlet.class
			.getName());

	/**
	 * This is where backoff parameters are configured. Here it is aggressively
	 * retrying with backoff, up to 10 times but taking no more that 15 seconds
	 * total to do so.
	 */
	private final GcsService gcsService = GcsServiceFactory
			.createGcsService(new RetryParams.Builder()
					.initialRetryDelayMillis(10).retryMaxAttempts(10)
					.totalRetryPeriodMillis(15000).build());

	/**
	 * Used below to determine the size of chucks to read in. Should be > 1kb
	 * and < 10MB
	 */
	private static final int BUFFER_SIZE = 2 * 1024 * 1024;

	/**
	 * Default HTTP GET handle
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Sorry, just serving HTTP POSTs.");
	}

	/**
	 * HTTP POST serving function: Here all /upload requests come in.
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String filename = req.getParameter("filename");
		log.info("Received filename: " + filename);
		GcsOutputChannel outputChannel = gcsService.createOrReplace(
				getGCSFile(req, filename), GcsFileOptions.getDefaultInstance());
		copy(req.getInputStream(), Channels.newOutputStream(outputChannel));
	}

	private GcsFilename getGCSFile(HttpServletRequest req, String filename) throws IOException {
		// Load the bucketname from Properties
		Properties props = new Properties();
		props.load(getGCSPropsStream());
		String bucketName = props.getProperty("bucket");
		if(filename == null){
			Key key = KeyFactory.createKey("unique_id", new Date().getTime());
			filename = KeyFactory.keyToString(key);
		}
		
		return new GcsFilename(bucketName, filename);
	}

	/**
	 * Transfer the data from the inputStream to the outputStream. Then close
	 * both streams.
	 * 
	 * @throws IOException
	 */
	private void copy(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = input.read(buffer);
			while (bytesRead != -1) {
				output.write(buffer, 0, bytesRead);
				bytesRead = input.read(buffer);
			}
		} catch (Exception e) {
			log.warning(e.getMessage());
		} finally {
			input.close();
			output.close();
		}
	}
	
	protected InputStream getGCSPropsStream() {
		return getServletContext().getResourceAsStream(GCS_PROPS_FILE_PATH);
	}

}
