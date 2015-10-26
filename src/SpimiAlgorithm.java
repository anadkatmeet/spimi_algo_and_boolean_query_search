import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;


/*please use three folders to store input files, intermediate files(dictionaries) and output files
 * viz,input_files, intermediate_files, output_file
 * */
public class SpimiAlgorithm {

	static ArrayList<String> stopWords=new ArrayList<>(Arrays.asList("a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in",
			"is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with"));
	
	static TreeMap<String, TreeSet<Integer>> postings=new TreeMap<>();
	
	int memorySize;
	int intermediateFileCount=0;
	final static double TOTAL_INPUT_FILES=22;
	long startTime,stopTime;	//to store time in millis
	
	
	public static void main(String[] args){
		showMenu();
	}
	
	private static void showMenu(){
		System.out.println("Information Retrieval Project 1");
		System.out.println("--------------------------------");
		
		Scanner inputChoice=new Scanner(System.in);
		Scanner inputQuery=new Scanner(System.in);
		SpimiAlgorithm start=new SpimiAlgorithm();
		int choice;
		
		while(true){
			System.out.println("\nSelect 1 - Execute SPIMI algorithm");
			System.out.println("Select 2 - Search a query");
			System.out.println("Select 3 - Exit");
			choice=inputChoice.nextInt();
			if (choice==1) {
				start.initialize();
			}else if(choice==2){
				System.out.println("Enter a query to search");
				String query=inputQuery.nextLine();
				start.searchQuery(query);
			}else if(choice==3){
				return;
			}
			else {
				System.out.println("Wrong choice");
				continue;
			}
		}
	}
	private void initialize(){
		System.out.println("Welcome to spimi algorithm");
		System.out.println("--------------------------");
		System.out.println("In this algorithm, 1 block = 1 file and input data set contains 22 files or 22 blocks");
		System.out.println("Please select memory size in terms of blocks");
		
		//reading total number of memory blocks
		Scanner input=new Scanner(System.in);
		memorySize=input.nextInt();
		
		while(true){
			if (memorySize>0) {
				break;
			} else {
				System.out.println("Wrong choice\nPlease select memory size between 1 to 22");
				memorySize=input.nextInt();
				continue;
			}
		}
		
		startSPIMIPhaseOne();
		
	}
	
	private void startSPIMIPhaseOne(){
		startTime=System.currentTimeMillis();
		//calculating how many iterations needed for executing phase one
		intermediateFileCount=(int)(Math.ceil(TOTAL_INPUT_FILES/memorySize));
		System.out.println("This will generate "+intermediateFileCount+" intermediate files");
		
		System.out.println("-------------------");
		System.out.println("Executing phase one");
		System.out.println("-------------------");
		
		//remove previous files if exist
		deleteDir(new File("output_file"));
		deleteDir(new File("intermediate_files"));
		
		File folder = new File("input_files/");
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> fileList=new ArrayList<>();
		for (File tempfile : listOfFiles) {
		    if (tempfile.isFile()) {
		    	fileList.add(tempfile);
		    }
		}
		
		Iterator<File> fileIterator=fileList.iterator();
		for (int i = 0; i < intermediateFileCount; i++) {	//total iterations
			postings.clear();
			for (int j = 0; j < memorySize; j++) {			//loop for each block
				if (fileIterator.hasNext()) {
					readOneBlock(fileIterator.next());
				}
			}
			//writing each block to disk
			new File("intermediate_files").mkdir();
			String tempFileName="intermediate_files/output"+i+".txt";
			writePostingsToFile(tempFileName);
			System.out.println("done writing "+tempFileName+" to disk");
		}
		
		System.out.println("-------------------");
		System.out.println("Done with phase one");
		System.out.println("-------------------");
		
		//call to phase two; merging
		startSPIMIPhaseTwo();
		
	}
	
