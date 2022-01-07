# Jumpbox Setup Guide

To automate this build w/in Openshift, we will require a separate jumpbox, external to the Openshift cluster.

We will need this box to build the container image of our application using Docker.

During a build, the Openshift Pipeline will leverage a Tekton Task which will ssh into the jumpbox to initiate a docker image build and push back to your Openshift cluster.

## Jumpbox Requirements

- Physical or virtualized environment. It must be reachable by your Openshift cluster (preferably on the same network)
- Any Linux environment that can support the following tooling below (i.e. Fedora (>= 33 ))
- Docker CE installed
- CLI Tools
  - Docker CLI
  - git CLI
  - wget CLI
  - Openshift CLI (`oc`) >= 4.6.x
- Openshift username/password (i.e. `oc_user/oc_passwd`) (via htpasswd)
    - You will need to set this up on your openshift cluster

## SSH Key Creation and Setup + Copy Build Script

**step 1: Create public and private keys using ssh-key-gen on local-host**

```bash
jsmith@local-host$ [Note: You are on local-host here]

jsmith@local-host$ ssh-keygen -t ed25519 -N '' -f ~/.ssh/id_wls_jumpbox         
Generating public/private ed25519 key pair.
Your identification has been saved in ~/.ssh/id_wls_jumpbox
Your public key has been saved in ~/.ssh/id_wls_jumpbox.pub
The key fingerprint is:
SHA256:T7wE/aHoM0TeaCekG3Vjz7BUhJyoz29IMz5mwiQ3Zic jsmith@local-host
```

**step 2: Copy the public key to jumpbox-host using ssh-copy-id**

```bash
jsmith@local-host$ ssh-copy-id -i ~/.ssh/id_wls_jumpbox.pub user@jumpbox-host
jsmith@remote-host's password:
```

**step 3: Create an Openshift secrets file of SSH keys and known_hosts**

NOTE: before creating the secret,  create a copy of the `known_hosts` file (i.e. located in `home/jsmith/.ssh/known_hosts`) and edit the copy to include only the jumpbox-host and remove other hosts.

```bash
jsmith@local-host$ oc create secret generic jumpbox-secrets \
  --from-file=id_ed25519=/home/jsmith/.ssh/id_wls_jumpbox \ 
  --from-file=id_ed25519.pub=/home/jsmith/.ssh/id_wls_jumpbox.pub \
  --from-file=known_hosts=/path/to/copy-of/modified-file/known_hosts \
  --from-literal=jumpbox_host=user@jumpbox-host  \
  --dry-run=client -o yaml > ~/jumpbox-secrets.yaml
```
the statement above will create a secret to a file (`jumpbox-secrets.yaml`) and not on the cluster. Do not distribute this file or check it in to source control.

**step 4: Login to remote-host without entering the password**

```bash
jsmith@local-host$ ssh user@jumpbox-host
Last login: Sun Nov 16 17:22:33 2008 from x.x.x.x
[Note: SSH did not ask for password.]

user@jumpbox-host$ [Note: You are on jump-host here]
```

exit from the session after validating login

```bash
user@jumpbox-host$ exit
jsmith@local-host$
```

**step 5: copy `wls12214-todo-app-migration/jumpbox/start-build.sh` script to `user` home directory of jumpbox host, `user@jumpbox-host`**

```bash
jsmith@local-host$ scp wls12214-todo-app-migration/jumpbox/start-build.sh user@jumpbox-host:~/
```

**step 6: update `~/start-build.sh` on jumpbox host**

Login into jumpbox:

```bash
jsmith@local-host$ ssh user@jumpbox-host
user@jumpbox-host$ vi ~/start-build.sh
```

and modify the following values on the script located on the **jumpbox** in `~/start-build.sh`:

```
OCP_USER=openshift_user # an openshift user with sufficient privileges
OCP_PASSWORD=openshift_passwd # your user password
OCP_SERVER=https://api.cluster-name.domain.local:6443 # your openshift server's api uri
GIT_BRANCH=main
```

## Openshift External Image Registry Access Setup

**step 1: create an openshift login token**

Login into the jumpbox host (if not already) 

```bash
jsmith@local-host$ ssh user@jumpbox-host
user@jumpbox-host$ 
```

On the jumpbox, login to Openshift (i.e.)

```bash
 user@jumpbox-host$ oc login -u openshift_user -p openshift_passwd  https://api.cluster-name.domain.local:6443
```

create an openshift login token and save it to a shell variable:

```bash
user@jumpbox-host$ token=$(oc whoami -t)
user@jumpbox-host$ echo $token
```

validate the output of the token

**step 2: Expose Openshift image registry**

Within Openshift, the internal image registry is located at:

```
image-registry.openshift-image-registry.svc:5000
```

