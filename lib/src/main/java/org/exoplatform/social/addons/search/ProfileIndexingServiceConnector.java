/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;

import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.relationship.model.Relationship;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Sep
 * 29, 2015
 */
public class ProfileIndexingServiceConnector extends ElasticIndexingServiceConnector {
  public final static String TYPE = "profile"; 
  /** */
  private final IdentityManager identityManager;
  /** */
  private final ConnectionDAO connectionDAO;

  private final IdentityDAO identityDAO;

  public ProfileIndexingServiceConnector(InitParams initParams,
                                         IdentityManager identityManager,
                                         IdentityDAO identityDAO,
                                         ConnectionDAO connectionDAO) {
    super(initParams);
    this.identityManager = identityManager;
    this.identityDAO = identityDAO;
    this.connectionDAO = connectionDAO;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    Identity identity = identityManager.getIdentity(id, true);
    Profile profile = identity.getProfile();
    
    Map<String, String> fields = new HashMap<>();
    fields.put("name", profile.getFullName());
    fields.put("firstName", (String) profile.getProperty(Profile.FIRST_NAME));
    fields.put("lastName", (String) profile.getProperty(Profile.LAST_NAME));
    fields.put("position", profile.getPosition());
    fields.put("skills", (String)profile.getProperty(Profile.EXPERIENCES_SKILLS));
    fields.put("avatarUrl", profile.getAvatarUrl());
    fields.put("userName", identity.getRemoteId());
    fields.put("email", profile.getEmail());
    Date createdDate = new Date(profile.getCreatedTime());
    //confirmed connections
    List<Connection> connections = connectionDAO.getConnections(identity, Relationship.Type.CONFIRMED, 0, -1);
    String connectionsStr = buildConnectionsToStr(identity, connections);
    if (connectionsStr.length() > 0) {
      fields.put("connections", connectionsStr);
    }
    
    //outgoing connections
    connections = connectionDAO.getConnections(identity, Relationship.Type.OUTGOING, 0, -1);
    connectionsStr = buildConnectionsToStr(identity, connections);
    if (connectionsStr.length() > 0) {
      fields.put("outgoings", connectionsStr);
    }
    //incoming connections
    connections = connectionDAO.getConnections(identity, Relationship.Type.INCOMING, 0, -1);
    connectionsStr = buildConnectionsToStr(identity, connections);
    if (connectionsStr.length() > 0) {
      fields.put("incomings", connectionsStr);
    }
    return new Document(TYPE, id, null, createdDate, (Set<String>)null, fields);
  }
  /**
   * Gets the identityId from connection list
   * @param identity
   * @param connections
   * @return
   */
  private String buildConnectionsToStr(Identity identity, List<Connection> connections) {
    StringBuilder sb = new StringBuilder();
    String identityId = identity.getId();
    for(Connection con : connections) {
      if (identityId.equals(con.getSenderId())) {
        sb.append(con.getReceiverId()).append(",");
      } else {
        sb.append(con.getSenderId()).append(",");
      }
    }
    //Remove the last ","
    if (sb.length()>0) {
      sb.deleteCharAt(sb.length()-1);
    }
    return sb.toString();
  }

  @Override
  public Document update(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    Identity identity = identityManager.getIdentity(id, true);
    Profile profile = identity.getProfile();
    
    Map<String, String> fields = new HashMap<String, String>();  
    fields.put("name", profile.getFullName());
    fields.put("firstName", (String) profile.getProperty(Profile.FIRST_NAME));
    fields.put("lastName", (String) profile.getProperty(Profile.LAST_NAME));
    fields.put("position", profile.getPosition());
    fields.put("skills", (String)profile.getProperty(Profile.EXPERIENCES_SKILLS));
    fields.put("avatarUrl", profile.getAvatarUrl());
    fields.put("userName", identity.getRemoteId());
    fields.put("email", profile.getEmail());
    Date createdDate = new Date(profile.getCreatedTime());
    //confirmed connections
    List<Connection> connections = connectionDAO.getConnections(identity, Relationship.Type.CONFIRMED, 0, -1);
    String connectionsStr = buildConnectionsToStr(identity, connections);
    if (connectionsStr.length() > 0) {
      fields.put("connections", connectionsStr);
    }
    //outgoing connections
    connections = connectionDAO.getConnections(identity, Relationship.Type.OUTGOING, 0, -1);
    connectionsStr = buildConnectionsToStr(identity, connections);
    if (connectionsStr.length() > 0) {     
      fields.put("outgoings", connectionsStr);
    }
    //incoming connections
    connections = connectionDAO.getConnections(identity, Relationship.Type.INCOMING, 0, -1);
    connectionsStr = buildConnectionsToStr(identity, connections);
    
    if (connectionsStr.length() > 0) {     
      fields.put("incomings", connectionsStr);
    }
    return new Document(TYPE, id, null, createdDate, (Set<String>)null, fields);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    List<Long> ids = identityDAO.getAllIds(offset, limit);

    if (ids == null || ids.isEmpty()) {
      return new ArrayList<>();
    } else {
      List<String> result = new ArrayList<>(ids.size());
      for (Long id : ids) {
        result.add(String.valueOf(id));
      }
      return result;
    }
  }
  
  @Override
  public String getMapping() {
    JSONObject postingHighlighterField = new JSONObject();
    postingHighlighterField.put("type", "string");
    postingHighlighterField.put("index_options", "offsets");

    JSONObject notAnalyzedField = new JSONObject();
    notAnalyzedField.put("type", "string");
    notAnalyzedField.put("index", "not_analyzed");

    JSONObject properties = new JSONObject();
    properties.put("permissions", notAnalyzedField);
    properties.put("sites", notAnalyzedField);
    properties.put("userName", notAnalyzedField);    
    properties.put("email", notAnalyzedField);
    
    properties.put("name", postingHighlighterField);
    properties.put("firstName", postingHighlighterField);
    properties.put("lastName", postingHighlighterField);
    properties.put("position", postingHighlighterField);
    properties.put("skills", postingHighlighterField);

    JSONObject mappingProperties = new JSONObject();
    mappingProperties.put("properties", properties);

    JSONObject mappingJSON = new JSONObject();
    mappingJSON.put(getType(), mappingProperties);

    return mappingJSON.toJSONString();
  }

}
