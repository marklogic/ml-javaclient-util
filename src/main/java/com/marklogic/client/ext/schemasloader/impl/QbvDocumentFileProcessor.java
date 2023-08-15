package com.marklogic.client.ext.schemasloader.impl;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.file.DocumentFile;
import com.marklogic.client.ext.file.DocumentFileProcessor;
import com.marklogic.client.ext.helper.LoggingObject;

import java.util.ArrayList;
import java.util.List;

public class QbvDocumentFileProcessor extends LoggingObject implements DocumentFileProcessor {

	private final DatabaseClient schemasDatabaseClient;
	private List<DocumentFile> qbvFiles;

	public QbvDocumentFileProcessor(DatabaseClient schemasDatabaseClient) {
		this.schemasDatabaseClient = schemasDatabaseClient;
		this.qbvFiles = new ArrayList<>();
	}

	@Override
	public DocumentFile processDocumentFile(DocumentFile documentFile) {
		String uri = documentFile.getUri();
		if (uri != null && uri.startsWith("/qbv/")) {
			qbvFiles.add(documentFile);
			return null;
		}
		return documentFile;
	}

	public void processQbvFiles() {
		qbvFiles.forEach(qbvFile -> {
			// TODO Process each file
		});
		qbvFiles.clear();
	}
}
