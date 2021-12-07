# NOTE: this is an example script of what to put in the home directory of your jumpbox
# it is NOT used in this repository. you will need to create your own version of this file
# and tailor it as necessary

# CHANGE THESE VALUES
OCP_USER=openshift_user # an openshift user with sufficient privileges
OCP_PASSWORD=openshift_passwd # your user password
OCP_SERVER=https://api.cluster-name.domain.local:6443 # your openshift server's api uri

# remove previous project build
rm -rf wls12214-todo-app-on-ocp

# login into openshift cluster
oc login -u $OCP_USER -p $OCP_PASSWORD --server=$OCP_SERVER

# get openshift image registry route
IMAGE_REGISTRY=$(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')

# obtain a login token for docker image registry login
IMAGE_REGISTRY_LOGIN_TOKEN=$(oc whoami -t)

# login into docker
docker login -u $OCP_USER -p $IMAGE_REGISTRY_LOGIN_TOKEN $IMAGE_REGISTRY

# get build assets by cloning the repository on the selected branch
git clone -b dev --single-branch https://github.com/keunlee/wls12214-todo-app-on-ocp.git

# launch build to build image and push to image registry
cd wls12214-todo-app-on-ocp/wls12214-todo-app-migration/weblogic/
source build.sh
