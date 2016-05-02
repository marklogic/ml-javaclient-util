package com.marklogic.client.workspaces;
import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import com.marklogic.client.DatabaseClient;

public interface WorkspacesImporter {

	Set<File> importWorkspaces(Path path, DatabaseClient client, String user);
	 
}
