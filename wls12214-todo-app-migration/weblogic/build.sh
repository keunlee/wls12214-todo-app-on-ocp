wget https://github.com/oracle/weblogic-deploy-tooling/releases/download/release-1.9.12/weblogic-deploy.zip
git clone https://github.com/oracle/weblogic-kubernetes-operator.git
cd weblogic-kubernetes-operator
git checkout b5e5c314d8

cd ../

oc label ns demo-todo-wls12214 weblogic-operator=enabled

./weblogic-kubernetes-operator/kubernetes/samples/scripts/create-weblogic-domain-credentials/create-weblogic-credentials.sh -u weblogic -p password123 -n demo-todo-wls12214 -d domain1

source container-scripts/setEnv.sh properties/docker-build/domain.properties

CONTAINER_REGISTRY=$(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')
sed -i "s/@@REGISTRY@@/$CONTAINER_REGISTRY/g" Dockerfile.template

docker build \
    $BUILD_ARG \
    --build-arg WDT_MODEL=domain/topology.yaml \
    --build-arg WDT_VARIABLE=properties/docker-build/domain.properties \
    --build-arg WDT_ARCHIVE=domain/archive.zip \
    --force-rm=true \
    -f Dockerfile.template \
    -t wls12214todoapp:1.3 .
docker tag wls12214todoapp:1.3 $(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')/demo-todo-wls12214/wls12214todoapp:1.3
docker push $(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')/demo-todo-wls12214/wls12214todoapp:1.3