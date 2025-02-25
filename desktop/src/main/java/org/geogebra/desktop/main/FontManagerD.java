package org.geogebra.desktop.main;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.UIManager;

import org.geogebra.common.awt.GFont;
import org.geogebra.common.main.FontManager;
import org.geogebra.common.util.lang.Language;
import org.geogebra.desktop.awt.GFontD;

import com.himamis.retex.editor.share.util.Unicode;

/**
 * Manages fonts for different languages. Use setLanguage() and setFontSize() to
 * initialize the default fonts.
 */
public class FontManagerD extends FontManager {

	private GFont boldFont;
	private GFont italicFont;
	private GFont smallFont;
	private GFont plainFont;
	private GFont serifFont;
	private GFont serifFontBold;
	private GFont javaSans;
	private GFont javaSerif;

	private int fontSize;
	private String sansName;
	private String serifName;

	private final HashMap<String, Font> fontMap = new HashMap<>();
	private final StringBuilder key = new StringBuilder();

	private static final String[] FONT_NAMES_SANSSERIF = { "SansSerif", // Java
			"Arial Unicode MS", // Windows
			"Helvetica", // Mac OS X
			"LucidaGrande", // Mac OS X
			"ArialUnicodeMS" // Mac OS X
	};
	private static final String[] FONT_NAMES_SERIF = { "Serif", // Java
			"Times New Roman", // Windows
			"Times" // Mac OS X
	};

	public static class NoFontException extends Exception {
		public NoFontException() {
			super("Sorry, there is no font for this language available on your computer.");
		}
	}

	public FontManagerD() {
		setFontSize(12);
	}

	/**
	 * Sets default font that works with the given language.
	 * @throws NoFontException if no font works for given locale
	 */
	public void setLanguage(final Locale locale) throws NoFontException {
		final String lang = locale.getLanguage();

		// new font names for language
		String fontNameSansSerif;
		String fontNameSerif;

		// certain languages need special fonts to display its characters
		final StringBuilder testCharacters = new StringBuilder();
		final LinkedList<String> tryFontsSansSerif = new LinkedList<>(
				Arrays.asList(FONT_NAMES_SANSSERIF));
		final LinkedList<String> tryFontsSerif = new LinkedList<>(
				Arrays.asList(FONT_NAMES_SERIF));

		final String testChar = Language.getTestChar(lang);
		if (testChar != null) {
			testCharacters.append(testChar);
		}

		// CHINESE
		if ("zh".equals(lang)) {
			// last CJK unified ideograph in unicode alphabet
			// testCharacters.append('\u984F');
			tryFontsSansSerif.addFirst("\u00cb\u00ce\u00cc\u00e5");
			tryFontsSerif.addFirst("\u00cb\u00ce\u00cc\u00e5");
		}

		// GEORGIAN
		else if ("ka".equals(lang)) {
			// some Georgian letter
			// testCharacters.append('\u10d8');
			tryFontsSerif.addFirst("Sylfaen");
		}

		// HEBREW
		// Guy Hed, 26.4.2009 - added Yiddish, which also use Hebrew letters.
		else if ("iw".equals(lang) || "ji".equals(lang)) {
			// Hebrew letter "tav"
			// testCharacters.append('\u05ea');

			// move Java fonts to end of list
			tryFontsSansSerif.remove("SansSerif");
			tryFontsSansSerif.addLast("SansSerif");
			tryFontsSerif.remove("Serif");
			tryFontsSerif.addLast("Serif");
		}

		// we need roman (English) characters if possible
		// eg the language menu :)
		testCharacters.append('a');

		// make sure we use a font that can display the Euler character
		// add at end -> lowest priority
		testCharacters.append(Unicode.EULER_CHAR);

		// get fonts that can display all test characters
		fontNameSansSerif = getFontCanDisplay(tryFontsSansSerif,
				testCharacters.toString());
		fontNameSerif = getFontCanDisplay(tryFontsSerif,
				testCharacters.toString());

		// make sure we have sans serif and serif fonts
		if (fontNameSansSerif == null) {
			fontNameSansSerif = "SansSerif";
		}
		if (fontNameSerif == null) {
			fontNameSerif = "Serif";
		}

		// update application fonts if changed
		updateDefaultFonts(fontSize, fontNameSansSerif, fontNameSerif);
	}

