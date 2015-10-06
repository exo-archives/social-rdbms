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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.elastic.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.api.IdentityStorage;

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

  /** */
  private String index;
  
  /** */
  private List<String> indexFields;

  // SearchResult information
  private String nameElasticFieldName = "name";

  private Map<String, String> sortMapping = new HashMap<String, String>();

  public ProfileIndexingServiceConnector(InitParams initParams,
                                         IdentityManager identityManager,
                                         ConnectionDAO connectionDAO) {
    super(initParams);
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.index = param.getProperty("index");
    this.nameElasticFieldName = param.getProperty("nameField");
    this.indexFields = new ArrayList<String>(Arrays.asList(param.getProperty("indexFields").split(",")));
    //Indicate in which order element will be displayed
    sortMapping.put("name", "name");
    
    this.identityManager = identityManager;
    this.connectionDAO = connectionDAO;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    Identity identity = identityManager.getIdentity(id, true);
    Profile profile = identity.getProfile();
    
    Map<String, String> fields = new HashMap<String, String>();  
    fields.put("name", profile.getFullName());
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
    return new Document(TYPE, id, null, createdDate, null, fields);
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
    return new Document(TYPE, id, null, createdDate, null, fields);
  }

  @Override
  public String delete(String id) {
    return id;
  }

}
