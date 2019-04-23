package com.marklogic.client.ext.modulesloader.impl;

import com.marklogic.client.ext.file.DocumentFile;
import com.marklogic.client.ext.file.DocumentFileProcessor;
import com.marklogic.client.ext.modulesloader.ModulesManager;

import java.util.Date;

public class ModulesManagerDocumentFileProcessor implements DocumentFileProcessor {

	private ModulesManager modulesManager;

	public ModulesManagerDocumentFileProcessor(ModulesManager modulesManager) {
		this.modulesManager = modulesManager;
	}

	@Override
	public DocumentFile processDocumentFile(DocumentFile documentFile) {
		if (documentFile != null) {
			if (!modulesManager.hasDocumentFileBeenModifiedSinceLastLoaded(documentFile)) {
				return null;
			}
			modulesManager.saveLastLoadedTimestamp(documentFile, new Date());
		}
		return documentFile;
	}
}