	private void readOneBlock(File file){
		//postings.clear();
		Document doc;
		try {
			doc = Jsoup.parse(file,"utf-8");
		
		
			Document doc1 = Jsoup.parse(doc.toString(), "", Parser.xmlParser());
	        
	        doc1.select("title").remove();
	        doc1.select("dateline").remove();
	        Elements textList=doc1.select("reuters text");	//array of texts
	        Elements reutersList=doc1.select("reuters");	//array of documents
	        
	        for (int i = 0; i < textList.size(); i++) {
	        	String section=textList.get(i).text();		//body of each document
	        	String id=reutersList.get(i).attr("NEWID");	//doc id of each document
	        	generateTokensForEachDocument(section, Integer.parseInt(id));	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void startSPIMIPhaseTwo(){
		System.out.println("Executing phase two");
		System.out.println("-------------------");
		System.out.println("Merging "+intermediateFileCount+" blocks");
		postings.clear();
		
		ArrayList<BufferedReader> list=new ArrayList<>();
		File folder = new File("intermediate_files/");
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> fileList=new ArrayList<>();
		for (File tempfile : listOfFiles) {
		    if (tempfile.isFile()) {
		    	fileList.add(tempfile);
		    }
		}
		
		
		BufferedReader[] br=new BufferedReader[fileList.size()];
		
		//initialize all the buffer readers for each block in disk
		for (int i = 0; i < fileList.size(); i++) {
			try {
				br[i]=new BufferedReader(new FileReader(fileList.get(i)));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			list.add(br[i]);
		}
		String sCurrentLine;
		Iterator<BufferedReader> itr;
		int count=list.size();
		while(count>0){			//till all the blocks are exhausted
			itr=list.iterator();
			while(itr.hasNext()){		//reading one line from each block
				
				BufferedReader tempbuffer=itr.next();
				try {
					if ((sCurrentLine=tempbuffer.readLine())!=null) {
						//convert each line which is as a string to key,<list of values> pair
						extractAndStoreToMap(sCurrentLine);			
						
					}else {
						itr.remove();
						count--;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		}
		//finally write the final inverted index to disk
		new File("output_file").mkdir();
		String finalFileName="output_file/FinalInvertedIndex.txt";
		writePostingsToFile(finalFileName);
		
		System.out.println("-------------------");
		System.out.println("Done with phase two");
		System.out.println("-------------------");
		stopTime=System.currentTimeMillis();
		System.out.println("Total time required to execute SPIMI algorithm with current memory constraint is : "+(stopTime-startTime)/1000+"secs");
	}
	
	
	private void generateTokensForEachDocument(String section, int id) {
		String[] words=section.split(" ");
		//generating tokens from the supplied document
		for (String str : words) {
			if ( str==null || str.equals("") || str.equals(" ")) {
				return;
			}
			str=normalize(str);
				
			if(str!=null){								//if its a stop word, then it will be returned null
				//now add to postings
				if (postings.containsKey(str)) {		//if term already present; then append the doc Id to existing doc id list
					postings.get(str).add(id);
				}
				else {									//if term is seen for the first time
					TreeSet<Integer> tempSet=new TreeSet<>();	
					tempSet.add(id);
					postings.put(str, tempSet);
				}
			}
		}//for loop ends
	}
	
	//normalizing each token
	private String normalize(String str){
		str=str.trim();
		str=str.toLowerCase();		//case folding
		str = str.replaceAll("[0-9]","");	//number removal
		str = str.replaceAll("[^a-zA-Z]", "");
		str = str.replaceAll("\\r", "");
		str=str.replaceAll("\\n", "");
		str=str.replaceAll("\\t", "");
		str=str.replaceAll("\t", "");
		str=str.replaceAll(" ", "");
		str = str.replace("\n", "").replace("\r", "");
		if (stopWords.contains(str)) {
			return null;
		}
		return str;
	}

	private void writePostingsToFile(String filename){
		//writing postings from memory to disk
		FileWriter fstream;
		try {
			fstream = new FileWriter(filename);
		
			BufferedWriter out = new BufferedWriter(fstream);
	
			for (Entry<String, TreeSet<Integer>> entry : postings.entrySet()) {
				if (!(entry.getKey()==null || entry.getKey().equals("") || entry.getKey().equals(" "))) {
				     out.write(entry.getKey() + "\t" + entry.getValue()+"\n");
				     out.flush();   // Flush the buffer and write all changes to the disk
				}
			 }
			out.close();    // Close the file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void searchQuery(String query){
		System.out.println("Searching documents containing given query: '"+query+"'");	
		try {
			BufferedReader br=new BufferedReader(new FileReader("output_file/FinalInvertedIndex.txt"));
			//reading file contents to map
			String sCurrentLine;
			//generate postings from inverted index
			while ((sCurrentLine=br.readLine())!=null) {
				extractAndStoreToMap(sCurrentLine);
			}
			
			//generate boolean query
			String[] queryTokens=query.split(" ");
			TreeSet<Integer> resultSet=new TreeSet<>();
			for (int i = 0; i < queryTokens.length; i++) {
				
				//normalize the word before searching the dictionary
				queryTokens[i]=normalize(queryTokens[i]);	
				
				if (queryTokens[i]!=null) {
					if (resultSet.isEmpty()) {		//enter all the doc ids for the first term
						if (postings.containsKey(queryTokens[i])) {
							resultSet.addAll(postings.get(queryTokens[i]));
						}
						continue;
					}
					if (postings.containsKey(queryTokens[i])) {		//intersect the doc ids with the existing ones
						resultSet.retainAll(postings.get(queryTokens[i]));
					}
					else {
						continue;
					}
				}
				
			}
			if (resultSet.isEmpty()) 
				System.out.println("No documents found matching above query");
			else
				System.out.println("Documents found : "+resultSet.size()+"\nDocuments are : "+resultSet);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void extractAndStoreToMap(String str){
		TreeSet<Integer> tempTreeSet=new TreeSet<>();
		String[] values=str.split("\t");
		values[1]=values[1].substring(1);
		values[1]=values[1].substring(0, values[1].length()-1);
		String[] numbers=values[1].split(",");
		for (int i = 0; i < numbers.length; i++) {
			numbers[i]=numbers[i].trim();
			//System.out.println(Integer.parseInt(numbers[i]));
			tempTreeSet.add(Integer.parseInt(numbers[i]));
		}
		
		//check before adding, if its there then add the tempSet with the existing TreeSet in the map
		if (postings.containsKey(values[0])) {
			TreeSet<Integer> newTreeSet=postings.get(values[0]);
			newTreeSet.addAll(tempTreeSet);
			postings.remove(values[0]);
			postings.put(values[0], newTreeSet);
		}else{		//else put it right away
			postings.put(values[0], tempTreeSet);
		}
	}
	
	//delete files from previous executions
	private static boolean deleteDir(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i = 0; i < children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	                return false;
	            }
	        }
	    }

	    return dir.delete(); // The directory is empty now and can be deleted.
	}
	
}
