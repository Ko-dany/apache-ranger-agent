# clickhouse-ranger-agent (Stage 1 skeleton)

Goal of this stage: start a JVM, load the Ranger plugin for service type `clickhouse`,
download policies from Ranger Admin and evaluate checks. No ClickHouse involved.

## Build
    mvn -q package -Dranger.version=2.5.0      # later: -Dranger.version=2.9.0

## Configure
Edit `conf/ranger-clickhouse-security.xml`: Ranger Admin URL and service name.
If Ranger Admin uses Kerberos or SSL, extra settings are needed (not covered yet).
Group resolution uses Hadoop UserGroupInformation; put the cluster's core-site.xml on the classpath if needed.

## Run
    java -cp conf:target/clickhouse-ranger-agent-0.1.0-SNAPSHOT.jar \
         com.acceldata.clickhouse.ranger.AgentMain

Then type:
    check user1 select db1 t1
    check user1 select db1 t2 col1
    refresh
    quit

## Expected results
- A matching allow policy      -> ALLOW
- No matching policy           -> DENY
- A matching deny policy       -> DENY
- Policy change in Ranger UI   -> new result after `refresh` (or the poll interval)
