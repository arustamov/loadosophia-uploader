# loadosophia-uploader
Tool to upload JMeter test results to Loadosophia with command line CMDRunner

Example:
Assuming CMDRunner.jar, loadosophia-uploader.jar, token.txt and jmeter_results.csv are all in the current directory:
java -jar CMDRunner.jar --tool LoadosophiaUploader --token-file token.txt --data-file jmeter_results.csv --project-key JMeter_test
