/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.search;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;

import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.SpaceStorage;

public class SpaceIndexingServiceConnector extends ElasticIndexingServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger(SpaceIndexingServiceConnector.class);
  
  private static final long serialVersionUID = 9141474534628715938L;
  
  public final static String TYPE = "space"; 
  
  private SpaceService spaceService;
  
  private SpaceStorage spaceStorage;

  public SpaceIndexingServiceConnector(InitParams initParams, SpaceService spaceService, SpaceStorage spaceStorage) {
    super(initParams);
    this.spaceService = spaceService;
    this.spaceStorage = spaceStorage;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    Space space = spaceService.getSpaceById(id);
    
    Map<String, String> fields = new HashMap<>();
    fields.put("prettyName", space.getPrettyName());
    fields.put("displayName", space.getDisplayName());
    fields.put("description", space.getDescription());
    fields.put("visibility", space.getVisibility());
    
    String[] members = space.getMembers();
    fields.put(Status.MEMBER.name(), members != null ? StringUtils.join(members, " ") : null);
    String[] managers = space.getManagers();
    fields.put(Status.MANAGER.name(), managers != null ? StringUtils.join(managers, " ") : null);
    String[] pendings = space.getPendingUsers();    
    fields.put(Status.PENDING.name(), pendings != null ? StringUtils.join(pendings, " ") : null);
    String[] inviteds = space.getInvitedUsers();
    fields.put(Status.INVITED.name(), inviteds != null ? StringUtils.join(inviteds, " ") : null);
    
    Date createdDate = new Date(space.getCreatedTime());
    return new Document(TYPE, id, null, createdDate, (Set<String>)null, fields);
  }
  
  @Override
  public Document update(String id) {
    return create(id);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    
    List<String> ids = new LinkedList<>();
    try {
//      ExoContainer container = ExoContainerContext.getCurrentContainer();
//      RequestLifeCycle.begin(container);          
      List<Space> spaces = spaceStorage.getAllSpaces();
//      RequestLifeCycle.end();
      int to = offset + limit;
      to = to > spaces.size() ? spaces.size() : to;
      for (Space space : spaces.subList(offset, to)) {
        ids.add(space.getId());
      }      
    } catch (Exception ex) {
      LOG.error(ex);
    }
    return ids;
  }
  
  @Override
  public String getMapping() {
    JSONObject notAnalyzedField = new JSONObject();
    notAnalyzedField.put("type", "string");
    notAnalyzedField.put("index", "not_analyzed");

    JSONObject properties = new JSONObject();
    properties.put("permissions", notAnalyzedField);
    properties.put("sites", notAnalyzedField);
    properties.put("prettyName", notAnalyzedField);
    properties.put("displayName", notAnalyzedField);
    properties.put("description", notAnalyzedField);
    properties.put("visibility", notAnalyzedField);

    JSONObject mappingProperties = new JSONObject();
    mappingProperties.put("properties", properties);

    JSONObject mappingJSON = new JSONObject();
    mappingJSON.put(getType(), mappingProperties);

    return mappingJSON.toJSONString();
  }

}
