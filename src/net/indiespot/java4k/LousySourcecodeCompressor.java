package net.indiespot.java4k;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LousySourcecodeCompressor {
	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);

		int origLength = 0;
		StringBuilder trimmed = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;

			origLength += line.length() + 1;
			line = line.trim();
			if (line.isEmpty())
				continue;

			trimmed.append(line).append('\n');
		}
		String code = trimmed.toString();

		// make a lousy attempt at stripping comments
		code = Pattern.compile("//[a-zA-Z\\s]+$", Pattern.MULTILINE).matcher(code).replaceAll("");

		// make a lousy attempt at stripping annotations
		code = Pattern.compile("^@[a-zA-Z]+$", Pattern.MULTILINE).matcher(code).replaceAll("");

		// make a lousy attempt at stripping optional whitespace
		code = code.replaceAll("\\s*([\\+\\-\\*/%,\\(\\)\\{\\}\\[\\]=;:<>!])\\s*", "$1");

		System.out.println(code.length() + "/" + origLength);
		System.out.println();
		System.out.println(code);
	}
}
