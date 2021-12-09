# Generating a Weblogic Server Topology with Weblogic Deploy Tooling

## Steps to Generate Topology

```bash
# Grab a copy of WDT (Weblogic Deploy Tooling)
wget https://github.com/oracle/weblogic-deploy-tooling/releases/download/release-1.9.12/weblogic-deploy.zip

# Unzip it's contents to folder "weblogic-deploy"
unzip weblogic-deploy.zip -d weblogic-deploy  

# Set Weblogic home to existing app server environment 
# Note: This will be specific to your environment
WLS_HOME=/path/to/weblogic-home/wls12214

# Set Weblogic domain home to existing domain
# Note: This will be specific to your environment
DOMAIN_HOME=$WLS_HOME/user_projects/domains/base_domain

# Set the name of output files for topology and archive
# Note: These can be arbitrary file names
OUT_ARCHIVE=archive.zip     # must be a zip archive file name
OUT_TOPOLOGY=topology.yaml  # must be a yaml file name

# Run the Weblogic Deploy Tooling script "discoverDomain" to generate the topology of your existing
# Weblogic deployment.
weblogic-deploy/bin/discoverDomain.sh \
-oracle_home $WLS_HOME \
-domain_home $DOMAIN_HOME \
-archive_file $OUT_ARCHIVE \
-model_file $OUT_TOPOLOGY
```

This will generated two output files as specified above: 

```bash
.
├── archive.zip
└── topology.yaml
```

These two generated files are then placed into: 

```
/wls12214-todo-app-migration/weblogic/domain
```

## Overview of Generated Files

The contents of the archive file are the binary artifacts deployed to the Weblogic server environment. If you were to unzip those contents, they would look similar to the following layout: 

```
archive
└── wlsdeploy
    ├── applications
    │   └── wls12214-todo-app-ear-1.0-SNAPSHOT.ear
    └── domainLibraries
        └── postgresql-42.3.1.jar
```

The topology file that is generated represents the layout of your server environment. This may include configurations on Admin Server, Cluster layout, and JDBC configurations, to mention a few. 

```yaml
domainInfo:
    AdminUserName: '--FIX ME--'
    AdminPassword: '--FIX ME--'
    domainLibraries: [ 'wlsdeploy/domainLibraries/postgresql-42.3.1.jar' ]
topology:
    Name: base_domain
    DomainVersion: 12.2.1.4.0
    NMProperties:
        JavaHome: '/home/kelee/.sdkman/candidates/java/1.8.0_301-oracle'
        weblogic.StartScriptName: startWebLogic.sh
    Server:
        AdminServer:
resources:
    JDBCSystemResource:
        'JDBC Data Source-0':
            Target: AdminServer
            JdbcResource:
                DatasourceType: GENERIC
                JDBCConnectionPoolParams:
                    TestTableName: SQL SELECT 1
                JDBCDataSourceParams:
                    JNDIName: pg
                JDBCDriverParams:
                    URL: 'jdbc:postgresql://localhost:5432/todo'
                    PasswordEncrypted: '--FIX ME--'
                    DriverName: org.postgresql.Driver
                    Properties:
                        user:
                            Value: postgres
appDeployments:
    Application:
        'wls12214-todo-app-ear_ear':
            SourcePath: 'wlsdeploy/applications/wls12214-todo-app-ear-1.0-SNAPSHOT.ear'
            ModuleType: ear
            Target: AdminServer
```

To accommodate the build of the container image, we modify the template so that the Dockerfile can leverage external properties as well as modifications on the target server layout (i.e. admin servers, clusters, etc.).

these properties can be found in: [wls12214-todo-app-migration/weblogic/properties](../wls12214-todo-app-migration/weblogic/properties)

```yaml
# This is a modified topology to suite the needs of deployment for Openshift.
# See `topology.original.yaml` for comparison of changes
domainInfo:
  AdminUserName: '@@FILE:/u01/oracle/properties/adminuser.properties@@'
  AdminPassword: '@@FILE:/u01/oracle/properties/adminpass.properties@@'
  domainLibraries: [ 'wlsdeploy/domainLibraries/postgresql-42.3.1.jar' ]
topology:
  Name: '@@PROP:DOMAIN_NAME@@'
  AdminServerName: '@@PROP:ADMIN_NAME@@'
  ProductionModeEnabled: '@@PROP:PRODUCTION_MODE_ENABLED@@'
  Log:
    FileName: '@@PROP:DOMAIN_NAME@@.log'
  Cluster:
    '@@PROP:CLUSTER_NAME@@':
      DynamicServers:
        ServerTemplate: '@@PROP:CLUSTER_NAME@@-template'
        CalculatedListenPorts: false
        ServerNamePrefix: '@@PROP:MANAGED_SERVER_NAME_BASE@@'
        DynamicClusterSize: '@@PROP:CONFIGURED_MANAGED_SERVER_COUNT@@'
        MaxDynamicClusterSize: '@@PROP:CONFIGURED_MANAGED_SERVER_COUNT@@'
  Server:
    '@@PROP:ADMIN_NAME@@':
      ListenPort: '@@PROP:ADMIN_PORT@@'
      NetworkAccessPoint:
        T3Channel:
          ListenPort: '@@PROP:T3_CHANNEL_PORT@@'
          PublicAddress: '@@PROP:T3_PUBLIC_ADDRESS@@'
          PublicPort: '@@PROP:T3_CHANNEL_PORT@@'
  ServerTemplate:
    '@@PROP:CLUSTER_NAME@@-template':
      ListenPort: '@@PROP:MANAGED_SERVER_PORT@@'
      Cluster: '@@PROP:CLUSTER_NAME@@'
resources:
  JDBCSystemResource:
    'jdbc-data-source':
      Target: '@@PROP:ADMIN_NAME@@,@@PROP:CLUSTER_NAME@@'
      JdbcResource:
        DatasourceType: GENERIC
        JDBCConnectionPoolParams:
          TestTableName: SQL SELECT 1
        JDBCDataSourceParams:
          JNDIName: pg
        JDBCDriverParams:
          URL: 'jdbc:postgresql://postgres.demo-todo-wls12214:5432/todo'
          PasswordEncrypted: postgres
          DriverName: org.postgresql.Driver
          Properties:
            user:
              Value: postgres
appDeployments:
  Application:
    'wls12214-todo-app_war':
      SourcePath: 'wlsdeploy/applications/wls12214-todo-app-ear-1.0-SNAPSHOT.ear'
      ModuleType: ear
      Target: '@@PROP:CLUSTER_NAME@@'
      StagingMode: nostage
      PlanStagingMode: nostage

```

More info about generating topology with Weblogic Deploy Tooling, found here - see [Weblogic Deploy Tooling](https://oracle.github.io/weblogic-deploy-tooling/) 

# References
https://github.com/jnovotni/weblogic-operator-on-openshift


