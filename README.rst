EDACC API
=========

Experiment Design and Administration for Computer Clusters for SAT Solvers.
See http://sourceforge.net/projects/edacc/ for the EDACC project.

This API provides an interface to EDACC databases to submit jobs, query their results etc.
Its primary use is for automated algorithm configuration tools that want to make use of
EDACC's job processing backend.

It also implements parameter space functionality that helps configurators to navigate through
the parameter space of a solver.

Usage
=====

To use the API, include the following JAR files in your code.
Also make sure that EDACCAPI.jar appears before EDACC.jar in the java class path.
- dist/EDACCAPI.jar
- libs/EDACC.jar
- libs/SevenZip.jar
- mysql-connector-java-5.1.13-bin.jar (or a later version, download from MySQL website or copy from EDACC GUI repository)