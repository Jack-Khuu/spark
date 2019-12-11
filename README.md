Compile and Run (will take a few minutes):
            ./build/mvn -DskipTests clean package

Enter sparkShell:
            ./bin/spark-shell

When done with a sparkShell instance:
            rm -rf spark-warehouse/

UI tracking of spark jobs
            http://dragon.cs.washington.edu:4040/jobs/

Running a file in sparkShell:
            :load <>.scala

Run provided sparkShell experiment code:
            :load 1119.scala


