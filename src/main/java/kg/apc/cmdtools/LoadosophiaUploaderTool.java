package kg.apc.cmdtools;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.loadosophia.jmeter.LoggingStatusNotifier;
import org.loadosophia.jmeter.LoadosophiaAPIClient;
import org.loadosophia.jmeter.LoadosophiaUploadResults;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
            else if (nextArg.equalsIgnoreCase("--jtl-file")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing JTL file");
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

        try {
            readUploadToken();
            LoadosophiaAPIClient apiClient = getAPIClient();
            LoadosophiaUploadResults uploadResult = apiClient.sendFiles(new File(dataFile), new LinkedList<String>());
            String redirectLink = uploadResult.getRedirectLink();
            log.info("Uploaded successfully, go to results: " + redirectLink);
            return 0;
        } catch (IOException ex) {
            log.error("Failed to upload results to loadosophia", ex);
            return 1;
        }
    }

    protected void showHelp(PrintStream os) {
        os.println("Options for tool 'Loadosophia Uploader': --generate-png <filename> "
                + "--token-file <token file> "
                + "--jtl-file <data file> "
                + "--project-key <project name> "
                + "[ "
                + "--color-flag <\"none\"/\"red\"/\"green\"/\"blue\"/\"gray\"/\"orange\"/\"violet\"/\"cyan\"/\"black\"> "
                + "--test-title <test title> "
                + "--log-level <<debug|info|warn|error>> "
                + "]");
    }

    protected LoadosophiaAPIClient getAPIClient() {
        return new LoadosophiaAPIClient(new LoggingStatusNotifier(), ADDRESS, uploadToken, projectKey, colorFlag, testTitle);
    }

    private void checkParams() {
        if (tokenFile == null) {
            throw new IllegalArgumentException("Missing token file");
        }
        if (!(new File(tokenFile).exists())) {
            throw new IllegalArgumentException("Cannot find specified token file: " + tokenFile);
        }
        if (dataFile == null) {
            throw new IllegalArgumentException("Missing JTL file");
        }
        if (!(new File(dataFile).exists())) {
            throw new IllegalArgumentException("Cannot find specified JTL file: " + dataFile);
        }
        if (projectKey == null) {
            throw new IllegalArgumentException("Missing project key");
        }
    }

    private void readUploadToken() throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(tokenFile));
        uploadToken = new String(encoded);
    }
}
