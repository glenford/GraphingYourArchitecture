Graphing Your Architecture
==========================

This code is associated with a series of blog posts on how you can graph your architecture, in this case using neo4j and Clojure.

In order to gain an understanding of what this code is attempting to achieve, please read the following _TODO_


How to run the code
-------------------

First ensure that you have appropriate AWS credentials in your environment either by setting appropriate environment variables or through using an AWS instances with an appropriately associated IAM role.

	bash$ export AWS_ACCESS_KEY_ID={your access key id here}
        bash$ export AWS_SECRET_KEY={your secret key here}

Then ensure you have neo4j running locally (or adjust ```config.clj``` to point to the appropriate location). Note that the version of Neocons used will not work with the latest authentication method enabled, to disable server auth you need to update ```conf/neo4j-server.properties``` and set:

        dbms.security.auth_enabled=false

To run the code:

	bash$ git clone git@github.com:glenford/GraphingYourArchitecture.git
        bash$ cd GraphingYourArchitecture
	bash$ lein repl
	
	graph-your-arch.core=> (go)


It will attempt to open your browser to the neo4j web console so you can explore your graph further.


License
-------

This code is licensed under the MIT license.

