package org.exoplatform.social.addons.storage.entity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 4, 2015  
 */
public enum RelationshipFilterType {
  ALL("ALL"), SENDER("SENDER"), RECEIVER("RECEIVER");

  private final String type;

  public String getType() {
    return type;
  }

  RelationshipFilterType(String type) {
    this.type = type;
  }
}
