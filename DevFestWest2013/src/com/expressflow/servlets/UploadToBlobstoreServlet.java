package com.expressflow.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.expressflow.crypto.CryptUtils;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;

public class UploadToBlobstoreServlet extends HttpServlet {

	private static final long serialVersionUID = 2709050671747436824L;

	private static final Logger log = Logger
			.getLogger(UploadToBlobstoreServlet.class.getName());

	private static final String GCS_PROPS_FILE_PATH = "/WEB-INF/gcs_settings.properties";

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
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			// Retrieve the filename from the upload attributes "blobInfos"
			BlobstoreService blobstoreService = BlobstoreServiceFactory
					.getBlobstoreService();
			Map<String, List<BlobInfo>> blobs = blobstoreService
					.getBlobInfos(req);
			List<BlobInfo> list = blobs.get("file");
			BlobInfo blobInfo = list.get(0);
			String filename = blobInfo.getFilename();

			GcsFilename gcsFilename = getGCSFile(filename);

			GcsOutputChannel outputChannel = gcsService.createOrReplace(
					gcsFilename, GcsFileOptions.getDefaultInstance());
			copy(blobInfo.getBlobKey(), Channels.newOutputStream(outputChannel));

			// Encrypt it
			log.info("Starting file encryption.");
			Date start = new Date();

			try {

				GcsFilename encryptedFilename = new GcsFilename(
						gcsFilename.getBucketName(), filename + ".aes");
				GcsOutputChannel writeChannel = gcsService.createOrReplace(
						encryptedFilename, GcsFileOptions.getDefaultInstance());
				OutputStream encOut = Channels.newOutputStream(writeChannel);
				BlobstoreInputStream bis = new BlobstoreInputStream(
						blobInfo.getBlobKey());

				SecretKeySpec key = new SecretKeySpec(CryptUtils.getKeyBytes(),
						"AES");
				IvParameterSpec ivSpec = new IvParameterSpec(
						CryptUtils.getIVBytes());
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

				// Init the cipher

				cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

				CipherInputStream cIn = new CipherInputStream(bis, cipher);

				int ch;
				while ((ch = cIn.read()) >= 0) {
					encOut.write(ch);
				}

				cIn.close();
				encOut.close();

			} catch (Exception e) {
				log.warning(e.getMessage());
			} 

			Date end = new Date();
			long duration = end.getTime() - start.getTime();
			log.info("File encryption finished in " + duration + " ms.");

		} catch (JSONException je) {
			log.warning(je.getMessage());
		}
	}

	private GcsFilename getGCSFile(String filename) throws JSONException,
			IOException {
		// Load the bucketname from Properties
		Properties props = new Properties();
		props.load(getGCSPropsStream());
		String bucketName = props.getProperty("bucket");
		return new GcsFilename(bucketName, filename);
	}

	/**
	 * Transfer the data from the inputStream to the outputStream. Then close
	 * both streams.
	 * 
	 * @throws IOException
	 */
	private void copy(BlobKey blobKey, OutputStream output) throws IOException {
		BlobstoreInputStream bis = new BlobstoreInputStream(blobKey);
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = bis.read(buffer);
			while (bytesRead != -1) {
				output.write(buffer, 0, bytesRead);
				bytesRead = bis.read(buffer);
			}
		} catch (Exception e) {
			log.warning(e.getMessage());
		} finally {
			bis.close();
			output.close();
		}
	}

	protected InputStream getGCSPropsStream() {
		return getServletContext().getResourceAsStream(GCS_PROPS_FILE_PATH);
	}

}
