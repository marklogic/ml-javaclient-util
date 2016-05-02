package com.marklogic.client.workspaces.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.workspaces.WorkspacesExporter;

public class DefaultWorkspacesExporter implements WorkspacesExporter {

	@Override
	public Set<File> exportWorkspaces(String fileName, String user,
			String workspace, DatabaseClient client) {

		String xquery = getFileContents(Paths
				.get("src/main/xqy/qc-workspace-exporter.xqy"));

		Set<File> exportedWorkspaces = new HashSet<>();
		EvalResultIterator result = client.newServerEval()
				.addVariable("workspace", workspace)
				.addVariable("user", user)
				.xquery(xquery).eval();

		Path exportedWorkspace = Paths.get(fileName);
		;
		while (result.hasNext()) {
			EvalResult er = result.next();
			try {
				Files.write(exportedWorkspace,
						er.getString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		// TODO: error checking
		exportedWorkspaces.add(exportedWorkspace.toFile());

		return exportedWorkspaces;
	}

	// TODO: readAllLines could be expensive with large worksapces - refactor
	private String getFileContents(Path path) {
		StringBuffer sb = new StringBuffer();
		try {
			List<String> lines = Files.readAllLines(path);
			for (String s : lines) {
				sb.append(s);
			}
			;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
