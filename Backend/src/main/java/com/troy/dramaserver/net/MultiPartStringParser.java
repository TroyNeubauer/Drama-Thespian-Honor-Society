package com.troy.dramaserver.net;

import java.io.*;
import java.util.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

public class MultiPartStringParser implements org.apache.commons.fileupload.UploadContext {

	private String postBody;
	private String boundary;
	private HashMap<String, String> parameters = new HashMap<String, String>();

	public MultiPartStringParser(String postBody) throws Exception {
		this.postBody = postBody;
		// Sniff out the multpart boundary.
		this.boundary = postBody.substring(2, postBody.indexOf('\n')).trim();
		// Parse out the parameters.
		final FileItemFactory factory = new DiskFileItemFactory();
		FileUpload upload = new FileUpload(factory);
		List<FileItem> fileItems = upload.parseRequest(this);
		for (FileItem fileItem : fileItems) {
			if (fileItem.isFormField()) {
				parameters.put(fileItem.getFieldName(), fileItem.getString());
			} // else it is an uploaded file
		}
	}

	public HashMap<String, String> getParameters() {
		return parameters;
	}

	// The methods below here are to implement the UploadContext interface.
	@Override
	public String getCharacterEncoding() {
		return "UTF-8"; // You should know the actual encoding.
	}

	// This is the deprecated method from RequestContext that unnecessarily
	// limits the length of the content to ~2GB by returning an int.
	@Override
	public int getContentLength() {
		return -1; // Don't use this
	}

	@Override
	public String getContentType() {
		// Use the boundary that was sniffed out above.
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