To access this registry with Docker, ensure that the OpenShift registry is exposed by running the following command:

```
user@jumpbox-host$ oc patch configs.imageregistry.operator.openshift.io/cluster --patch '{"spec":{"defaultRoute":true}}' --type=merge
```

**step 3: Make openshift image registry accessible to Docker**

1. copy the external address to the openshift image registry. run the following to obtain it, and copy it to a clipboard (i.e. ctrl-c): 

```bash
user@jumpbox-host$ oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}'
```

you should get a value that looks similar to the following: 

```
default-route-openshift-image-registry.apps.cluster-name.domain
```

2. login as root

```bash
user@jumpbox-host$ sudo -i
root@jumpbox-host$
```

3. modify/create the file: `/etc/docker/daemon.json` and paste the following contents into it using the image registry address you copied earlier, and save the file.

```json
{
    "insecure-registries" : [
	    "default-route-openshift-image-registry.apps.cluster-name.domain:443"
    ]
}
```

4. create the following directory (while still logged in as root), using the address you copied earlier a directory name. It will look similar to the following below: 

```bash
root@jumpbox-host$ mkdir -p /etc/docker/certs.d/default-route-openshift-image-registry.apps.cluster-name.domain
```

5. create a `ca.crt` file from the openshift image registry you exposed earlier, into the directory created in the previous step. 

```bash
root@jumpbox-host$ (echo | openssl s_client -showcerts -connect default-route-openshift-image-registry.apps.cluster-name.domain:443) > /etc/docker/certs.d/default-route-openshift-image-registry.apps.cluster-name.domain/ca.crt
```
modify the `ca.crt` file, by deleting lines, such that you only keep the cert information between the `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----` delimiters. It should look similar to the following when completed. 

```bash
-----BEGIN CERTIFICATE-----
MIIDbzCCAlegAwIBAgIIPHTDIn7F+VYwDQYJKoZIhvcNAQELBQAwJjEkMCIGA1UE
AwwbaW5ncmVzcy1vcGVyYXRvckAxNjM2NTM5MTQ0MB4XDTIxMTExMDEwMTUzOVoX
DTIzMTExMDEwMTU0MFowJzElMCMGA1UEAwwcKi5hcHBzLm9rZC50aGVrZXVuc3Rl
ci5sb2NhbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALcGML8oA0Lw
p5UHwYnWacWbGZqvd/DWRdpwTmgmnlkNCl3zVxZoHcnLUno9FZSJfFc79LME3nfz
dcCuuyAovO6xROXMEm+eoaQXrNN+j40FZtw9fp+YFCsiM/9RuuKj3Oqo5ZA9AIwp
ZqAQqWXjF9Ukka8bEznzEMjTeE4zwv2SsekKHLZFdDopHZ9D0AoldaO2/+NWmuoD
Bm8ZGmV7lxwnmqeJT6ge+a64BexoP5QQnbwsRgCQT0GhY3bae5oXLtTfofad751D
bpMAx9rszCYltYmpjwm3F35SYm6OkwDD+UBS33Q0JzDNqueVcRHQwAMAvw2lppdK
ROeZ9ZLcZg0CAwEAAaOBnzCBnDAOBgNVHQ8BAf8EBAMCBaAwEwYDVR0lBAwwCgYI
KwYBBQUHAwEwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUqilKRKnGMbh1BpU6T14e
sU0j0+AwHwYDVR0jBBgwFoAUQuBSmjx2Huw3RAVe5flrRNIsZBEwJwYDVR0RBCAw
HoIcKi5hcHBzLm9rZC50aGVrZXVuc3Rlci5sb2NhbDANBgkqhkiG9w0BAQsFAAOC
AQEANgw723f572PGRtf07zc0k7Vd/YXq5mK/on0QKmdzXaGtgwI+SYKxg0YO8xs3
V+Bfha7jC/AI3uo5wU7St+xWkx/iteYwUUH3fwmJYTxhJ4gvYOYv8cZQOPYv4wV5
FT0C54210f3+xnmiq6KyCjTO97dq6TRvK3dN81pSZgEhMK5lpred7FT7s1v3a2Oh
cskOP9SWTwsKml1qpzj7827D8xjnLlpb1F5JVz7469Pz9cO+mFVWgav5UZ30SzKo
Ug6mqt6bkf+wHC0fVZKcRqlNI4ECvsRCV0Rl7uDiKZAhz4flPTsBAc8zDj7qFoUP
baL/XJoMt1ii+1M6LUPMoHIyXg==
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIDDDCCAfSgAwIBAgIBATANBgkqhkiG9w0BAQsFADAmMSQwIgYDVQQDDBtpbmdy
ZXNzLW9wZXJhdG9yQDE2MzY1MzkxNDQwHhcNMjExMTEwMTAxMjIzWhcNMjMxMTEw
MTAxMjI0WjAmMSQwIgYDVQQDDBtpbmdyZXNzLW9wZXJhdG9yQDE2MzY1MzkxNDQw
ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDvX73EJkJ2VVI+TVlWafJ7
8kMJYcZOAcd2WaI8wa2wTErXi+ipzsyStDlsRIyUeIBjt6ZrYrIWeGzAVUvji/cL
HyGCHEL4Rj0VIOETU9H/RSLJ5izMQ6waLYedm44wwo5iSpiy2R1Q0EillpT8LvzS
lndObYBV2wRkyWRM3DQRVMUEUchUpCdgXdUUFk6+PXZXiatqdYcl1YZkVP4nkaJF
kSLqs2NJmBFbCJmCeUaEpjYYhuNRoZ06ojTidjgfrYIhzJa0HoWj+j4pKB8sfVDh
7KBAAMFAiiY1/uS061SYvbQZW0YH12rhMNGBr2HrLSbj/zTJv4YVtTGuH8IzD9eD
AgMBAAGjRTBDMA4GA1UdDwEB/wQEAwICpDASBgNVHRMBAf8ECDAGAQH/AgEAMB0G
A1UdDgQWBBRC4FKaPHYe7DdEBV7l+WtE0ixkETANBgkqhkiG9w0BAQsFAAOCAQEA
2s9WL8ttuWsZZX86MxNxaFjZUMCXRoBVEfJj3SaPlJutg4r2SmRrJRmZXrA5PPcf
Q+k2l5OEncLMr0H2XxzkkLj+NJWcDc5yJFeqBW2gL25XxRkoLc6yzyoVmbliPEoY
tuP/T/89QvPMa8I8s1OO8P20dREm9M6lP+OaY436dIVV9osgMwmOAflqahr3lcGB
fxypSsUXggL32npautTCQExjNgVOP3chV00uEUrhXhioaj48JDeVOcZN9ItRMUV3
ByFzvxJEcwKUBA540P9g2B4qcOZiaTKTztzik8KK7jUwVz/gKObkqGyjC677jGtS
6aPHObaBuTq8L9IAWZccUQ==
-----END CERTIFICATE-----
```