	/**
	 * Set default font size.
	 */
	@Override
	public void setFontSize(final int size) {
		// current sans and sansserif font names
		final String sans = plainFont == null ? "SansSerif"
				: plainFont.getFontName();
		final String serif = serifFont == null ? "Serif"
				: serifFont.getFontName();

		// update size
		updateDefaultFonts(size, sans, serif);
	}

	/**
	 * @param size font size
	 * @param sans sans-serif font name
	 * @param serif serif font name
	 */
	public void updateDefaultFonts(final int size, final String sans,
			final String serif) {
		if ((size == fontSize) && sans.equals(sansName)
				&& serif.equals(serifName)) {
			return;
		}
		fontSize = size;
		sansName = sans;
		serifName = serif;

		// Java fonts
		javaSans = getFont("SansSerif", Font.PLAIN, size);
		javaSerif = getFont("Serif", Font.PLAIN, size);

		// create similar font with the specified size
		plainFont = getFont(sans, Font.PLAIN, size);
		boldFont = getFont(sans, Font.BOLD, size);
		italicFont = getFont(sans, Font.ITALIC, size);
		smallFont = getFont(sans, Font.PLAIN, size - 2);

		// serif
		serifFont = getFont(serif, Font.PLAIN, size);
		serifFontBold = getFont(serif, Font.BOLD, size);

		// TODO: causes problems with multiple windows (File -> New Window)
		setLAFFont(((GFontD) plainFont).getAwtFont());
	}

	/**
	 * @return a font with the specified attributes.
	 * 
	 * @param serif whether the font is serif
	 * @param style font style
	 * @param size font size
	 */
	public GFont getFont(final boolean serif, final int style, final int size) {
		final String name = serif ? getSerifFont().getFontName()
				: getPlainFont().getFontName();
		return getFont(name, style, size);
	}

	/**
	 * Gets a font from a HashMap to avoid multiple creations of the same font.
	 */
	private GFont getFont(final String name, final int style, final int size) {
		// build font's key name for HashMap
		key.setLength(0);
		key.append(name);
		key.append('_');
		key.append(style);
		key.append('_');
		key.append(size);

		// look if we have this font already in the HashMap
		Font f = fontMap.get(key.toString());
		if (f == null) {
			// new font: create it and keep it in the HashMap
			f = new Font(name, style, size);
			fontMap.put(key.toString(), f);

			// System.out.println("NEW font: " + f);
		}

		return new GFontD(f);
	}

	/**
	 * Returns a font that can display testString.
	 */
	@Override
	public GFont getFontCanDisplay(final String testString,
			final boolean serif, final int fontStyle, final int fontSize) {

		final GFont appFont = serif ? serifFont : plainFont;
		if (appFont == null) {
			return plainFont;
		}

		// check if default font is ok
		if ((testString == null)
				|| (appFont.canDisplayUpTo(testString) == -1)) {
			if (appFont.getSize() == fontSize) {
				if (appFont.getStyle() == fontStyle) {
					return appFont;
				} else if (fontStyle == Font.BOLD) {
					return serif ? serifFontBold : boldFont;
				}
			}

			// need to compute new font
			return getFont(appFont.getFontName(), fontStyle, fontSize);
		}

		// check if standard Java fonts can be used
		final GFont javaFont = serif ? javaSerif : javaSans;
		if (javaFont.canDisplayUpTo(testString) == -1) {
			return getFont(((GFontD) javaFont).getAwtFont().getName(),
					fontStyle, fontSize);
		}

		// no standard fonts worked: try harder and go through all
		// fonts to find one that can display the testString
		try {
			final LinkedList<String> tryFonts = serif
					? new LinkedList<>(Arrays.asList(FONT_NAMES_SERIF))
					: new LinkedList<>(Arrays.asList(FONT_NAMES_SANSSERIF));
			final String fontName = getFontCanDisplay(tryFonts, testString);
			return getFont(fontName, fontStyle, fontSize);
		} catch (final Exception e) {
			return appFont;
		}
	}

