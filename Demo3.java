package my.cute.discordbot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import org.apache.commons.io.FileUtils;



public class Demo3 {
	
	
	public static void main(String[] args) {
		Charset utf8 = StandardCharsets.UTF_8;
		Calendar cal = Calendar.getInstance();
		ArrayList<String> quotes = new ArrayList<String>();
		long seed = Long.parseLong((""+cal.get(Calendar.YEAR)+cal.get(Calendar.MONTH)+cal.get(Calendar.DAY_OF_MONTH)));
		try {
			quotes = (ArrayList<String>)FileUtils.readLines(new File("twitchchat.txt"),utf8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(quotes.size() + ", seed: " + seed);
		for(String s : quotes) {
			System.out.println(s);
		}
		
		Random r = new Random(2017801L);
		String[] quoteArray = quotes.toArray(new String[0]);
		
		seed = 2017806L;
		System.out.println(r.nextInt(128));
		System.out.println(0x5DEECE66DL);
		long setSeed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
		System.out.println(setSeed);
		System.out.println(Long.toBinaryString(setSeed));
		
		long nextSeed = (setSeed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
		//System.out.println(r.nextInt(quotes.size()));
		System.out.println( nextSeed );
		System.out.println(Long.toBinaryString(nextSeed));
		int out =  (int)(nextSeed >>> (48 - 31));
		System.out.println(out);
		System.out.println(Integer.toBinaryString(out));
		int out2 = (int)((128 * (long) out) >> 31);
		long out3 = 128 * (long) out;
		System.out.println(Long.toBinaryString(out3));
		System.out.println(Integer.toBinaryString(out2));
		System.out.println(out2);
	}
}
