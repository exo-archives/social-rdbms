package org.exoplatform.social.core.entity;

/**
 * Created by bdechateauvieux on 4/1/15.
 */
public enum StreamType {
  SPACE("SPACE"), POSTER("POSTER"), LIKER("LIKER"), COMMENTER("COMMENTER"), MENTIONER("MENTIONER"), SPACE_MEMBER("SPACE_MEMBER");

  private final String type;

  public String getType() {
    return type;
  }

  StreamType(String type) {
    this.type = type;
  }
}
