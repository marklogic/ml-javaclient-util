package com.marklogic.client.qconsole.impl;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.helper.LoggingObject;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.qconsole.WorkspaceManager;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Default implementation that uses the ML Java Client API to manage workspaces. By default, workspaces
 * will be exported and imported to/from ~/.qconsole/workspaces/(ML username).
 */
public class DefaultWorkspaceManager extends LoggingObject implements WorkspaceManager {

    private DatabaseClient client;

    private File baseDir;

    public DefaultWorkspaceManager(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public List<File> exportWorkspaces(String user, String... workspaceNames) {
        String xquery = "declare namespace qconsole='http://marklogic.com/appservices/qconsole'; " +
                "declare variable $user external; " +
                "declare variable $workspaceNames external; " +
                
                "let $workspaceQuery := if ($workspaceNames) "
                + "then (cts:element-value-query(xs:QName('qconsole:name'), ((fn:tokenize($workspaceNames, ','))))) else () "
                + "let $_ := xdmp:log($workspaceQuery)"
                + "return "
                + "cts:search(/qconsole:workspace, "
                + "cts:and-query((cts:element-value-query(xs:QName('qconsole:userid'), string(xdmp:user($user))), $workspaceQuery)))";

        EvalResultIterator result = client.newServerEval()
                .addVariable("user", user)
                .addVariable("workspaceNames", StringUtils.arrayToCommaDelimitedString(workspaceNames))
                .xquery(xquery).eval();

        if (baseDir == null) {
            baseDir = getDefaultWorkspacesDir();
        }

        File userDir = new File(baseDir, user);
        userDir.mkdirs();

        List<File> files = new ArrayList<>();

        while (result.hasNext()) {
            DOMHandle dom = result.next().get(new DOMHandle());
            String workspaceId = getWorkspaceId(dom);
            File f = new File(userDir, workspaceId + ".xml");
            try {
                FileCopyUtils.copy(dom.toBuffer(), f);
                if (logger.isInfoEnabled()) {
                    logger.info(format("Exported workspace %s to %s", workspaceId, f.getAbsolutePath()));
                }
                files.add(f);
            } catch (IOException ie) {
                throw new RuntimeException("Unable to write workspace XML to file, workspace ID: " + workspaceId + "; cause: " + ie.getMessage());
            }
        }

        return files;
    }

    protected String getWorkspaceId(DOMHandle dom) {
        return dom.get().getDocumentElement().getElementsByTagNameNS("http://marklogic.com/appservices/qconsole", "id").item(0).getTextContent();
    }

    @Override
	public List<File> importWorkspaces(String user, String... workspaceNames) {
		if (baseDir == null) {
			baseDir = getDefaultWorkspacesDir();
		}

		List<File> files = new ArrayList<>();
		File userDir = new File(baseDir, user);
		if (!userDir.exists()) {
			return files;
		}

		List<String> workspacesList = workspaceNames == null ? new ArrayList<String>() : Arrays.asList(workspaceNames);
		boolean isImportAll = workspacesList.isEmpty();
		
		DocumentBuilder docBuilder = null;
		if (!isImportAll) {
			try {
				docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (ParserConfigurationException e1) {
	            throw new RuntimeException("Unable to import specified workspaces because DocumentBuilder could not be created; cause: " + e1.getMessage());
			}
		}

		String xquery = "import module namespace amped-qconsole = 'http://marklogic.com/appservices/qconsole/util-amped' at '/MarkLogic/appservices/qconsole/qconsole-amped.xqy'; "
				+ "declare variable $workspace external; " + "declare variable $uri external; "
				+ "amped-qconsole:qconsole-document-insert($uri, $workspace)";

		for (File f : userDir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".xml")) {
				if (isImportAll || isWorkspaceImportable(f, workspacesList, docBuilder)) {
					client.newServerEval().addVariable("uri", "/workspaces/" + f.getName())
							.addVariable("workspace", new FileHandle(f).withFormat(Format.XML)).xquery(xquery)
							.eval();

					if (logger.isInfoEnabled()) {
						logger.info(format("Imported workspace from %s", f.getAbsolutePath()));
					}
					files.add(f);
				} 
			}
		}
		return files;
	}

    private boolean isWorkspaceImportable(File f, List<String> workspacesList, DocumentBuilder docBuilder) {
   
		String docWsName = null;
		try {
			Document doc = docBuilder.parse(f);
			docWsName = doc.getElementsByTagName("qconsole:name").item(0).getTextContent();
		} catch (DOMException | SAXException | IOException e) {
            throw new RuntimeException("Unable to import workspace to MarkLogic: " + f.getName() + "; cause: " + e.getMessage());
		}
		return (workspacesList.contains(docWsName));
	}

	/**
     * Defaults to ~/.qconsole/workspaces.
     *
     * @return
     */
    protected File getDefaultWorkspacesDir() {
        File homeDir = new File(System.getProperty("user.home"));
        File qcDir = new File(homeDir, ".qconsole");
        File dir = new File(qcDir, "workspaces");
        dir.mkdirs();
        return dir;
    }


    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }
}
