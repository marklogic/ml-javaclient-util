package com.marklogic.client.qconsole.impl;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;

import java.io.IOException;

/**
 * Debug-style program for manually verifying how DefaultWorkspaceManager works.
 * <p>
 * If the content source ID (an appserver ID) is not valid, that's fine - the content source just defaults
 * to the first app server.
 * <p>
 * If the user doesn't exist, that's fine - it just won't be accessible.
 */
public class DefaultWorkspaceManagerDebug {

    public static void main(String[] args) throws IOException {
        DatabaseClient client = DatabaseClientFactory.newClient("obp-test-1.demo.marklogic.com", 8000, "App-Services", "thale", "isSparta", DatabaseClientFactory.Authentication.DIGEST);
        DefaultWorkspaceManager dwm = new DefaultWorkspaceManager(client);
        String user = "thale";
        String[] workspaces = {"workspace"};
//      String[] workspaces = null;
//      String[] workspaces = {"bogus name"};
//      String[] workspaces = {"workspace", "test name"};
        try {
            System.out.println(dwm.exportWorkspaces(user, workspaces));
            System.out.println(dwm.importWorkspaces(user));
        } finally {
            client.release();
        }
    }

}
