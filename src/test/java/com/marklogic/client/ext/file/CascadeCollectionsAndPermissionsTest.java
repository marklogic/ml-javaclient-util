/*
 * Copyright (c) 2023 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.ext.file;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.AbstractIntegrationTest;
import com.marklogic.client.ext.modulesloader.impl.AssetFileLoader;
import com.marklogic.client.ext.modulesloader.impl.DefaultModulesFinder;
import com.marklogic.client.ext.modulesloader.impl.DefaultModulesLoader;
import com.marklogic.client.io.DocumentMetadataHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CascadeCollectionsAndPermissionsTest extends AbstractIntegrationTest {

	final private String PARENT_COLLECTION = "ParentCollection";
	final private String CHILD_COLLECTION = "ChildCollection";

	private GenericFileLoader loader;

	@BeforeEach
	public void setup() {
		client = newClient(MODULES_DATABASE);
		DatabaseClient modulesClient = client;
		modulesClient.newServerEval().xquery("cts:uris((), (), cts:true-query()) ! xdmp:document-delete(.)").eval();
	}

	@Test
	public void parentWithBothProperties() {
		String directory = "src/test/resources/process-files/cascading-metadata-test/parent1-withCP";
		GenericFileLoader loader = new GenericFileLoader(client);
		loader.loadFiles(directory);

		verifyCollections("/child1/child1.json", "ParentCollection");
		verifyPermissions("/child1/child1.json", "rest-writer", "update");

		verifyCollections("/child1_1-noCP/test.json", PARENT_COLLECTION);
		verifyPermissions("/child1_1-noCP/test.json", "rest-writer", "update");

		verifyCollections( "/child1_2-withCP/test.json", CHILD_COLLECTION);
		verifyPermissions( "/child1_2-withCP/test.json", "rest-reader", "read");

		verifyCollections("/child2/child2.json", "child2");
		verifyPermissions("/child2/child2.json", "app-user", "read");

		verifyCollections("/child3_1-withCP/test.json", CHILD_COLLECTION);
		verifyPermissions("/child3_1-withCP/test.json", "rest-reader", "read");

		verifyCollections("/child3_1-withCP/grandchild3_1_1-noCP/test.json", CHILD_COLLECTION);
		verifyPermissions("/child3_1-withCP/grandchild3_1_1-noCP/test.json", "rest-reader", "read");

		verifyCollections("/parent.json", "ParentCollection");
		verifyPermissions("/parent.json", "rest-writer", "update");
	}

	@Test
	public void parentWithNoProperties() {
		String directory = "src/test/resources/process-files/cascading-metadata-test/parent2-noCP";
		GenericFileLoader loader = new GenericFileLoader(client);
		loader.loadFiles(directory);

		verifyCollections( "/child2_1-withCP/test.json", CHILD_COLLECTION);
		verifyPermissions( "/child2_1-withCP/test.json", "rest-reader", "read");

		verifyCollections( "/child2_2-noCP/test.json");
		verifyPermissions( "/child2_2-noCP/test.json");

		verifyCollections( "/child2_3-withCnoP/test.json", PARENT_COLLECTION);
		verifyPermissions( "/child2_3-withCnoP/test.json");

		verifyCollections( "/child2_3-withCnoP/grandchild2_3_1-withPnoC/test.json", PARENT_COLLECTION);
		verifyPermissions( "/child2_3-withCnoP/grandchild2_3_1-withPnoC/test.json", "rest-reader", "read");
	}

	/**
	 * Verifies that by default, cascading is disabled. This is to preserve backwards compatibility in 4.x. We
	 * expect to change this for 5.0.
	 */
//	@Test
//	void cascadingDisabled() {
//		loader = new GenericFileLoader(client);
//
//		loader.loadFiles("src/test/resources/process-files/cascading-metadata-test/parent1-withCP");
//		verifyCollections("/child1_1-noCP/test.json");
//		verifyPermissions("/child1_1-noCP/test.json");
//	}

	@Test
	public void withPropertiesFiles() {
		AssetFileLoader fileLoader = new AssetFileLoader(client);
		fileLoader.setPermissions("rest-extension-user,read,rest-extension-user,update,rest-extension-user,execute");

		DefaultModulesLoader modulesLoader = new DefaultModulesLoader(fileLoader);
		modulesLoader.setModulesManager(null);

		String dir = Paths.get("src", "test", "resources", "base-dir-with-properties-files").toString();
		Set<Resource> files = modulesLoader.loadModules(dir, new DefaultModulesFinder(), client);
		assertEquals(2, files.size());

		DocumentMetadataHandle metadata = new DocumentMetadataHandle();

		client.newDocumentManager().readMetadata("/root.sjs", metadata);
		assertEquals(1, metadata.getCollections().size());
		assertEquals("parent", metadata.getCollections().iterator().next());
		DocumentMetadataHandle.DocumentPermissions perms = metadata.getPermissions();
		assertEquals(DocumentMetadataHandle.Capability.READ, perms.get("qconsole-user").iterator().next());
		assertEquals(3, perms.get("rest-extension-user").size());

		client.newDocumentManager().readMetadata("/lib/lib.sjs", metadata);
		assertEquals(1, metadata.getCollections().size());
		assertEquals("lib", metadata.getCollections().iterator().next());
		perms = metadata.getPermissions();
		assertEquals(DocumentMetadataHandle.Capability.UPDATE, perms.get("app-user").iterator().next());
		assertEquals(3, perms.get("rest-extension-user").size());
	}
}
