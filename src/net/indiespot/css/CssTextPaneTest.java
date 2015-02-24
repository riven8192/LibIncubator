package net.indiespot.css;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Data;
import com.google.api.client.util.Key;

public class CssTextPaneTest {
	public static class User {
		@Key
		public String name;
		@Key
		public String pass;
		@Key
		public String empty;
		@Key
		public String pass2 = null;
		@Key
		public String pass3;
		@Key
		public int count;

		@Key
		public Set<String> set;
	}

	public static void main(String[] args) throws IOException {

		if (true) {
			String base = "https://cloudwise-login.appspot.com/oauth2callback?subId=";
			for (int i = 0; i < 256; i++) {
				String hex = Integer.toHexString(i);
				if (hex.length() < 2)
					hex = "0" + hex;
				System.out.println(base + hex);
			}

			System.exit(0);
		}

		if (true) {
			String json = "{ \"name\": \"hello\", \"pass\":null, \"empty\":\"\", \"pass2\":null, \"count\":3, \"set\":null }";
			User user = GsonFactory.getDefaultInstance().fromString(json, User.class);

			System.out.println(user.name);
			System.out.println(user.pass);
			System.out.println(user.empty);
			System.out.println(user.pass2);
			System.out.println(user.pass3);
			System.out.println(user.set);
			System.out.println();
			System.out.println(Data.isNull(user.name));
			System.out.println(Data.isNull(user.pass));
			System.out.println(Data.isNull(user.empty));
			System.out.println(Data.isNull(user.pass2));
			System.out.println(Data.isNull(user.pass3));
			System.out.println(Data.isNull(user.set));
			System.out.println();
			System.out.println(user.pass == null);
			System.out.println(user.pass2 == null);
			System.out.println(user.pass2 == user.pass3);
			System.out.println(user.pass3 == null);

			System.out.println(user.count);

			System.exit(0);
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		final CssTextPane pane = new CssTextPane();

		pane.defineStyle("*        { color: #000; font-style: normal; font-size:12; font-family: courier; } ");
		pane.defineStyle("signal   { color: #00f; font-style: normal; } ");
		pane.defineStyle("schedule { color: #c0c; font-style: normal; } ");
		pane.defineStyle("control  { color: #080; font-style: normal; } ");
		pane.defineStyle("trycatch { color: #c40; font-style: normal; } ");
		pane.defineStyle("jump     { color: #048; font-style: normal; } ");
		pane.defineStyle("comment  { color: #088; font-style: italic; text-decoration: none } ");

		String pre = "^|\\s|\\G";
		String post = "\\s|//|$";

		{
			pane.addSyntaxElement("signal", Pattern.compile("(" + pre + ")(\\d+)(" + post + ")"), 2);
			pane.addSyntaxElement("trycatch", Pattern.compile("(" + pre + ")(THROW|CATCH)(" + post + ")"), 2);
			pane.addSyntaxElement("schedule", Pattern.compile("(" + pre + ")(YIELD|SLEEP|WAIT|HALT)(" + post + ")"), 2);
			pane.addSyntaxElement("control", Pattern.compile("(" + pre + ")(BEGIN|END|FUNCTION|NOT|WHILE|DO|IF|THEN|ELSE)(" + post + ")"), 2);
			pane.addSyntaxElement("jump", Pattern.compile("(" + pre + ")(GOTO|CALL|BREAK|LOOP)(" + post + ")"), 2);
			pane.addSyntaxElement("comment", Pattern.compile("(#.*$)"), 1);
		}

		pane.activateUndoRedo();
		pane.activateAutoRestyle();

		JFrame frame = new JFrame("CrudeScript syntax highlighter");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(pane.wrapInScrollPane(640, 480));
		frame.setResizable(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}