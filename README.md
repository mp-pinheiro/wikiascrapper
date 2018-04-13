# WikiaScrapper
JAVA scrapper for the top 250 characters in the Marvel and DC Wikia.

# Compiling
Compiled using Eclipse Oxygen.3 Release (4.7.3). 
Point the ".classpath" JSOUP library to the "jar" file on the "lib" folder.

# Dependencies
This project uses "JSOUP" which is licensed under the MIT license. For more information on the JSOUP library, visit the [official GitHub Repository](https://github.com/jhy/jsoup).

# Structure
The program works by reading all the urls on the brute "csv" converted from the "json" file fetched via the Wikia API. All the entries are stored into a shared Queue named "lineList". The Worker threads then take lines from the Queue and, if the entry is a character, scrap the data. The scrapped data is then inserted into a list of scrapped the data named "finalList". When there are no more entries on the Queue to be scrapped, the threads die and the program sorts the finalList and writes it to the "final.csv" file.
