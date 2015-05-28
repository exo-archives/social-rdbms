Social RDBMS Addon
==================

 The Social RDBMS Addon is an eXo-addon used for version PLF-4.3.x
- Supports RDBMS for storage layer of social-intranet.
- Migrates social-intranet data from JCR to RDBMS (No rollback strategy is needed for data migration).

Usage
=====

Build from sources
------------------

To build add-on from sources use [Maven 3](http://maven.apache.org/download.html).

- Clone the project:

    git clone git@github.com:exo-addons/social-rdbms.git
    cd social-rdbms

- Build it:

    mvn clean install

- If you want to use `liquibase` (a tool to keep changelog), you need update info your mysql server on social-rdbms/webapp/pom.xml on `liquibase-maven-plugin` and use maven command-line

    mvn liquibase:update
    
Go to packaging bundle file created by last build in `social-rdbms/bundle/target/social-rdbms-addons-bundle-1.0.x-SNAPSHOT.zip`. Use it for deployment to Platform as below.


Deploy to eXo Platform
----------------------

Install [eXo Platform 4.3 Tomcat bundle](http://learn.exoplatform.com/Download-eXo-Platform-Express-Edition-En.html) to a directory, e.g. `/opt/platform-tomcat`.

Users of Platform 4.3, and those who installed [Addons Manager](https://github.com/exoplatform/addons-manager) in Platform 4.3, can simply install the add-on from central catalog by command:

```
./addon install social-rdbms
```

License
========

This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
