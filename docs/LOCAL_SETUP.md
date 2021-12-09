# Deploy Locally

## Setup Postgresql

execute the following SQL scripts on your Postgresql Database server located in:

```bash
├── wls12214-todo-app-migration
│   └── scripts
│       └── 01-database.sql
│       └── 02-schemas.sql
```

## Build, Deploy and Run

**Build the WAR file and package into an EAR file**

```bash
mvn clean install package
```
The resulting build will show in the following target folders:

```bash
├── wls12214-todo-app
│   └── target
│       └── wls12214-todo-app-1.0-SNAPSHOT.war
├── wls12214-todo-app-ear
│   └── target
│       └── wls12214-todo-app-ear-1.0-SNAPSHOT.ear
```

**Start the application server (if not started already)**

- start by command line:
    - `wls12214/user_projects/domains/base_domain/startWeblogic.sh`

**Setup JNDI in Websphere Application Server**

Login to the server:

Weblogic Admin Console - http://localhost:7001/console


You will need to have a copy of the Postgres Driver Library at hand, `postgresql-42.3.1.jar`, to point to in the JNDI setup classpath below. see:

- Add a JDBC Provider 

![Screenshot from 2021-12-07 18-05-37](https://user-images.githubusercontent.com/61749/145124950-5a1d5350-2afd-4432-9168-2703c6af57b1.png)
)

- Configure a Connection Pool under the JDBC Provider

![Screenshot from 2021-12-07 18-06-01](https://user-images.githubusercontent.com/61749/145124943-ee8df872-7eae-4052-af9d-0d34b6afd62f.png)

**Deploy the EAR file**

Weblogic Admin Console - http://localhost:7001/console

![Screenshot from 2021-12-07 18-11-25](https://user-images.githubusercontent.com/61749/145125393-48379ecf-8dee-4450-a84a-fca0beff8cad.png)


**Validate your database connectivity by navigating to:**

http://localhost:7001/wls12214-todo-app/dbtest-servlet

**Validate your application by navigating to:**

http://localhost:7001/wls12214-todo-app