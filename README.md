# JDBC Test
A simple program to test the connection to a database via JDBC

REQUIREMENTS:
 - java jdk 1.7 or greater
 - mvn 3.0 or greater

QUICK START:
 - build with "mvn clean install"
 - copy jdbctest.props.sample to jdbctest.props
 - edit the -cp argument in run.sh to point to your driver jar instead of the current Oracle driver
 - run with ./run.sh


This program will test a connection and query to a database via JDBC
 
To use this test class copy jdbctest.props.sample to jdbctest.props and edit
to match your database information.  All parameters in the jdbctest.props
are required except sid, db.server.name  and db.url.  If db.url is not provided then it
is assumed you are connecting to Oracle and a url will be generated using
the sid (which is then required). If db.server.name is not provided the server name and port
number are parsed from the db.url, which is then required.
 
A jdbc library matching your db system must be included in the classpath when
running.


