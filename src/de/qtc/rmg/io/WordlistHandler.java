package de.qtc.rmg.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import de.qtc.rmg.internal.MethodCandidate;
import javassist.CannotCompileException;
import javassist.NotFoundException;

/**
 * The WordlistHandler is responsible for reading and writing wordlist files. During read operations,
 * it also creates the corresponding MethodCandidates right away and during write operations, it
 * writes method hashes and meta information into the corresponding wordlist.
 *
 * @author Tobias Neitzel (@qtc_de)
 */
public class WordlistHandler {

    private String wordlistFile;
    private String wordlistFolder;
    private boolean updateWordlists;

    private final static String[] defaultWordlists = {"rmg.txt", "rmiscout.txt"};

    /**
     * Create a new WordlistHandler.
     *
     * @param wordlistFile wordlist file to use (if not null, takes priority over wordlist Folder)
     * @param wordlistFolder wordlist folder to look for wordlist files
     * @param updateWordlists whether wordlists should be updated to the advanced format
     */
    public WordlistHandler(String wordlistFile, String wordlistFolder, boolean updateWordlists)
    {
        this.wordlistFile = wordlistFile;
        this.wordlistFolder = wordlistFolder;
        this.updateWordlists = updateWordlists;
    }

    /**
     * Read the specified wordlist and return the corresponding MethodCandidates. Only uses a wordlist
     * file, if one was specified. Otherwise, it searches the specified wordlist folder.
     *
     * @return HashSet of MethodCandidates build from the wordlist
     * @throws IOException if an IO operation fails
     */
    public HashSet<MethodCandidate> getWordlistMethods() throws IOException
    {
        if( this.wordlistFile != null && !this.wordlistFile.isEmpty() ) {
            return getWordlistMethodsFromFile(this.wordlistFile, this.updateWordlists);

        } else if( this.wordlistFolder != null && !this.wordlistFolder.isEmpty() ) {
            return getWordlistMethodsFromFolder(this.wordlistFolder, this.updateWordlists);

        } else {
            return getWordlistMethodsFromStream();
        }
    }

    public static HashSet<MethodCandidate> getWordlistMethodsFromStream() throws IOException
    {
        HashSet<MethodCandidate> methods = new HashSet<MethodCandidate>();

        for(String wordlist : defaultWordlists) {

            Logger.printlnMixedBlue("Reading method candidates from internal wordlist", wordlist);
            Logger.increaseIndent();

            InputStream stream = WordlistHandler.class.getResourceAsStream("/wordlists/" + wordlist);
            String content = new String(IOUtils.toByteArray(stream));
            stream.close();

            methods.addAll(parseMethods(content.split("\n")));
            Logger.decreaseIndent();
        }

        return methods;
    }

    /**
     * Reads all files ending with .txt within the wordlist folder and returns the corresponding MethodCandidates.
     *
     * @return HashSet of MethodCandidates build from the wordlist files
     * @throws IOException if an IO operation fails
     */
    public static HashSet<MethodCandidate> getWordlistMethodsFromFolder(String folder, boolean updateWordlists) throws IOException
    {
        File wordlistFolder = new File(folder);
        if( !wordlistFolder.isDirectory() ) {
            throw new IOException("wordlist-folder " + wordlistFolder.getCanonicalPath() + " is not a directory.");
        }

        List<File> files = (List<File>)FileUtils.listFiles(wordlistFolder, new String[]{"txt", "TXT"}, false);
        Logger.printlnMixedBlueFirst(String.valueOf(files.size()), "wordlist files found.");

        HashSet<MethodCandidate> methods = new HashSet<MethodCandidate>();
        for(File file : files){
            methods.addAll(getWordlistMethodsFromFile(file.getCanonicalPath(), updateWordlists));
        }

        return methods;
    }

    /**
     * Parses a wordlist file for available methods and creates the corresponding MethodCandidates. Comments prefixed with '#'
     * within wordlist files are ignored. Each non comment line is split on the ';' character. If the split has a length of 1,
     * the ordinary wordlist format (that just contains the method signature) is assumed. If the length is 4 instead, it should
     * be the advanced format. Otherwise, we have an unknown format and print a warning. If updateWordlists was set within the
     * constructor, each wordlist file is updated to the advanced format after the parsing.
     *
     * @param file wordlist file to read in
     * @return HashSet of MethodCandidates build from the wordlist file
     * @throws IOException if an IO operation fails
     */
    public static HashSet<MethodCandidate> getWordlistMethodsFromFile(String filename, boolean updateWordlists) throws IOException
    {
        File file = new File(filename);

        Logger.printlnMixedBlue("Reading method candidates from file", file.getCanonicalPath());
        Logger.increaseIndent();

        String[] content = FileUtils.readLines(file, StandardCharsets.UTF_8).toArray(new String[0]);
        HashSet<MethodCandidate> methods = parseMethods(content);

        if(updateWordlists) {
            Logger.println("Updating wordlist file.");
            updateWordlist(file, methods);
        }

        Logger.decreaseIndent();
        return methods;
    }

    /**
     * Parses a wordlist file for available methods and creates the corresponding MethodCandidates. Comments prefixed with '#'
     * within wordlist files are ignored. Each non comment line is split on the ';' character. If the split has a length of 1,
     * the ordinary wordlist format (that just contains the method signature) is assumed. If the length is 4 instead, it should
     * be the advanced format. Otherwise, we have an unknown format and print a warning. If updateWordlists was set within the
     * constructor, each wordlist file is updated to the advanced format after the parsing.
     *
     * @param file wordlist file to read in
     * @return HashSet of MethodCandidates build from the wordlist file
     * @throws IOException if an IO operation fails
     */
    public static HashSet<MethodCandidate> parseMethods(String[] lines) throws IOException
    {
        HashSet<MethodCandidate> methods = new HashSet<MethodCandidate>();

        for(String line : lines) {

            if( line.trim().startsWith("#") || line.trim().isEmpty() ) {
                continue;
            }

            line = line.trim().replaceAll(" +", " ").replaceAll(" *, *", ", ").replaceAll("\\<[^>]+\\>", "");
            String[] split = line.split(";");

            try {
                if(split.length == 1)
                    methods.add(new MethodCandidate(split[0].trim()));

                else if(split.length == 4)
                    methods.add(new MethodCandidate(split[0].trim(), split[1].trim(), split[2].trim(), split[3].trim()));

                else {
                    Logger.eprintlnMixedYellow("Encountered unknown method format:", line);
                    Logger.eprintln("Skipping this signature");
                }

            } catch(CannotCompileException | NotFoundException e) {
                Logger.eprintlnMixedYellow("Caught Exception while processing", line);
                Logger.eprintln("Skipping this signature");
            }
        }

        Logger.printlnMixedYellowFirst(String.valueOf(methods.size()), "methods were successfully parsed.");
        return methods;
    }

    /**
     * Write MethodCandidates with their advanced format to a wordlist.
     *
     * @param file destination to write the advanced wordlist file
     * @param methods MethodCandidates to write into the wordlist
     * @throws IOException if an IO operation fails
     */
    public static void updateWordlist(File file, HashSet<MethodCandidate> methods) throws IOException
    {
        List<String> signatures = new ArrayList<String>();
        for(MethodCandidate method : methods) {
            signatures.add(method.convertToString());
        }

        FileUtils.writeLines(file, signatures);
    }
}