	/**
	 * Tries to find a font that can display all given unicode characters.
	 * Starts with tryFontNames first.
	 * @return font name
	 * @throws NoFontException if no font works for given locale
	 */
	public String getFontCanDisplay(final LinkedList<String> tryFontNames,
			final String testCharacters) throws NoFontException {

		// try given fonts
		if (tryFontNames != null) {
			for (String fontName : tryFontNames) {
				// create font for name
				final GFont font = getFont(fontName, Font.PLAIN, 12);

				// check if creating font worked
				if (((GFontD) font).getAwtFont().getFamily()
						.startsWith(fontName)) {
					// test if this font can display all test characters
					if (font.canDisplayUpTo(testCharacters) == -1) {
						return font.getFontName();
					}
				}
			}
		}

		int maxDisplayedChars = 0;
		int bestFont = -1;

		// Determine which fonts best support the characters in testCharacters
		final Font[] allfonts = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getAllFonts();
		for (int j = 0; j < allfonts.length; j++) {
			// Log.debug(allfonts[j].toString());
			final int charsDisplayed = allfonts[j]
					.canDisplayUpTo(testCharacters);
			if (charsDisplayed == -1) {
				// avoid "Monospace" font here
				if (!allfonts[j].getFamily().equals("Monospaced")) {
					return allfonts[j].getFontName();
				}
			}

			// no exact match, but store how much matches
			if (charsDisplayed > maxDisplayedChars) {
				bestFont = j;
				maxDisplayedChars = charsDisplayed;
			}

		}

		// no exact match, return the font that matches the most characters
		if (bestFont > -1) {
			return allfonts[bestFont].getFontName();
		}

		throw new NoFontException();
	}

	final public GFont getBoldFont() {
		return boldFont;
	}

	final public GFont getItalicFont() {
		return italicFont;
	}

	final public GFont getPlainFont() {
		return plainFont;
	}

	final public GFont getSmallFont() {
		return smallFont;
	}

	final public GFont getSerifFont() {
		return serifFont;
	}

	private static void setLAFFont(final Font plain) {
		UIManager.put("ColorChooser.font", plain);
		UIManager.put("FileChooser.font", plain);

		// Panel, Pane, Bars
		UIManager.put("Panel.font", plain);
		UIManager.put("TextPane.font", plain);
		UIManager.put("OptionPane.font", plain);
		UIManager.put("OptionPane.messageFont", plain);
		UIManager.put("OptionPane.buttonFont", plain);
		UIManager.put("EditorPane.font", plain);
		UIManager.put("ScrollPane.font", plain);
		UIManager.put("TabbedPane.font", plain);
		UIManager.put("ToolBar.font", plain);
		UIManager.put("ProgressBar.font", plain);
		UIManager.put("Viewport.font", plain);
		UIManager.put("TitledBorder.font", plain);

		// Buttons
		UIManager.put("Button.font", plain);
		UIManager.put("RadioButton.font", plain);
		UIManager.put("ToggleButton.font", plain);
		UIManager.put("ComboBox.font", plain);
		UIManager.put("CheckBox.font", plain);

		// Menus
		UIManager.put("Menu.font", plain);
		UIManager.put("Menu.acceleratorFont", plain);
		UIManager.put("PopupMenu.font", plain);
		UIManager.put("MenuBar.font", plain);
		UIManager.put("MenuItem.font", plain);
		UIManager.put("MenuItem.acceleratorFont", plain);
		UIManager.put("CheckBoxMenuItem.font", plain);
		UIManager.put("RadioButtonMenuItem.font", plain);

		// Fields, Labels
		UIManager.put("Label.font", plain);
		UIManager.put("Table.font", plain);
		UIManager.put("TableHeader.font", plain);
		UIManager.put("Tree.font", plain);
		UIManager.put("Tree.rowHeight", Integer.valueOf(plain.getSize() + 5));
		UIManager.put("List.font", plain);
		UIManager.put("TextField.font", plain);
		UIManager.put("PasswordField.font", plain);
		UIManager.put("TextArea.font", plain);
		UIManager.put("ToolTip.font", plain);
	}

	/**
	 * @return font size
	 */
	public int getFontSize() {
		return fontSize;
	}

}
