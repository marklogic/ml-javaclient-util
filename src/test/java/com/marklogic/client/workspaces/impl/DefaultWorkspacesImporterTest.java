package com.marklogic.client.workspaces.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.workspaces.WorkspacesImporter;
import com.marklogic.client.workspaces.impl.DefaultWorkspacesImporter;

public class DefaultWorkspacesImporterTest extends Assert {
	private static final String USER = "admin";
	private static final String WORKSPACE_PATH = "/Users/thale/git/ml-javaclient-util/src/test/resources/sample-base-dir/workspaces/sample-workspace.xml";
	private WorkspacesImporter wsi = new DefaultWorkspacesImporter();

	@Test 
	public void testImportWorkspaces() throws IOException {

		 DatabaseClient client = DatabaseClientFactory.newClient("obp-test-1.demo.marklogic.com", 8000, "thale", "isSparta",
	                Authentication.DIGEST);
		 
		Set<File> importedWorkspaces = wsi.importWorkspaces(Paths.get(WORKSPACE_PATH), client, USER);
		assertNotNull(importedWorkspaces);
		Iterator<File> iter = importedWorkspaces.iterator();
		while (iter.hasNext()) {
			File file = iter.next();
			assertNotNull(file);
			assertTrue(file.getName().endsWith(".xml"));
			
		}

		String xquery = "xquery version '1.0-ml';"
				+ "declare namespace qconsole='http://marklogic.com/appservices/qconsole';"
				+ "cts:search(doc()/qconsole:workspace, cts:and-query((cts:element-value-query(xs:QName('qconsole:name'), 'Tammy'))))";

		EvalResultIterator result = client.newServerEval()
				.xquery(xquery).eval();

		while (result.hasNext()) {
			EvalResult er = result.next();
			assertNotNull(er);
	}
	
}
}
