package net.indiespot.css;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import java.util.LinkedList;

public class CssTextPane {
	private static class SyntaxElement {
		Pattern pattern;
		int group;
		String styleName;

		public SyntaxElement(String styleName, Pattern pattern, int group) {
			this.pattern = pattern;
			this.group = group;
			this.styleName = styleName;
		}
	}

	private final StyledDocument doc;
	private final Style root;
	final JTextPane textPane;
	final List<SyntaxElement> elements;

	public CssTextPane() {
		this.textPane = new JTextPane();
		this.doc = textPane.getStyledDocument();
		this.root = doc.addStyle("root", StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE));
		this.elements = new ArrayList<SyntaxElement>();
	}

	public void addSyntaxElement(String styleName, Pattern pattern, int matchGroup) {
		this.elements.add(new SyntaxElement(styleName, pattern, matchGroup));
	}

	public JTextPane getTextPane() {
		return this.textPane;
	}

	public JScrollPane wrapInScrollPane(int w, int h) {
		JScrollPane scroller = new JScrollPane(this.textPane);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.setPreferredSize(new Dimension(w, h));
		return scroller;
	}

	public void defineStyles(String css) {
		if((css = css.trim()).isEmpty()) {
			return;
		}

		for(String part : Text.split(css, '}')) {
			if(!(part = part.trim()).isEmpty()) {
				defineStyle(part + '}');
			}
		}
	}

	public Style defineStyle(String css) {
		return defineStyle(Text.before(css, '{').trim(), Text.between(css, "{", "}").trim());
	}

	public Style defineStyle(String name, String props) {
		Style style = null;

		if(name.equals("*")) {
			style = this.root;
		}
		else {
			style = this.doc.getStyle(name);

			if(style == null) {
				style = this.doc.addStyle(name, this.root);
			}
		}

		for(String part : Text.split(props, ';')) {
			part = part.trim();
			if(part.isEmpty())
				continue;

			String[] keyval = Text.splitPair(part, ":");
			if(keyval == null) {
				this.logInputIssue("invalid property definition: [" + part + "]");
				continue;
			}

			String key = keyval[0].trim();
			String val = keyval[1].trim();

			if(key.isEmpty() || val.isEmpty()) {
				this.logInputIssue("invalid property definition: [" + part + "]");
				continue;
			}

			setStyleProperty(style, key, val);
		}
		return style;
	}

	public void setStyleProperty(Style style, String property, String value) {
		if(property.equals("font-family")) {
			StyleConstants.setFontFamily(style, value);
		}
		else if(property.equals("font-size")) {
			StyleConstants.setFontSize(style, Integer.parseInt(Text.replace(value, "pt", "")));
		}
		else if(property.equals("font-weight")) {
			StyleConstants.setBold(style, value.equals("bold"));
		}
		else if(property.equals("font-style")) {
			StyleConstants.setItalic(style, value.equals("italic"));
		}
		else if(property.equals("color")) {
			Color color = parseColor(value);
			if(color != null)
				StyleConstants.setForeground(style, color);
		}
		else if(property.equals("background") || property.equals("background-color")) {
			Color color = parseColor(value);
			if(color != null)
				StyleConstants.setBackground(style, color);
		}
		else if(property.equals("text-decoration")) {
			StyleConstants.setUnderline(style, false);
			StyleConstants.setStrikeThrough(style, false);

			for(String part : Text.split(Text.removeDuplicates(value, ' '), ' ')) {
				if(part.equals("none"))
					continue;
				if(part.equals("underline"))
					StyleConstants.setUnderline(style, true);
				else if(part.equals("line-through"))
					StyleConstants.setStrikeThrough(style, true);
				else
					this.logInputIssue("unexpected style property: '" + property + "'");
			}
		}
		else {
			this.logInputIssue("unexpected style property: '" + property + "'");
		}
	}

	private Color parseColor(String value) {
		if(!value.startsWith("#")) {
			this.logInputIssue("failed to parse color: '" + value + "'");
			return null;
		}

		value = Text.after(value, "#");
		if(value.length() == 3) // "abc" => "aabbcc"
		{
			char[] hex = new char[6];
			for(int i = 0; i < hex.length; i++)
				hex[i] = value.charAt(i >> 1);
			value = new String(hex);
		}

		int rgba;
		try {
			rgba = Integer.parseInt(value, 16);
		}
		catch (NumberFormatException exc) {
			this.logInputIssue("failed to parse hex color: '#" + value + "'");
			return null;
		}
		return new Color(rgba, value.length() == 8);
	}

	private final void logInputIssue(String issue) {
		System.err.println(this.getClass().getSimpleName() + " :: " + issue);
	}

	private String getRange(int off, int end) {
		try {
			return textPane.getDocument().getText(off, end - off);
		}
		catch (BadLocationException e) {
			throw new IllegalStateException(e);
		}
	}

	public final void activateAutoRestyle() {
		this.textPane.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				this.onEdit(e, false);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				this.onEdit(e, true);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// DO NOTHING!
			}

			private void onEdit(DocumentEvent e, boolean isRemove) {
				//long t0 = System.nanoTime();
				final int len = textPane.getDocument().getLength();

				//System.out.println(e.getOffset() + "/" + e.getLength());

				int bottom = 0;
				int off = e.getOffset();
				do {
					//System.out.println(off);
					int min = off;
					int end = off;

					while (min > bottom && !getRange(min, end).startsWith("\n")) {
						min = Math.max(bottom, min - 1);
					}
					if(min > bottom) {
						min += 1;
					}

					while (end < len && !getRange(min, end).endsWith("\n")) {
						end = Math.min(len, end + 1);
					}
					if(end < len) {
						end -= 1;
					}

					//System.out.println("[" + min + ".." + end + "]");
					if(min != end) {
						//String text = getRange(min, end);
						//System.out.println("[" + min + ".." + end + "/" + e.getOffset() + "+" + e.getLength() + "] " + text);
						restyleRange(min, end);
					}

					if(isRemove || end + 1 >= e.getOffset() + e.getLength())
						break;
					off = end + 1;
					bottom = end + 1;
				}
				while (true);

				//long t1 = System.nanoTime();
				//System.out.println((t1 - t0) / 1000 + "us");
			}
		});
	}

	@SuppressWarnings("serial")
	public final DocumentListener activateUndoRedo() {
		final TextPaneUndo undoSupport = new TextPaneUndo(this.textPane);

		this.textPane.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
		this.textPane.getActionMap().put("Undo", new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent evt) {
				undoSupport.undo();
			}
		});

		this.textPane.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
		this.textPane.getActionMap().put("Redo", new AbstractAction("Redo") {
			public void actionPerformed(ActionEvent evt) {
				undoSupport.redo();
			}
		});

		return undoSupport.register();
	}

	public final void restyleRange(final int off, final int end) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final String range = getRange(off, end);

				clearStyles(off, end);
				applyStyleOnRange("root", off, end);

				for(SyntaxElement element : elements) {
					for(Matcher matcher = element.pattern.matcher(range); matcher.find();) {
						applyStyleOnMatch(element.styleName, off, matcher, element.group);
					}
				}
			}
		});
	}

	public void clearStyles(int off, int end) {
		StyledDocument doc = this.textPane.getStyledDocument();
		Style style = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		doc.setCharacterAttributes(off, end - off, style, true);
	}

	public void applyStyleOnRange(String styleName, int off, int end) {
		StyledDocument doc = this.textPane.getStyledDocument();
		doc.setCharacterAttributes(off, end - off, doc.getStyle(styleName), false);
	}

	public void applyStyleOnMatch(String styleName, int off, Matcher matcher) {
		this.applyStyleOnRange(styleName, off + matcher.start(), off + matcher.end());
	}

	public void applyStyleOnMatch(String styleName, int off, Matcher matcher, int group) {
		this.applyStyleOnRange(styleName, off + matcher.start(group), off + matcher.end(group));
	}
}

