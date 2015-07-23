package kg.apc.cmdtools;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.loadosophia.jmeter.LoggingStatusNotifier;
import org.loadosophia.jmeter.LoadosophiaAPIClient;
import org.loadosophia.jmeter.LoadosophiaUploadResults;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by Anton Rustamov (arustamov) on 4/1/2015.
 */
public class LoadosophiaUploaderTool extends AbstractCMDTool {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final String ADDRESS = "https://loadosophia.org/";

    private String tokenFile;
    private String uploadToken;
    private String jtlFilePath;
    private boolean singleJtlFile;
    private String perfMonFilePath;
    private String projectKey;
    private String colorFlag = LoadosophiaAPIClient.COLOR_NONE;
    private String testTitle = "";
    private LoadosophiaAPIClient apiClient;

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
                    throw new IllegalArgumentException("Missing jtl file");
                }
                jtlFilePath = (String) args.next();
                singleJtlFile = true;
            }
            else if (nextArg.equalsIgnoreCase("--jtl-files")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing jtl files");
                }
                jtlFilePath = (String) args.next();
                singleJtlFile = false;
            }
            else if (nextArg.equalsIgnoreCase("--perf-mon-file")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Missing perf mon file");
                }
                perfMonFilePath = (String) args.next();
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
        LinkedList<File> jtlFiles = findDataFiles(jtlFilePath);
        removeEmptyDataFiles(jtlFiles);

        if(jtlFiles.isEmpty()) {
            throw new RuntimeException("No jtl data files found");
        }

        readUploadToken();
        initAPIClient();

        if (singleJtlFile) {
            File jtlFile = jtlFiles.getFirst();
            LinkedList<File> perfMonDataFiles = new LinkedList<File>();
            if (perfMonFilePath != null) {
                perfMonDataFiles = findDataFiles(perfMonFilePath);
                removeEmptyDataFiles(perfMonDataFiles);
            }
            return doUpload(jtlFile, perfMonDataFiles);
        }
        else {
            return doUpload(jtlFiles);
        }
    }

    protected int doUpload(LinkedList<File> dataFiles) {
        int errors = 0;
        log.info(String.format("Going to upload %d not empty data files", dataFiles.size()));
        for (File dataFile : dataFiles) {
            int status = doUpload(dataFile);
            errors += status;
        }
        log.info(String.format("Successfully uploaded %d data files", dataFiles.size() - errors));
        return errors;
    }

    protected int doUpload(File dataFile) {
        return doUpload(dataFile, new LinkedList<File>());
    }

    protected int doUpload(File dataFile, LinkedList<File> perfMonDataFiles) {
        LinkedList<String> perfMonDataFilesPath = getPerfMonDataFilesPaths(perfMonDataFiles);
        try {
            log.info(String.format("Going to upload data file: %s", dataFile));
            LoadosophiaUploadResults uploadResult = apiClient.sendFiles(dataFile, perfMonDataFilesPath);
            String redirectLink = uploadResult.getRedirectLink();
            log.info(String.format("Successfully uploaded data file: %s", dataFile));
            log.info(String.format("Go to results: %s", redirectLink));
            return 0;
        } catch (IOException ex) {
            log.error("Failed to upload data file: " + dataFile + " to Loadosophia", ex);
            return 1;
        }

    }

    private LinkedList<String> getPerfMonDataFilesPaths(LinkedList<File> perfMonDataFiles) {
        LinkedList<String> perfMonDataFilesPaths = new LinkedList<String>();
        for (File perfMonDataFile : perfMonDataFiles) {
            perfMonDataFilesPaths.add(perfMonDataFile.getPath());
        }
        return perfMonDataFilesPaths;
    }

    protected void checkParams() {
        if (tokenFile == null) {
            throw new IllegalArgumentException("Missing token file");
        }
        if (!(new File(tokenFile).exists())) {
            throw new IllegalArgumentException("Cannot find specified token file: " + tokenFile);
        }
        if (jtlFilePath == null) {
            throw new IllegalArgumentException("Missing path to data file(s) to upload");
        }
        if (projectKey == null) {
            throw new IllegalArgumentException("Missing project key");
        }
    }

    protected void initAPIClient() {
        apiClient = new LoadosophiaAPIClient(new LoggingStatusNotifier(), ADDRESS, uploadToken, projectKey, colorFlag, testTitle);
    }

    private LinkedList<File> findDataFiles(String filePath) {
        final LinkedList<File> dataFiles = new LinkedList<File>();
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePath);
        try {
            Files.walkFileTree(getStartPath(filePath, true), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(path)) {
                        log.info(String.format("Found data file: %s", path));
                        dataFiles.add(path.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error("I/O error in visitor", ex);
        }
        log.info(String.format("Total count of data files found: %d", dataFiles.size()));
        return dataFiles;
    }

    private Path getStartPath(String path, boolean parent) {
        Path startPath = null;
        try {
            Path inputPath = Paths.get(path);
            if (inputPath.isAbsolute()) {
                startPath = parent? inputPath.getParent() : inputPath;
            } else {
                throw new RuntimeException(String.format("%s data file path is not absolute", path));
            }
        }
        catch (InvalidPathException ex) {
            int globIndex = ex.getIndex();
            String newPath = path.substring(0, globIndex);
            startPath = getStartPath(newPath, false);
        }

        return startPath;
    }

    private void removeEmptyDataFiles(LinkedList<File> dataFiles) {
        CollectionUtils.filter(dataFiles, new Predicate() {
            @Override
            public boolean evaluate(Object dataFile) {
                try {
                    boolean isEmpty = true;
                    LineNumberReader reader = new LineNumberReader(new FileReader((File) dataFile));
                    while (reader.readLine() != null) {
                        int lineNumber = reader.getLineNumber();
                        if (lineNumber > 1)  {
                            isEmpty = false;
                            break;
                        }
                    }
                    return !isEmpty;
                } catch (IOException ex) {
                    throw new RuntimeException(String.format("Failed to read specified data file: %s", dataFile), ex);
                }
            }
        });
    }

    private void readUploadToken() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(tokenFile));
            uploadToken = new String(encoded);
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to read specified token file: %s", tokenFile), ex);
        }
    }
}
