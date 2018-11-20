package my.cute.discordbot.preferences;

import java.io.File;
import java.io.IOException;

import my.cute.discordbot.IdUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordFilterLoader {
	private static final Logger logger = LoggerFactory.getLogger(WordFilterLoader.class);
	
	public static WordFilter load(long id) {
		WordFilter filter = new WordFilter(id);
		File filterFile = new File("./markovdb/" + id + "/wordfilter.ini");
		if(filterFile.isFile()) {
			try (LineIterator it = FileUtils.lineIterator(filterFile, "UTF-8")) {
				while(it.hasNext()) {
					String line = it.nextLine();
					String tokens[] = line.split("=",2);
					String option = tokens[0];
					String value = tokens[1];
					
					switch(option) {
						case "bannedwords":
							if(!value.equals("[empty]")) filter.addFilteredWords(value);
							break;
						case "filteraction":
							filter.setFilterResponse(value);
							break;
						case "compiledfilter":
							if(!value.equals("[null]")) filter.setCompiledFilter(value);
							break;
						case "isusingexplicitregex":
							filter.setExplicitRegexStatus(Boolean.parseBoolean(value));
							break;
						default:
							logger.warn("unknown option encountered in PreferencesLoader.load() for guild "
									+ IdUtils.getFormattedServer(id) + ". line: " + line);
							break;
					}
				}
			} catch (IOException e) {
				logger.error("couldn't build wordfilter for guild " + IdUtils.getFormattedServer(id)
						+ " in WordFilterLoader.load(). fatal error");
				e.printStackTrace();
				System.exit(1);
			}
		} 
		return filter;
	}
}
