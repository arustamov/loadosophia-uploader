# loadosophia-uploader
Tool to upload JMeter test results to Loadosophia with command line CMDRunner

Example:
Assuming CMDRunner.jar and loadosophia-uploader.jar are both in current directory:
java -jar CMDRunner.jar --tool LoadosophiaUploader --token-file token.txt --jtl-file jmeter_results.csv --project-key JMeter_test
