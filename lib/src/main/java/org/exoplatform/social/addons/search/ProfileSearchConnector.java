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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.exoplatform.addons.es.client.ElasticSearchingClient;
import org.exoplatform.addons.es.search.ElasticSearchException;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 29, 2015  
 */
public class ProfileSearchConnector {
  private static final Log LOG = ExoLogger.getLogger(ProfileSearchConnector.class);
  private final ElasticSearchingClient client;
  private List<String> searchFields;
  private String index;
  private String searchType;
  
  public ProfileSearchConnector(InitParams initParams, ElasticSearchingClient client) {
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.searchFields = new ArrayList<String>(Arrays.asList(param.getProperty("searchFields").split(",")));
    this.index = param.getProperty("index");
    this.searchType = param.getProperty("searchType");
    this.client = client;
  }

  public Collection<Identity> search(SearchContext context,
                                         ProfileFilter filter,
                                         int offset,
                                         int limit) {
    
    String esQuery = buildQueryStatement(filter, offset, limit);
    String jsonResponse = this.client.sendRequest(esQuery, this.index, this.searchType);
    return buildResult(jsonResponse);
  }
  
  private Collection<Identity> buildResult(String jsonResponse) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    Collection<Identity> results = new ArrayList<Identity>();
    JSONParser parser = new JSONParser();

    Map json = null;
    try {
      json = (Map)parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

    Identity identity = null;
    Profile p;
    for(Object jsonHit : jsonHits) {
      JSONObject hitSource = (JSONObject) ((JSONObject) jsonHit).get("_source");
      String position = (String) hitSource.get("position");
      String name = (String) hitSource.get("name");
      String userName = (String) hitSource.get("userName");
      String avatarUrl = (String) hitSource.get("avatarUrl");
      String email = (String) hitSource.get("email");
      String profileId = (String) hitSource.get("_id");
      identity = new Identity(OrganizationIdentityProvider.NAME, userName);
      p = new Profile(identity);
      p.setAvatarUrl(avatarUrl);
      p.setProperty(Profile.FULL_NAME, name);
      p.setProperty(Profile.POSITION, position);
      p.setId(profileId);
      results.add(identity);
    }
    return results;
  }
  
  
  private String buildQueryStatement(ProfileFilter filter, int offset, int limit) {
    StringBuilder esQuery = new StringBuilder();
    esQuery.append("{\n");
    esQuery.append("     \"from\" : " + offset + ", \"size\" : " + limit + ",\n");
    esQuery.append("       \"sort\": [\n");
    esQuery.append("              {    \n");
    esQuery.append("              \"name\": {\n");
    esQuery.append("               \"order\": \"desc\")\n");
    esQuery.append("               }\n");
    esQuery.append("               ],\n");
    esQuery.append("     \"query\": {\n");
    esQuery.append("        \"filtered\" : {\n");
    esQuery.append("            \"query\" : {\n");
    esQuery.append("                \"query_string\" : {\n");
    esQuery.append("                    \"query\" : \"" + buildExpression(filter) + "\"\n");
    esQuery.append("                }\n");
    esQuery.append("            }\n");
    esQuery.append("        }\n");
    esQuery.append("     }\n");
    esQuery.append("}");

    LOG.debug("Search Query request to ES : {} ", esQuery);

    return esQuery.toString();
  }
  
  private String buildExpression(ProfileFilter filter) {
    StringBuilder esExp = new StringBuilder();
    String inputName = filter.getName().replace(StorageUtils.ASTERISK_STR, StorageUtils.SPACE_STR);
    if (inputName != null && inputName.length() > 0) {
      esExp.append("name:").append(inputName);
    }

    //skills
    String skills = filter.getSkills().replace(StorageUtils.ASTERISK_STR, StorageUtils.SPACE_STR);
    if (skills != null && skills.length() > 0) {
      if (esExp.length() > 0) {
        esExp.append(" OR ");
      }
      esExp.append("skills:").append(skills);
    }
    
  //skills
    String postition = filter.getPosition().replace(StorageUtils.ASTERISK_STR, StorageUtils.SPACE_STR);
    if (skills != null && skills.length() > 0) {
      if (esExp.length() > 0) {
        esExp.append(" OR ");
      }
      esExp.append("position:").append(postition);
    }
    return esExp.toString();
  }
}
