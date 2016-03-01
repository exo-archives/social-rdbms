package org.exoplatform.social.addons.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.core.space.SpaceFilter;

public class ESSpaceFilter extends SpaceFilter {

  private Map<String, Set<Status>> statusMap = new HashMap<>();

  private boolean                  includePrivate;

  private boolean unifiedSearch;

  private boolean notHidden;

  private String displayName;

  private String prettyName;

  public ESSpaceFilter setSpaceFilter(SpaceFilter spaceFilter) {
    if (spaceFilter != null) {
      this.setAppId(spaceFilter.getAppId());
      this.setFirstCharacterOfSpaceName(spaceFilter.getFirstCharacterOfSpaceName());
      this.setIncludeSpaces(spaceFilter.getIncludeSpaces());
      this.setRemoteId(spaceFilter.getRemoteId());
      this.setSorting(spaceFilter.getSorting());
      if (spaceFilter.getSpaceNameSearchCondition() != null) {
        this.setSpaceNameSearchCondition(spaceFilter.getSpaceNameSearchCondition());        
      }
      
      if (spaceFilter instanceof ESSpaceFilter) {
        ESSpaceFilter filter = (ESSpaceFilter)spaceFilter;
        
        this.setIncludePrivate(filter.isIncludePrivate());
        this.setUnifiedSearch(filter.isIncludePrivate());
        this.setNotHidden(filter.isNotHidden());
        this.setDisplayName(filter.getDisplayName());
        this.setPrettyName(filter.getPrettyName());
        
        for (String userId : statusMap.keySet()) {
          for (Status status : statusMap.get(userId)) {
            this.addStatus(userId, status);
          }
        }
      }      
    }
    return this;
  }

  public ESSpaceFilter addStatus(String userId, Status st) {
    Set<Status> status = statusMap.get(userId);
    if (status == null) {
      status = new HashSet<>();
      statusMap.put(userId, status);
    }
    status.add(st);
    return this;
  }


  public ESSpaceFilter setIncludePrivate(boolean includePrivate) {
    this.includePrivate = includePrivate;
    return this;
  }
  
  public ESSpaceFilter setUnifiedSearch(boolean unifiedSearch) {
    this.unifiedSearch = unifiedSearch;
    return this;
  }

  public ESSpaceFilter setNotHidden(boolean notHidden) {
    this.notHidden = notHidden;
    return this;
  }

  public ESSpaceFilter setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }  

  public Map<String, Set<Status>> getStatusMap() {
    return statusMap;
  }

  public boolean isIncludePrivate() {
    return includePrivate;
  }

  public boolean isUnifiedSearch() {
    return unifiedSearch;
  }

  public boolean isNotHidden() {
    return notHidden;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setPrettyName(String prettyName) {
    this.prettyName = prettyName;
  }

  public String getPrettyName() {
    return prettyName;
  }
  
}
