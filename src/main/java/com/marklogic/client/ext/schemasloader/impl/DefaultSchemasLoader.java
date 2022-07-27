package com.marklogic.client.ext.schemasloader.impl;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.ext.batch.BatchWriter;
import com.marklogic.client.ext.batch.RestBatchWriter;
import com.marklogic.client.ext.file.DocumentFile;
import com.marklogic.client.ext.file.GenericFileLoader;
import com.marklogic.client.ext.helper.ClientHelper;
import com.marklogic.client.ext.modulesloader.impl.DefaultFileFilter;
import com.marklogic.client.ext.schemasloader.SchemasLoader;
import com.marklogic.client.io.DocumentMetadataHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultSchemasLoader extends GenericFileLoader implements SchemasLoader {

	private DatabaseClient schemasDatabaseClient;
	private String tdeValidationDatabase;

	/**
	 * Simplest constructor for using this class. Just provide a DatabaseClient, and this will use sensible defaults for
	 * how documents are read and written. Note that the DatabaseClient will not be released after this class is done
	 * with it, as this class wasn't the one that created it.
	 *
	 * @param schemasDatabaseClient
	 */
	public DefaultSchemasLoader(DatabaseClient schemasDatabaseClient) {
		this(schemasDatabaseClient, null);
	}

	/**
	 * If you want to validate TDE templates before they're loaded, you need to provide a second DatabaseClient that
	 * connects to the content database associated with the schemas database that schemas will be loaded into. This is
	 * because the "tde.validate" function must run against the content database.
	 *
	 * @param schemasDatabaseClient
	 * @param tdeValidationDatabase
	 */
	public DefaultSchemasLoader(DatabaseClient schemasDatabaseClient, String tdeValidationDatabase) {
		super(((Supplier<BatchWriter>) () -> {
			RestBatchWriter writer = new RestBatchWriter(schemasDatabaseClient);
			// Default this to 1, as it's not typical to have such a large number of schemas to load that multiple threads
			// are needed. This also ensures that if an error occurs when loading a schema, it's thrown to the client.
			writer.setThreadCount(1);
			writer.setReleaseDatabaseClients(false);
			return writer;
		}).get());

		this.schemasDatabaseClient = schemasDatabaseClient;
		this.tdeValidationDatabase = tdeValidationDatabase;
		initializeDefaultSchemasLoader();
	}

	/**
	 * Assumes that the BatchWriter has already been initialized.
	 *
	 * @param batchWriter
	 */
	public DefaultSchemasLoader(BatchWriter batchWriter) {
		super(batchWriter);
		initializeDefaultSchemasLoader();
	}

	/**
	 * Adds the DocumentFileProcessors and FileFilters specific to loading schemas, which will then be used to construct
	 * a DocumentFileReader by the parent class.
	 */
	protected void initializeDefaultSchemasLoader() {
		addDocumentFileProcessor(new TdeDocumentFileProcessor(this.schemasDatabaseClient, this.tdeValidationDatabase));
		addFileFilter(new DefaultFileFilter());
	}

	/**
	 * Run the given paths through the DocumentFileReader, and then send the result to the BatchWriter, and then return
	 * the result.
	 *
	 * @param paths
	 * @return a DocumentFile for each file that was loaded as a schema
	 */
	@Override
	public List<DocumentFile> loadSchemas(String... paths) {
		ClientHelper helper = new ClientHelper(schemasDatabaseClient);
		if (helper.getMLEffectiveVersion() >= 10000900 && tdeValidationDatabase != null && !tdeValidationDatabase.isEmpty()) {
			logger.info("Installing tde's using tde.templateBatchInsert");
			List<DocumentFile> documentFiles = super.getDocumentFiles(paths);
			buildTemplateBatchInsertCall(documentFiles).eval().close();
			return documentFiles;
		}
		return super.loadFiles(paths);
	}

	public String getTdeValidationDatabase() {
		return tdeValidationDatabase;
	}

	public void setTdeValidationDatabase(String tdeValidationDatabase) {
		this.tdeValidationDatabase = tdeValidationDatabase;
	}

	protected ServerEvaluationCall buildTemplateBatchInsertCall(List<DocumentFile> documentFiles) {
		String tdeTemplate = getTdeBatchInsertQuery(documentFiles);
		StringBuilder script = new StringBuilder("declareUpdate(); xdmp.invokeFunction(function() {var tde = require('/MarkLogic/tde.xqy');");
		script.append(tdeTemplate);
		script.append(format("}, {database: xdmp.database('%s')})", tdeValidationDatabase));
		return schemasDatabaseClient.newServerEval().javascript(script.toString());
	}

	private String getTdeBatchInsertQuery(List<DocumentFile> documentFiles) {
		List<String> templateInfoList = new ArrayList<>();
		for (DocumentFile doc : documentFiles) {
			String uri = doc.getUri();
			String content = doc.getContent().toString();

			// Permissions
			DocumentMetadataHandle.DocumentPermissions documentPermissions = doc.getDocumentMetadata().getPermissions();
			List<String> permissionList = new ArrayList<>();
			documentPermissions.keySet().forEach(key -> {
				Set<DocumentMetadataHandle.Capability> values = documentPermissions.get(key);
				values.forEach(value -> permissionList.add(String.format("xdmp.permission('%s', '%s')", key, value)));
			});
			String permissions = "[".concat(permissionList.stream().collect(Collectors.joining(", "))).concat("]");

			// Collections
			List<String> collectionsList = new ArrayList<>();
			doc.getDocumentMetadata().getCollections().forEach(collection -> collectionsList.add(collection));
			String collections = collectionsList.stream().map(coll -> '"' + coll + '"').collect(Collectors.joining(", "));
			collections = "[".concat(collections).concat("]");

			// Template info
			String templateFormat = "";
			if (doc.getFormat().toString().equals("XML")) {
				templateFormat = String.format("tde.templateInfo('%s', xdmp.unquote(`%s`), %s, %s)", uri, content, permissions, collections);
			} else if (doc.getFormat().toString().equals("JSON")) {
				templateFormat = String.format("tde.templateInfo('%s', xdmp.toJSON(%s), %s, %s)", uri, content, permissions, collections);
			} else {
				templateFormat = String.format("tde.templateInfo('%s',%s, %s, %s)", uri, content, permissions, collections);
			}
			templateInfoList.add(templateFormat);
		}

		String templateString = "tde.templateBatchInsert(["
			.concat(templateInfoList.stream().collect(Collectors.joining(",")))
			.concat("]);");
		return templateString;
	}
}
