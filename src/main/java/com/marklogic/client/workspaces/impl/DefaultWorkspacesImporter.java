package com.marklogic.client.workspaces.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.workspaces.WorkspacesImporter;

public class DefaultWorkspacesImporter implements WorkspacesImporter {

	@Override
	public Set<File> importWorkspaces(Path path, DatabaseClient client, String user) {

		//TODO: abstract out
        String xquery = getFileContents(Paths.get("src/main/xqy/qc-workspace-importer.xqy"));
        String workspace = getFileContents(path);
        
		Set<File> importedWorkspaces = new HashSet<>();
		client.newServerEval()
			.addVariable("exported-workspace", workspace)
			.addVariable("user", user)
			.xquery(xquery)
			.eval();
 
		//TODO: error checking
        importedWorkspaces.add(path.toFile());

        return importedWorkspaces;
    }

	//TODO: readAllLines could be expensive with large worksapces - refactor
	private String getFileContents(Path path) {
		StringBuffer sb = new StringBuffer();
		try {
			List<String> lines = Files.readAllLines(path);
			for (String s : lines) {
				sb.append(s);
			};
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	


}
