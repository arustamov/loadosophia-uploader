package kg.apc.cmdtools;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.loadosophia.jmeter.LoggingStatusNotifier;
import org.loadosophia.jmeter.LoadosophiaAPIClient;
import org.loadosophia.jmeter.LoadosophiaUploadResults;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by Anton Rustamov (arustamov) on 4/1/2015.
 */
public class LoadosophiaUploaderTool extends AbstractCMDTool {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final String ADDRESS = "https://loadosophia.org/";

    private String tokenFile;
    private String uploadToken;
    private String dataFile;
    private String projectKey;
    private String colorFlag = LoadosophiaAPIClient.COLOR_NONE;
    private String testTitle = "";
    private LoadosophiaAPIClient apiClient;

    protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
        LoggingManager.setPriority(Priority.INFO);

        while (args.hasNext()) {
            String nextArg = (String) args.next();
            log.debug("Arg: " + nextArg);
            if (nextArg.equalsIgnoreCase("--token-file")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing token file");
                }
                tokenFile = (String) args.next();
            }
            else if (nextArg.equalsIgnoreCase("--data-file")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing data file");
                }
                dataFile = (String) args.next();
            }
            else if (nextArg.equalsIgnoreCase("--project-key")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing project key");
                }
                projectKey = (String) args.next();
            }
            else if (nextArg.equalsIgnoreCase("--color-flag")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing color flag");
                }
                colorFlag = (String) args.next();
                if (!Arrays.asList(LoadosophiaAPIClient.colors).contains(colorFlag)) {
                    throw new IllegalArgumentException("Illegal color flag");
                }
            }
            else if (nextArg.equalsIgnoreCase("--test-title")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing test title");
                }
                testTitle = (String) args.next();
            }
            else if (nextArg.equals("--log-level")) {
                args.remove();
                String logLevel = (String) args.next();
                LoggingManager.setPriority(logLevel);
            }
            else {
                throw new UnsupportedOperationException("Unrecognized option: " + nextArg);
            }
        }

        checkParams();
        checkDataFileNotEmpty();
        readUploadToken();
        initAPIClient();
        int result = doUpload();
        return result;
    }

    protected int doUpload() {
        try {
            log.info("Going to upload data file: " + dataFile + " to Loadosophia");
            LoadosophiaUploadResults uploadResult = apiClient.sendFiles(new File(dataFile), new LinkedList<String>());
            String redirectLink = uploadResult.getRedirectLink();
            log.info("Data file: " + dataFile + " uploaded successfully. Go to results: " + redirectLink);
            return 0;
        } catch (IOException ex) {
            log.error("Failed to upload data file: " + dataFile + " to Loadosophia", ex);
            return 1;
        }
    }

    protected void showHelp(PrintStream os) {
        os.println("Options for tool 'Loadosophia Uploader': --generate-png <filename> "
                + "--token-file <token file> "
                + "--data-file <data file> "
                + "--project-key <project name> "
                + "[ "
                + "--color-flag <\"none\"/\"red\"/\"green\"/\"blue\"/\"gray\"/\"orange\"/\"violet\"/\"cyan\"/\"black\"> "
                + "--test-title <test title> "
                + "--log-level <<debug|info|warn|error>> "
                + "]");
    }

    protected void initAPIClient() {
        apiClient = new LoadosophiaAPIClient(new LoggingStatusNotifier(), ADDRESS, uploadToken, projectKey, colorFlag, testTitle);
    }

    private void checkParams() {
        if (tokenFile == null) {
            throw new IllegalArgumentException("Missing token file");
        }
        if (!(new File(tokenFile).exists())) {
            throw new IllegalArgumentException("Cannot find specified token file: " + tokenFile);
        }
        if (dataFile == null) {
            throw new IllegalArgumentException("Missing path to data file(s) to upload");
        }
        if (projectKey == null) {
            throw new IllegalArgumentException("Missing project key");
        }
    }

    private void checkDataFileNotEmpty() {
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(dataFile));
            while (reader.readLine() != null) {
                int lineNumber = reader.getLineNumber();
                if (lineNumber > 1) return;
            }
            throw new RuntimeException("Empty data set read from file");
        }
        catch (FileNotFoundException ex) {
            throw new RuntimeException("Cannot find specified data file: " + dataFile, ex);
        }
        catch (IOException ex) {
            throw new RuntimeException("Failed to read specified data file: " + dataFile, ex);
        }
    }

    private void readUploadToken() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(tokenFile));
            uploadToken = new String(encoded);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read specified token file: " + tokenFile, ex);
        }
    }
}
