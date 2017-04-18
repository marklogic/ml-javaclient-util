package com.marklogic.client.workspaces.impl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.workspaces.impl.DefaultWorkspacesExporter;

public class DefaultWorkspacesExporterTest extends Assert {
	
	private static final String FILE_NAME = "/Users/thale/git/ml-javaclient-util/src/main/ml-workspaces/test-workspace.xml";
	private DefaultWorkspacesExporter wsx;
	
	@Test
	public void testExportWorkspaces() throws IOException {

        DatabaseClient client = DatabaseClientFactory.newClient("obp-test-1.demo.marklogic.com", 8000, "thale", "isSparta",
                Authentication.DIGEST);
        
		wsx = new DefaultWorkspacesExporter();
		Set<File> exportedWorkspaces = wsx.exportWorkspaces(FILE_NAME, "admin", "Workspace", client);
		assertNotNull(exportedWorkspaces);
		
		Iterator<File> iter = exportedWorkspaces.iterator();
		while (iter.hasNext()) {
			File value = iter.next();
			assertNotNull(value);
			assertTrue(value.getName().endsWith(".xml"));
			assertTrue(value.getName().startsWith("test-workspace"));
			assertTrue(value.getPath().contains("ml-workspaces"));
		}
		
	}
	

}
