package com.marklogic.client.ext.modulesloader.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.file.DocumentFile;
import com.marklogic.client.ext.modulesloader.ModulesManager;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class DatabaseTimestampModulesManager implements ModulesManager {

	private DatabaseClient databaseClient;
	private ObjectNode uriToTimestampMapping;

	private String initializationScript;

	public DatabaseTimestampModulesManager(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	/**
	 * TODO On initialize, get a data structure from ML with every URI in the modules database and its corresponding
	 * document timestamp.
	 */
	@Override
	public void initialize() {
		if (uriToTimestampMapping == null) {
			if (initializationScript == null) {
				initializationScript = "const map = {};\n" +
					"cts.uris(null, null, cts.trueQuery()).toArray().forEach(uri => {\n" +
					"  map[uri.toString().toLowerCase()] = fn.adjustDateTimeToTimezone(\n" +
					"    xdmp.timestampToWallclock(xdmp.documentTimestamp(uri)),\n" +
					"    xs.dayTimeDuration(\"PT0H\")\n" +
					"  );\n" +
					"});\n" +
					"map";
			}

			uriToTimestampMapping = databaseClient.newServerEval().javascript(initializationScript).evalAs(ObjectNode.class);
		}
	}

	@Override
	public boolean hasFileBeenModifiedSinceLastLoaded(File file) {
		throw new UnsupportedOperationException("No way to support this yet because we don't know how to map any given File to a URI");
	}

	@Override
	public void saveLastLoadedTimestamp(File file, Date date) {
		throw new UnsupportedOperationException("No way to support this yet because we don't know how to map any given File to a URI");
	}

	@Override
	public boolean hasDocumentFileBeenModifiedSinceLastLoaded(DocumentFile documentFile) {
		String uri = documentFile.getUri();
		if (uri != null && uriToTimestampMapping.has(uri)) {
			String timestamp = uriToTimestampMapping.get(uri).asText();
			try {
				Date mlDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'").parse(timestamp);

				long lastModified = documentFile.getFile().lastModified();
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(lastModified);
				cal.setTimeZone(TimeZone.getTimeZone("Zulu"));
				Date fileDate = cal.getTime();

				System.out.println(mlDate + " : " + fileDate);
				return fileDate.after(mlDate);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		// If there's no record of the URI, consider it to be new, and thus needs loading
		return true;
	}

	@Override
	public void saveLastLoadedTimestamp(DocumentFile documentFile, Date date) {
		// No need to do anything here!
	}

	public void setInitializationScript(String initializationScript) {
		this.initializationScript = initializationScript;
	}
}
