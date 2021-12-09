# Deploy to Openshift

## Generated Artifacts
| Artifact                               | Description                                                             |
|----------------------------------------|-------------------------------------------------------------------------|
| weblogic/domain/archive.zip            | Application binary ear, or war, or jar file compressed into a zip file  |
| weblogic/domain/topology.original.yaml | Weblogic Server Topology Descriptor                                     |

## Non-Generated/Provided Artifacts
| Artifact                     | Description                                         |
|------------------------------|-----------------------------------------------------|
| weblogic/Dockerfile          | Dockerfile to build the application container image |
| weblogic/Dockerfile.template | Dockerfile template to be used during scripting automation process |
| weblogic/build.sh            | A build script used to invoke the build process for automation |
| weblogic/properties          | Property files used to aid in the container image build and run process |
| weblogic/container-scripts   | Scripts used to aid in the build of the application image during a Docker build run |
| scripts/*.sql                | Database creation scripts |
| jumpbox/start-build.sh       | Start build script, which gets placed onto the Jumpbox to kickoff of a build |
| pipeline/k8s/ | Directory for the application deployment manifest files |
| pipeline/openshift/ | Directory for OpenShift pipeline manifest files |

## Setup a Jumpbox

To leverage build automation of Docker images on Openshift, for this project, it requires the use of a jumpbox. To set up a jumpbox: 

see [docs/JUMPBOX_SETUP](../docs/JUMPBOX_SETUP.md)

### Notes
- The pipeline artifacts for openshift are compatible with OpenShift >= 4.6.

## Deploy to Red Hat OpenShift Container Platform using OpenShift Pipelines

### Prerequisites
1. Install the OpenShift Pipeline Operator if its not already installed. See OpenShift [documentation](https://docs.openshift.com/container-platform/4.6/pipelines/installing-pipelines.html) for more details.

2. A jumpbox to build your container image. See previous section on setting up a jumpbox.

### Steps - Build and Deploy

Below are build and deploy steps for "as is" deployment. 

Note: You will need a jumpbox for this work. see [docs/JUMPBOX_SETUP](../docs/JUMPBOX_SETUP.md)

```bash
# delete project if it exists and wait for it to terminate
oc delete project demo-todo-wls12214

# create project
oc new-project demo-todo-wls12214

# apply jumpbox secrets (these are not checked in, and you need to generate
# this if you do not have it -- see docs/JUMPBOX_SETUP.md)
oc apply -f /path/to/jumpbox-secrets.yaml

# create pipeline, pipeline resources, and a pipeline run to trigger a pipeline
oc create -f wls12214-todo-app-migration/pipeline/openshift

# navigate to the route below after deployment
oc get routes
```

## References

- https://github.com/jnovotni/weblogic-operator-on-openshift