7. restart the docker daemon: 

```bash
root@jumpbox-host$ systemctl restart docker
root@jumpbox-host$ systemctl status docker
```

if all is well, the docker daemon should have restarted and be active

8. log out as root

```bash
root@jumpbox-host$ exit
user@jumpbox-host$
```

**step 4: Login to Openshift image registry with Docker**

Login to the OpenShift registry to allow Docker to push images using your credentials by running the following command:

```bash
user@jumpbox-host$ docker login $(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')
```

When prompted, enter your OpenShift username, and the token (`echo $token`) from before. (The token will look like `sha256~xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`. 

You should be successfully logged into the Openshift image registry at this point

## Push Weblogic Container Image to Openshift Registry

You must have an account that has permission to access the Oracle Container Registry. This enables you to pull the base image used in the steps below. If you do not already have an account, you can go to https://container-registry.oracle.com and create one.

Additionally, once you login to the container registry at Oracle, navigate to the weblogic image page, and make sure to accept the terms and restrictions agreement, otherwise, you will NOT be able to pull down the image. 

![Screenshot from 2021-12-07 17-33-07](https://user-images.githubusercontent.com/61749/145122229-7e844362-301d-4117-b0d1-fcfc1bb5b8b4.png)

Login to the jumpbox if not already in: 

```bash
jsmith@local-host$ ssh user@jumpbox-host
user@jumpbox-host$ 
```

Login to the Oracle Container Registry to allow Docker to pull images using your credentials by running the following command:

```bash
user@jumpbox-host$ docker login container-registry.oracle.com
```

Pull the base image for the domain from the Oracle image registry by running the following command:

```bash
user@jumpbox-host$ docker pull container-registry.oracle.com/middleware/weblogic:12.2.1.4
```

The output will be similar to:

```bash
12.2.1.4: Pulling from middleware/weblogic
401a42e1eb4f: Pull complete
5779b03f4f45: Pull complete
1ea9ed498323: Pull complete
b99f19d3cc6a: Pull complete
3d288a26d69b: Pull complete
a1a80dd8562a: Pull complete
Digest: sha256:16eccb81a4ccf146326bad6bd9a74fb259799f5d968c6714aea80521197ae528
Status: Downloaded newer image for container-registry.oracle.com/middleware/weblogic:12.2.1.4
container-registry.oracle.com/middleware/weblogic:12.2.1.4
```

Push the weblogic image to your openshift image registry: 

```bash
user@jumpbox-host$ docker push $(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')/openshift/weblogic:12.2.1.4
```
