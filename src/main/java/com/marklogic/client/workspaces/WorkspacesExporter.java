package com.marklogic.client.workspaces;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.marklogic.client.DatabaseClient;

public interface WorkspacesExporter {

	Set<File> exportWorkspaces(String fileNameForWorkspace, String user,
			String workspaceToDownload, DatabaseClient client);
	 
}
