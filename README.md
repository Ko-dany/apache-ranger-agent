# clickhouse-ranger-agent (Stage 2: IPC)

JVM agent hosting the official Ranger plugin (service type `clickhouse`).
ClickHouse (C++) asks it "is this allowed?" over a local TCP socket. Fail-closed everywhere.

## Build
    mvn -q clean package -Dranger.version=2.5.0      # later: -Dranger.version=2.9.0

## Run
    java -cp conf:target/clickhouse-ranger-agent-0.1.0-SNAPSHOT.jar com.acceldata.clickhouse.ranger.AgentMain [--daemon]
Options (-D): agent.host=127.0.0.1, agent.port=9099, agent.principal + agent.keytab (Kerberos keytab login;
without them the kinit ticket cache is used), agent.log.decisions=true.
If Kerberos is enabled (core-site.xml) and login fails, the agent exits instead of silently using SIMPLE auth.

## Test the IPC by hand (needs a running agent)
    printf 'PING\n' | nc 127.0.0.1 9099
    printf 'CHECK\tclickhouse\tSELECT\tdb1\tt1\t\n' | nc 127.0.0.1 9099           # -> ALLOW / DENY<TAB>reason
    printf 'CHECK\tclickhouse\tDROP TABLE\tdb1\tsecret\t\n' | nc 127.0.0.1 9099
    printf 'CHECK\tbob\tTRUNCATE\tdb1\tt1\t\n' | nc 127.0.0.1 9099               # unmapped -> DENY

## Protocol (one line in, one line out, TAB-separated)
    CHECK  user  privileges  database  table  column  [groups]  -> ALLOW | DENY<TAB>reason
    PING -> PONG        REFRESH -> OK
privileges = comma-separated ClickHouse privilege names; ALL must be allowed. Empty table/column = not given.
Database is required (global-level checks are denied in the MVP). Unmapped privilege => DENY.

## Privilege mapping (MVP) - see PrivilegeMapper
SELECT->select, INSERT->insert, ALTER UPDATE->update, ALTER DELETE->delete, other ALTER*->alter,
CREATE*/DROP*/SHOW* (database/table/view/dictionary)->create/drop/show. Everything else => DENY.

## Offline self test (no Ranger/Hadoop needed)
    javac -d /tmp/t src/main/java/com/acceldata/clickhouse/ranger/{AccessChecker,PrivilegeMapper,RequestHandler,IpcServer}.java \
          src/test/java/com/acceldata/clickhouse/ranger/ProtocolSelfTest.java
    java -cp /tmp/t com.acceldata.clickhouse.ranger.ProtocolSelfTest

## conf (Stage 1 files are unchanged)
Optional: silence tag noise (not used in the MVP) by adding to ranger-clickhouse-security.xml:
    ranger.plugin.clickhouse.disable.tag.retriever = true
    ranger.plugin.clickhouse.disable.tag.policy.evaluation = true
