package Model;

import View.Main;
import java.io.*;
import java.util.*;

public class Indexer {
    public static TreeMap<String, CityTerm> citiesDictionary;
    public static TreeMap<String, Term> termsDictionary;
    private String pathForWriting;
    private boolean useStemming;

    public Indexer() {
        citiesDictionary = new TreeMap<>(new StringComparator());
        termsDictionary = new TreeMap<>(new StringComparator());
        pathForWriting = null;
        useStemming=false;
    }

    /**
     * returns the number of terms discovered across the corpus.
     * @return number of unique terms in corpus.
     */
    public int getNumberOfTerms() {
        return termsDictionary.size();
    }

    // set the given stemming value
    public void setStemming(boolean useStemming){
        this.useStemming=useStemming;
    }
    /**
     * set the path to the given path.
     * @param dictionaryAndPostingPath- the path to which the postings and dictionaries files will be written.
     */
    public void setPath(String dictionaryAndPostingPath) {
        pathForWriting = dictionaryAndPostingPath;
    }

    /**
     * @return the path to whom we write the postings files and dictionaries.
     */
    public String getPath(){
        return pathForWriting;
    }

    /**
     * A function that reset all the data structure using in this class
     */
    public boolean resetData() {
        this.termsDictionary = new TreeMap<>(new StringComparator());
        citiesDictionary = new TreeMap<>(new StringComparator());
        return deleteAllFilesInDisk();
    }

