package com.acceldata.clickhouse.ranger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Stage 1 prototype: verify that the JVM can talk to Ranger and evaluate policies,
 * without ClickHouse. Later stages replace stdin/stdout with the IPC server.
 *
 * Input (one request per line, tab- or space-separated):
 *   check  <user> <accessType> <database> [table] [column]
 *   refresh
 *   quit
 *
 * Usage:
 *   java -cp conf:target/clickhouse-ranger-agent-0.1.0-SNAPSHOT.jar com.acceldata.clickhouse.ranger.AgentMain
 * The conf/ directory (ranger-clickhouse-security.xml, ranger-clickhouse-audit.xml) must be on the classpath.
 */
public class AgentMain {
    public static void main(String[] args) throws Exception {
        ClickHouseAuthorizer authorizer = new ClickHouseAuthorizer();
        authorizer.init();
        System.out.println("READY (type: check <user> <accessType> <db> [table] [column] | refresh | quit)");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] t = line.split("\\s+");
            try {
                switch (t[0]) {
                    case "check": {
                        // check <user> <accessType> <db> [table] [column]
                        String user = t[1], access = t[2], db = t[3];
                        String table = t.length > 4 ? t[4] : null;
                        String column = t.length > 5 ? t[5] : null;
                        boolean ok = authorizer.isAllowed(user, null, access, db, table, column);
                        System.out.println(ok ? "ALLOW" : "DENY");
                        break;
                    }
                    case "refresh":
                        authorizer.refreshPolicies();
                        System.out.println("REFRESHED");
                        break;
                    case "quit":
                        authorizer.cleanup();
                        return;
                    default:
                        System.out.println("ERROR unknown command: " + t[0]);
                }
            } catch (Exception e) {
                // Any error is a deny, never an allow.
                System.out.println("DENY (error: " + e + ")");
            }
        }
        authorizer.cleanup();
    }
}
