package net.indiespot.css;

import java.awt.BorderLayout;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class CssTextPaneTest {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
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