    /**
     * delete all files in the given current path, including posting files and dictionaries.
     */
    private boolean deleteAllFilesInDisk() {
        try {
            final File folder = new File(pathForWriting);
            final File[] files = folder.listFiles();
            for (final File file : files) {
                file.delete();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * compares between two strings while ignoring upper cases.
     * meaning, two string, one in upper case and the other in lower case, will be valuated as equals.
     */
    static class StringComparator implements Comparator<String> {
        public int compare(String o1, String o2) {
            int comparison = 0;
            int c1, c2;
            for (int i = 0; i < o1.length() && i < o2.length(); i++) {
                c1 = (int) o1.toLowerCase().charAt(i);
                c2 = (int) o2.toLowerCase().charAt(i);
                comparison = c1 - c2;
                if (comparison != 0)
                    return comparison;
            }
            if (o1.length() > o2.length())    // See note 4
                return 1;
            else if (o1.length() < o2.length())
                return -1;
            else
                return 0;
        }
    }

    /*
    initiate the scanners so each scanner will be set to one of the temporal posting files.
    initialize the posting files line array so the first line from each temporal posting file will be pulled.
     */
    private void initializeStructuresForMerge(String[] postingFilesLines, Scanner[] scanners) {
        //init scanners
        for (int i = 0; i < scanners.length; i++) {
            try {
                File file = new File(pathForWriting + "\\file_" + i);
                scanners[i] = new Scanner(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //read first line from each temporal posting file
        for (int j = 0; j < postingFilesLines.length; j++) {
            if (scanners[j].hasNext()) {
                postingFilesLines[j] = scanners[j].nextLine();
            }
        }
    }

    /**
     * Merges between all temporal posting files which were created before and creates the dictionary.
     * assumption: all temporal posting files are sorted by (char, numbers,abc..).
     */
    public void createInvertedIndex()throws Exception{
        int filesLeftToMerge = Parse.postingFileId;
        String[] postingFilesLines = new String[Parse.postingFileId];
        Scanner[] scanners = new Scanner[Parse.postingFileId];
        initializeStructuresForMerge(postingFilesLines, scanners);
        TreeSet<String> termsPQueue = new TreeSet<String>(new StringComparator());
        //add current terms from each line to the priority queue.
        for (String str : postingFilesLines) {
            if (str != null)
                termsPQueue.add(str.substring(0, str.indexOf(";")));
            else
                filesLeftToMerge--;
        }
        RandomAccessFile writer = null, citiesWriter = null;
        StringBuilder recordInUnitedPosting = new StringBuilder();
        String recentPrefix = "";
        long lineInPostingFile = 0;
        //merge equal terms
        while (filesLeftToMerge != 0 && !termsPQueue.isEmpty()) {
            recordInUnitedPosting.setLength(0);
            String currentTermToWrite = termsPQueue.pollFirst();
            boolean isTermUpperCase = true, isTermCity = false;
            for (int j = 0; j < postingFilesLines.length; j++) {
                if (postingFilesLines[j] != null) {
                    int separatorIndex = postingFilesLines[j].indexOf(";");
                    String termToCompare = postingFilesLines[j].substring(0, separatorIndex);
                    if (currentTermToWrite.equals(termToCompare) || currentTermToWrite.equals(termToCompare.toLowerCase()) || currentTermToWrite.equals(termToCompare.toUpperCase())) {
                        if (isTermUpperCase && termToCompare.equals(termToCompare.toLowerCase())) {
                            isTermUpperCase = false;
                            currentTermToWrite = currentTermToWrite.toLowerCase();
                        }
                        recordInUnitedPosting.append(postingFilesLines[j].substring(separatorIndex + 1) + " ");
                        if (scanners[j].hasNext()) {
                            postingFilesLines[j] = scanners[j].nextLine();
                            termsPQueue.add(postingFilesLines[j].substring(0, postingFilesLines[j].indexOf(";")));
                        } else {
                            postingFilesLines[j] = null;
                            filesLeftToMerge--;
                        }
                    }
                }
            }
            String[] postingDataSplited = Main.split(recordInUnitedPosting.toString(), " ");
            recordInUnitedPosting.insert(0, currentTermToWrite + ";");
            try {
                if(useStemming && citiesWriter==null)
                      citiesWriter = new RandomAccessFile(new File(pathForWriting + "\\citiesPosting_stemmed"),"rw");
                else if (!useStemming && citiesWriter==null)
                    citiesWriter= new RandomAccessFile(new File(pathForWriting + "\\citiesPosting"),"rw");
            } catch (Exception e) {
                e.printStackTrace();
            }
            //write to relevant posting
            try {
                if (!returnDestinatoinPostFile(currentTermToWrite).equals(recentPrefix)) {
                    recentPrefix = returnDestinatoinPostFile(currentTermToWrite);
                    if (writer != null)
                        writer.close();
                    if(useStemming)
                       writer = new RandomAccessFile(new File(pathForWriting + "\\" + recentPrefix+"_stemmed"),"rw");
                    else
                        writer = new RandomAccessFile(new File(pathForWriting + "\\" + recentPrefix),"rw");
                }
                if (citiesDictionary.containsKey(currentTermToWrite.toUpperCase())) {
                    lineInPostingFile=citiesWriter.getFilePointer();
                    citiesWriter.writeBytes(recordInUnitedPosting.toString() + "\n");
                    isTermCity = true;
                } else {
                    lineInPostingFile=writer.getFilePointer();
                    writer.writeBytes(recordInUnitedPosting.toString() + "\n");
                }
                //add term to dictionary
                int termDf = postingDataSplited.length;
                int totalTf =getTotalTf(postingDataSplited);
                if (isTermCity) {
                    CityTerm t = citiesDictionary.get(currentTermToWrite.toUpperCase());
                    t = new CityTerm(t);
                    t.setDf(termDf);
                    t.setCorpusTf(totalTf);
                    t.setPtr(lineInPostingFile);
                    citiesDictionary.put(currentTermToWrite, t);
                } else {
                    termsDictionary.put(currentTermToWrite, new Term(currentTermToWrite, termDf, totalTf, lineInPostingFile));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } // while
        try {
            writer.close();
            citiesWriter.close();
            for (int i = 0; i < scanners.length; i++)
                scanners[i].close();
        } catch (Exception e) {
            //e.printStackTrace}
        }
        Parse.postingFileId = 0;
        deleteTemporalFiles();
    }


    /**
     * returns the total appearances of the term throughout the corpus.
     */
    private int getTotalTf(String[] postingDataSplited){
        int totalTF = 0;
        for (int i = 0; i < postingDataSplited.length; i++) {
            totalTF += Integer.valueOf(postingDataSplited[i].substring(postingDataSplited[i].lastIndexOf("*") + 1));
        }
        return totalTF;
    }

    /**
     * when merge is done, all temporal postings files, which were required for the merge process, will be deleted.
     */
    private void deleteTemporalFiles() {
        final File folder = new File(pathForWriting);
        final File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File pathname, String name) {
                return name.startsWith("file_");
            }
        });
        for (final File file : files) {
            file.delete();
        }
    }

    /**
     * Returns the file into we need to write the term and it's posting.
     * @param term - the term for whom we wish to discover the relevant posting.
     * @return - a-z,0-9 or cities according to the prefix of given term.
     */
    private String returnDestinatoinPostFile(String term) {
        char prefix = term.toLowerCase().charAt(0);
        if (!Character.isDigit(prefix) && !Character.isLetter(prefix))
            return "chars";
        return String.valueOf(prefix);
    }

    /**
     * Loads the terms dictionary to disk.
     */
    public void loadTermsDictionaryIntoDisk(boolean useStemming) {
        String currenTermToLoadToDisk;
        Term termData;
        BufferedWriter dictionaryWriter=null;
        try {
            if (useStemming && dictionaryWriter==null)
                dictionaryWriter = new BufferedWriter(new FileWriter(pathForWriting + "\\terms_dictionary_stemmed"));
            else if (!useStemming && dictionaryWriter==null)
                dictionaryWriter = new BufferedWriter(new FileWriter(pathForWriting + "\\terms_dictionary"));
            for (Map.Entry<String, Term> entry : termsDictionary.entrySet()) {
                currenTermToLoadToDisk = entry.getKey();
                termData = entry.getValue();
                dictionaryWriter.write(currenTermToLoadToDisk + ";" + termData.getDf() + "|" +
                        termData.getCorpusTf() + "|" + termData.getPtr() + "\n");
            }
            dictionaryWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the cities dictionary to disk.
     */
    public void loadCitiesDictionaryIntoDisk() {
        String currentCityTermToLoadToDisk;
        try {
            BufferedWriter dictionaryWriter=null;
            if(useStemming && dictionaryWriter==null)
                 dictionaryWriter = new BufferedWriter(new FileWriter(pathForWriting + "\\cities_dictionary_stemmed"));
            else if (!useStemming && dictionaryWriter==null)
                dictionaryWriter = new BufferedWriter(new FileWriter(pathForWriting + "\\cities_dictionary"));
            //dictionaryWriter.write("city;df|corpusTf|ptrToPosting|country|currency|populationSize+\n");
            for (Map.Entry<String, CityTerm> entry : citiesDictionary.entrySet()) {
                currentCityTermToLoadToDisk = entry.getKey();
                CityTerm city = entry.getValue();
                dictionaryWriter.write(currentCityTermToLoadToDisk + ";" + city.getDf() + "|" + city.getCorpusTf() + "|" + city.getPtr() + "|" + city.getCountry()
                        + "|" + city.getCurrency() + "|" + city.getPopulationSize() + "\n");
            }
            dictionaryWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // more methods for the gui(!)


    /**
     * clear dictionaries.
     */
    public void clearStructors() {
        citiesDictionary.clear();
        termsDictionary.clear();
    }

    /**
     * returns a sub dictionary for purposes of display only.
     * @return sub dictionary containing all terms and their total frequency in the corpus.
     */
    public List<String> getDictionaryForDisplay(boolean useStemming) {
        List<String> dictionaryForDisplay= new ArrayList<>();
        try {
            if (termsDictionary.isEmpty())
                loadTermsDictionaryToMainMemory(useStemming);
            for (Map.Entry<String,Term> entry: termsDictionary.entrySet()) {
                dictionaryForDisplay.add(entry.getKey()+","+entry.getValue().getCorpusTf());
            }
            return dictionaryForDisplay;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * loads all the city dictionary and the terms dictionary (according to the useStemming argument) into main memory.
     * @param useStemming - a boolean parameter indicating whether we wish to load the dictionary after stemming or
     *                   without stemming
     * @return true- if process succeeded and false otherwise.
     */
    public boolean loadDictionariesToMainMemory(boolean useStemming) {
        try {
            return loadCitiesDictionaryToMainMemory()&& loadTermsDictionaryToMainMemory(useStemming);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * loads the cities dictionary into main memory.
     * @return True- if the process succeeded or false otherwise.
     */
    private boolean loadCitiesDictionaryToMainMemory() {
        // dictionaryWriter.write("city;df|corpusTf|ptrToPosting|country|currency|populationSize+\n");
        citiesDictionary.clear();
        String city, df;
        File citiesDictionary=null;
        if(useStemming)
            citiesDictionary = new File(pathForWriting + "\\cities_dictionary_stemmed");
        else
            citiesDictionary = new File(pathForWriting + "\\cities_dictionary");
        if (citiesDictionary.exists()) {
            try {
                Scanner scanner = new Scanner(citiesDictionary);
                while (scanner.hasNext()) {
                    String[] line = Main.split(scanner.nextLine(), "|");
                    city = line[0].substring(0, line[0].indexOf(";"));
                    df = line[0].substring(line[0].indexOf(";") + 1);
                    Indexer.citiesDictionary.put(city, new CityTerm(city, Integer.valueOf(df), Integer.valueOf(line[1]), Integer.valueOf(line[2]), line[3], line[4], line[5]));
                }
                scanner.close();
            } catch (Exception e) {
                return false;
            }
        } else
            return false;
        return true;
    }

    /**
     * loads the terms dictionary into main memory.
     * @param useStemming- indicating which dictionary we want to pull- the one with stemming or without.
     * @return True- if the process succeeded or false otherwise.
     */
    private boolean loadTermsDictionaryToMainMemory(boolean useStemming) {
        try {
            termsDictionary.clear();
            File terms_dictionary;
            if (useStemming)
                terms_dictionary = new File(pathForWriting + "\\terms_dictionary_stemmed");
            else
                terms_dictionary = new File(pathForWriting + "\\terms_dictionary");
            if (terms_dictionary.exists()) {
                String term, df;
                Scanner scanner = new Scanner(terms_dictionary);
                while (scanner.hasNext()) {
                    String[] line = Main.split(scanner.nextLine(), "|");
                    term = line[0].substring(0, line[0].indexOf(";"));
                    df = line[0].substring(line[0].indexOf(";") + 1);
                    termsDictionary.put(term, new Term(term, Integer.valueOf(df), Integer.valueOf(line[1]), Integer.valueOf(line[2])));
                }
                scanner.close();
            }
            else
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * A function that returns all the cities from the cities dictionary, for showing in the GUI
     * @return observableList of all cities in the corpus
     */
    public Set<String> getCities() {
        loadCitiesDictionaryToMainMemory();
        return citiesDictionary.keySet();
    }
}



