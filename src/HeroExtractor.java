import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class ExtractorWorker extends Thread{
	@Override
	public void run() {
		//while there are articles to be checked, run
		while(!HeroExtractor.isListEmpty()) {
			//get line
			String line[] = HeroExtractor.getListElement(this.getName());
			
			//get wikia url (marvel or dc)
			String url = "http://"+line[12]+".wikia.com" + line[3];
			//String url = "http://marvel.wikia.com/wiki/Amadeus_Cho_(Earth-616)";
			Document doc = null;
			try {
				//connect to url
				doc = Jsoup.connect(url).get();
				
				//check for "character" category (remove non-character articles)
				Elements categories = doc.select("li.category");
				for(Element e : categories) {
					//if the article is a character
					if(e.text().equals("Characters")) {
						System.out.println(getName() + ": Found character '" + line[1] + "', adding to the list.");
						
						//find Real Name
						//get first element with class "pi-data-value"
						String name = doc.select(".pi-data-value").first().text().replaceAll(";", "").replaceAll("\\[[1-9]+\\]", ""); //remove delimiters ";" and citations "[x]"
						
						//find aliases
						//get second element with class "pi-data-value"
						String alias = doc.select(".pi-data-value").get(1).text().replaceAll(";", "").replaceAll("\\[[1-9]+\\]", "");
						
						//find aligment
						//fetch all "pi-data-value" which children have "a" tags, 
						String aligment = "";
						for(Element ali : doc.select(".pi-data-value").select("a")){
							//store the ones that contain "Neutral", "Good" or "Bad"
							if(ali.text().equals("Neutral") || ali.text().equals("Bad") || ali.text().equals("Good")) aligment = ali.text().replaceAll(";", "");;
						};
						
						//find history (description)
						//fetch all "p" tag elements
						Elements ps = doc.select("p");
						String history = "";
						if(ps.size()==1) {
							//usually, when the article only has 1 "p" tag, it is the story of the character  
							history = ps.text().replaceAll(";", "").replaceAll("\\[[1-9]+\\]", "");
						}else {
							//this is trash
							//since there are no other properties (classes, ids, names) that differentiate the description "p" tags from the others, I tried to remove the ones that were probably wrong
							for(Element p : ps) {
								if(p.text().isEmpty() || p.text().length()<200) { 
									//removed empty "p" tags
									//removed "p" tags with less than 200 characters text. This is why there are descriptions with "family members" instead of history. Changing the number to 500 fetches only history, but some 10 entries get blank descriptions.
									continue;
								}else {
									history = p.text().replaceAll(";", "").replaceAll("\\[[1-9]+\\]", "");
									break;
								}
							}
							//in case there are only empty "p" tags and the history, store that
							if(history.isEmpty()) history = ps.text().replaceAll(";", "").replaceAll("\\[[1-9]+\\]", "");
						}
						
						//add the entry to the character list
						HeroExtractor.writeToFinalFile(getName(), line[1]+";"+line[12]+";"+line[9]+";"+name+";"+alias+";"+aligment+";"+history);
						break;
					}
				}
			} catch (IOException e) {
				//connection error
				System.err.println("WORKER " + getName() + ": Could not connect to " + line[3]);
			}
		}
	}
}

public class HeroExtractor {
	private static LinkedList<String> finalList = new LinkedList<String>();
	private static Scanner s;
	private static FileWriter fw;
	private static LinkedList<String[]> lineList = new LinkedList<>();
	private static Thread[] threads = new Thread[10]; 
	
	//synchronized method for checking if there are articles to be scrapped
	public static synchronized boolean isListEmpty() {
		return lineList.isEmpty();
	}
	
	//synchronized method for removing the top article from the list
	public static synchronized String[] getListElement(String string) {
		String[] s = lineList.removeFirst();
		System.out.println(string + ": trying page " + s[3]);
		return s;
	}
	
	//synchronized method for adding an entry to the character list
	public static synchronized void writeToFinalFile(String string, String line) {
		System.out.println(string + ": adding to final list: '" + line + "'.");
		finalList.add(line);
	}
	
	public static void initialize() {
		try {
			//read brute data with all 500 articles
			s = new Scanner(new File("marveldcfiltered.csv"));
			
			//create line list for threads
			s.nextLine();
			while(s.hasNext()) {
				lineList.add(s.nextLine().split(";"));
			}
			
			//initialize threads
			for(int i=0; i<threads.length; i++) {
				threads[i] = new ExtractorWorker();
				threads[i].start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//checks if there are any threads still running
	public static boolean areThreadsRunning() {
		for(Thread t : threads) {
			if(t.isAlive()) return true;
		}
		return false;
	}

	public static void main(String[] args) throws IOException {
		//long for time evaluation
		long before = System.currentTimeMillis();
		
		//initialize data
		initialize();
		
		//opens the "final.csv" file where all the scrapped entries will be written to
		fw = new FileWriter(new File("final.csv"));
		
		//writes the header of the file (item_title comes first for sorting alphabetically)
		fw.write("item_title;item_newtork;item_thumbnail;item_name;item_alias;item_aligment;item_description\n");
		
		//NOP while the threads are working
		while(areThreadsRunning());
		
		//sort the entry list
		Collections.sort(finalList);
		
		//write all the entries to the file
		fw.write(String.join("\n", finalList));
		fw.close();
		
		//print the elapsed time
		System.out.println();
		System.out.println("INFO: Finished scraping in " + (System.currentTimeMillis()-before)/1000 + " seconds.");
	}
}