class TextPaneUndo {
	final JTextPane textPane;
	private boolean isBusy;

	private final LinkedList<Snapshot> undoStack;
	private final LinkedList<Snapshot> redoStack;
	private final int maxHistory;

	public TextPaneUndo(JTextPane textPane) {
		this.textPane = textPane;

		this.maxHistory = 256;
		this.isBusy = false;

		this.redoStack = new LinkedList<Snapshot>();
		this.undoStack = new LinkedList<Snapshot>();
		this.undoStack.addFirst(new Snapshot(false));
	}

	public final DocumentListener register() {
		DocumentListener listener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				TextPaneUndo.this.onEvent(true);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				TextPaneUndo.this.onEvent(false);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// DO NOTHING!
			}
		};

		this.textPane.getDocument().addDocumentListener(listener);

		return listener;
	}

	private class Snapshot {
		final String text;
		final int caret;

		public Snapshot(boolean isInsertEvent) {
			this.text = TextPaneUndo.this.textPane.getText();

			int pos = TextPaneUndo.this.textPane.getCaretPosition();
			int len = TextPaneUndo.this.textPane.getDocument().getLength();
			this.caret = Math.min(pos + (isInsertEvent ? 1 : 0), len);
		}

		public void restore() {
			TextPaneUndo.this.textPane.setText(this.text);
			TextPaneUndo.this.textPane.setCaretPosition(this.caret);
		}
	}

	public void onEvent(boolean isInsertEvent) {
		if(!this.isBusy) {
			redoStack.clear();
			undoStack.addFirst(new Snapshot(isInsertEvent));

			while (undoStack.size() > this.maxHistory) {
				undoStack.removeLast();
			}
		}
	}

	public final void undo() {
		this.isBusy = true;

		try {
			String curr = textPane.getText();
			Snapshot undo = null;
			while (!undoStack.isEmpty()) {
				undo = undoStack.removeFirst();
				if(redoStack.isEmpty() || !redoStack.getFirst().text.equals(undo.text))
					redoStack.addFirst(undo);
				if(!undo.text.equals(curr))
					break;
			}

			if(undo != null) {
				undo.restore();
			}

			// store new situation
			undoStack.addFirst(new Snapshot(false));
		}
		finally {
			this.isBusy = false;
		}
	}

	public final void redo() {
		this.isBusy = true;

		try {
			String curr = textPane.getText();
			Snapshot redo = null;
			while (!redoStack.isEmpty()) {
				redo = redoStack.removeFirst();
				if(undoStack.isEmpty() || !undoStack.getFirst().text.equals(redo.text))
					undoStack.addFirst(redo);
				if(!redo.text.equals(curr))
					break;
			}

			if(redo != null) {
				redo.restore();
			}
		}
		finally {
			this.isBusy = false;
		}
	}
}

