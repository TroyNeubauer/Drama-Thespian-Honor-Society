package com.troy.dramaserver.net;

import java.io.*;
import java.util.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.logging.log4j.*;

import com.google.gson.*;

public class MultiPartStringParser implements org.apache.commons.fileupload.UploadContext {

	private static final Logger logger = LogManager.getLogger(MultiPartStringParser.class);

	private String postBody;
	private String boundary;

	public MultiPartStringParser(String postBody, String boundary) {
		super();
		this.postBody = postBody;
		this.boundary = boundary;
	}

	public static JsonObject parse(String postBody) throws Exception {
		if (postBody.isEmpty())
			return null;
		if (postBody.charAt(0) == '{') {// Json
			JsonElement element = new JsonParser().parse(postBody);
			if (element.isJsonObject())
				return element.getAsJsonObject();
			if (element.isJsonNull())
				return new JsonObject();
			logger.warn("Root Json element is not an object! " + element.getClass() + " | " + element.toString());
			return new JsonObject();
		} else {// Normal thing

			// Sniff out the multpart boundary.
			String boundary = postBody.substring(2, postBody.indexOf('\n')).trim();
			JsonObject parameters = new JsonObject();
			// Parse out the parameters.
			final FileItemFactory factory = new DiskFileItemFactory();
			FileUpload upload = new FileUpload(factory);
			List<FileItem> fileItems = upload.parseRequest(new MultiPartStringParser(postBody, boundary));
			for (FileItem fileItem : fileItems) {
				if (fileItem.isFormField()) {
					parameters.addProperty(fileItem.getFieldName(), fileItem.getString());
				} // else it is an uploaded file
			}

			return parameters;
		}
	}

	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public int getContentLength() {
		return postBody.length();
	}

	@Override
	public String getContentType() {
		return "multipart/form-data, boundary=" + this.boundary;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(postBody.getBytes());
	}

	@Override
	public long contentLength() {
		return postBody.length();
	}
}