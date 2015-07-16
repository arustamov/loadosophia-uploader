# loadosophia-uploader
Tool to upload JMeter test results to Loadosophia with command line CMDRunner


Usage:

There are 3 possible ways to run LoadosophiaUploader:

1. Single JTL file
2. Single JTL file with corresponding PerfMon file(s)
3. Multiple JTL files


Note for data file(s) path param:

Glob pattern lookup is supported while specifying path to data file(s)


How to run:

Assuming CMDRunner.jar, loadosophia-uploader.jar, token.txt and jmeter_results.csv are all in the current directory with all neccessary dependencies in classpath (e.g. $JMETER_HOME/lib/ext):
java -jar CMDRunner.jar --tool LoadosophiaUploader --token-file token.txt --jtl-file jmeter_results.csv --project-key JMeter_test