class Text {

	public static String normalizeLines(String s) {
		s = replace(s, "\r\n", "\n");
		s = replace(s, "\r", "\n");
		return s;
	}

	public static String[] splitOnLines(String s) {
		return normalizeLines(s).split("\n");
	}

	public static String[] splitPair(String s, String find) {
		int io = s.indexOf(find);
		if(io == -1)
			return null;
		return new String[] { s.substring(0, io), s.substring(io + find.length()) };
	}

	//

	public static String before(String value, char find) {
		return before(value, find, null);
	}

	public static String before(String value, char find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(0, io);
	}

	public static String before(String value, String find) {
		return before(value, find, null);
	}

	public static String before(String value, String find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(0, io);
	}

	public static String after(String value, char find) {
		return after(value, find, null);
	}

	public static String after(String value, char find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(io + 1);
	}

	public static String after(String value, String find) {
		return after(value, find, null);
	}

	public static String after(String value, String find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(io + find.length());
	}

	public static String removeLast(String value, char find) {
		int indexOf = value.lastIndexOf(find);
		if(indexOf != -1) {
			String before = value.substring(0, indexOf);
			String after = value.substring(indexOf + 1);
			value = before + after;
		}
		return value;
	}

	public static String trimToNull(String s) {
		if(s == null)
			return null;
		s = s.trim();
		if(s.isEmpty())
			return null;
		return s;
	}

	public static String between(String s, String after, String before) {
		return Text.before(Text.after(s, after), before);
	}

	// ----

	public static List<String> split(String s, char find) {
		List<String> list = new ArrayList<>();
		int off = 0;
		while (true) {
			int io = s.indexOf(find, off);
			if(io == -1)
				break;
			list.add(s.substring(off, io));
			off = io + 1;
		}
		list.add(s.substring(off));
		return list;
	}

	public static List<String> split(String s, String find) {
		List<String> list = new ArrayList<>();
		int off = 0;
		while (true) {
			int io = s.indexOf(find, off);
			if(io == -1)
				break;
			list.add(s.substring(off, io));
			off = io + find.length();
		}
		list.add(s.substring(off));
		return list;
	}

	public static String replace(String s, String find, String replace) {
		StringBuilder sb = new StringBuilder();
		int off = 0;
		while (true) {
			int io = s.indexOf(find, off);
			if(io == -1)
				break;
			sb.append(s.substring(off, io));
			sb.append(replace);
			off = io + find.length();
		}
		return sb.append(s.substring(off)).toString();
	}

	public static String removeDuplicates(String s, char c) {
		StringBuilder sb = new StringBuilder();
		boolean isAt = false;
		for(char x : s.toCharArray()) {
			if(x == c) {
				if(isAt)
					continue;
				isAt = true;
			}
			else {
				isAt = false;
			}
			sb.append(x);
		}
		return sb.toString();

	}
}