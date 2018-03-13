# jmeter-jmx-create

Create a Jmeter .jmx project from Java code with pre-configured proxy-recording controller.

# Purpose

My goal here is to create a Java project which will generate a JMeter JMX project file that
  contains a `HTTP(S) Test Script Recorder` pre-configured to record Ruby api tests that are ran
  through it.  Then I will be able to use that recording as a load test.
  
Once I get the basic thing working, I will refactor this into a re-useable library of some sort.

# Notes

Jmeter currently marshals .jmx files using XStream but I have not figured out yet where there Java bean code is for building a JMX manually in Java code.
