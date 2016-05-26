package org.exoplatform.social.addons.updater.utils;

public class StringUtil {

  // Code point of last 3 bytes UTF-8 character
  private static final int LAST_3_BYTE_CHAR = "\uFFFF".codePointAt(0);

  /**
   * Finds and removes UTF-8 character that need more than 3 bytes
   * 
   * @param input the input string potentially containing invalid character
   * @return string
   */
  public static String removeLongUTF(String input) {
    if (input == null) {
      return input;
    }
    StringBuilder sb = new StringBuilder(input);

    for (int i = 0; i < sb.length(); i++) {
      int codePoint = sb.codePointAt(i);
      if (codePoint > LAST_3_BYTE_CHAR) {
        int count = Character.charCount(codePoint);
        sb.replace(i, i + count, "");
        i -= 1;
      }
    }
    return sb.toString();
  }
}
