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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.addons.es.client.ElasticSearchingClient;
import org.exoplatform.addons.es.search.ElasticSearchException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.core.search.Sorting;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.StorageUtils;

public class SpaceSearchConnector {
  private static final Log             LOG = ExoLogger.getLogger(SpaceSearchConnector.class);

  private final ElasticSearchingClient client;

  private String                       index;

  private String                       searchType;

  public SpaceSearchConnector(InitParams initParams, ElasticSearchingClient client) {
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.index = param.getProperty("index");
    this.searchType = param.getProperty("searchType");
    this.client = client;
  }

  public List<Space> search(ESSpaceFilter spaceFilter, long offset, long limit) {
    String esQuery = buildQueryStatement(spaceFilter, offset, limit);
    String jsonResponse = this.client.sendRequest(esQuery, this.index, this.searchType);
    return buildResult(jsonResponse);
  }

  public int count(ESSpaceFilter spaceFilter) {
    String esQuery = buildQueryStatement(spaceFilter, 0, 1);
    String jsonResponse = this.client.sendRequest(esQuery, this.index, this.searchType);
    return getCount(jsonResponse);
  }

  private int getCount(String jsonResponse) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);
    JSONParser parser = new JSONParser();

    Map<?, ?> json = null;
    try {
      json = (Map<?, ?>) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult == null)
      return 0;

    int count = Integer.parseInt(jsonResult.get("total").toString());
    return count;
  }

  private List<Space> buildResult(String jsonResponse) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    List<Space> results = new LinkedList<Space>();
    JSONParser parser = new JSONParser();

    Map json = null;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (Exception e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult == null)
      return results;

    //
    try {
      JSONArray jsonHits = (JSONArray) jsonResult.get("hits");
      for (Object jsonHit : jsonHits) {
        String spaceId = (String) ((JSONObject) jsonHit).get("_id");
        Space space = CommonsUtils.getService(SpaceStorage.class).getSpaceSimpleById(spaceId);
        fillMember(space, (JSONObject) jsonHit);

        results.add(space);
      }
    } catch (Exception ex) {
      LOG.error(ex);
    }
    return results;
  }

  private void fillMember(Space space, JSONObject jsonHit) {
    JSONObject jSpace = (JSONObject) jsonHit.get("_source");
    String pendings = (String) jSpace.get(Status.PENDING.name());
    if (pendings != null && !pendings.isEmpty()) {
      space.setPendingUsers(pendings.split(" "));
    }

    String inviteds = (String) jSpace.get(Status.INVITED.name());
    if (inviteds != null && !inviteds.isEmpty()) {
      space.setInvitedUsers(inviteds.split(" "));
    }

    String members = (String) jSpace.get(Status.MEMBER.name());
    if (members != null && !members.isEmpty()) {
      space.setMembers(members.split(" "));
    }

    String managers = (String) jSpace.get(Status.MANAGER.name());
    if (managers != null && !managers.isEmpty()) {
      space.setManagers(managers.split(" "));
    }
  }

  private String buildQueryStatement(ESSpaceFilter filter, long offset, long limit) {
    JSONObject query = new JSONObject();
    query.put("from", offset);
    query.put("size", limit < 0 ? Integer.MAX_VALUE : limit);

    query.put("sort", buildSort(filter));

    JSONObject queryString = buildQueryString(filter);
    if (queryString != null) {
      query.put("query", queryString);
    } else {
      JSONObject matchAll = new JSONObject();
      matchAll.put("match_all", new JSONObject());
      query.put("query", matchAll);
    }

    LOG.debug("Search Query request to ES : {} ", query.toString());

    return query.toString();
  }

  private JSONObject buildQueryString(ESSpaceFilter filter) {
    JSONObject result = new JSONObject();
    JSONObject query = new JSONObject();

    try {
      String query_string = buildExpression(filter);
      if (query_string.isEmpty()) {
        return null;
      }
      query.put("query", buildExpression(filter));
      result.put("query_string", query);
    } catch (Exception ex) {
      LOG.error(ex);
    }
    return result;
  }

  private JSONArray buildSort(ESSpaceFilter filter) {
    //
    Sorting sorting;
    if (filter == null) {
      sorting = new Sorting(Sorting.SortBy.TITLE, Sorting.OrderBy.ASC);
    } else {
      sorting = filter.getSorting();
    }

    JSONObject jSort = new JSONObject();
    JSONObject jOrder = new JSONObject();
    jOrder.put("order", sorting.orderBy.name().toLowerCase());

    //
    switch (sorting.sortBy) {
    case DATE:
      jSort.put("createdTime", jOrder);
      break;
    case TITLE:
      jSort.put("prettyName", jOrder);
      break;
    case RELEVANCY:
      jSort.put("_score", jOrder);
      break;
    }

    JSONArray result = new JSONArray();
    result.add(jSort);
    return result;
  }

  private String buildExpression(ESSpaceFilter filter) {
    StringBuilder esExp = new StringBuilder();

    //
    String searchCondition = filter.getSpaceNameSearchCondition();
    if (searchCondition != null && searchCondition.length() > 0) {
      searchCondition = StorageUtils.ASTERISK_STR + normalize(searchCondition) + StorageUtils.ASTERISK_STR;
      esExp.append("(prettyName:").append(searchCondition);
      esExp.append(" OR ")
           .append("displayName:")
           .append(searchCondition)
           .append(" OR ")
           .append("description: ")
           .append(StringEscapeUtils.escapeHtml(searchCondition))
           .append(")");
    }

    if (filter.isNotHidden()) {
      if (esExp.length() > 0) {
        esExp.append(" AND ");
      }
      esExp.append("visibility:(-").append(Space.HIDDEN).append(")");
    }

    Map<Status, List<String>> statusMap = buildStatus(filter.getStatusMap());
    if (!statusMap.isEmpty() || filter.isIncludePrivate()) {
      if (esExp.length() > 0) {
        esExp.append(" AND ");
      }

      StringBuilder statusStr = new StringBuilder();
      for (Status status : statusMap.keySet()) {
        if (statusStr.length() > 0) {
          statusStr.append(" OR ");
        }
        statusStr.append(status.name()).append(":(");
        statusStr.append(StringUtils.join(statusMap.get(status), " OR "));
        statusStr.append(")");
      }
      if (filter.isIncludePrivate()) {
        if (statusStr.length() > 0) {
          statusStr.append(" OR ");
        }
        statusStr.append("visibility:(").append(Space.PRIVATE).append(")");
      }

      esExp.append("(").append(statusStr.toString()).append(")");
    }

    String appId = filter.getAppId();
    if (appId != null && !appId.isEmpty()) {
      appId = StorageUtils.ASTERISK_STR + normalize(appId) + StorageUtils.ASTERISK_STR;
      if (esExp.length() > 0) {
        esExp.append(" AND ");
      }
      esExp.append("appId:").append(appId);
    }

    String prettyName = filter.getPrettyName();
    if (prettyName != null && !prettyName.isEmpty()) {
      if (esExp.length() > 0) {
        esExp.append(" AND ");
      }
      esExp.append("prettyName:").append(normalize(prettyName));
    }

    String displayName = filter.getDisplayName();
    if (displayName != null && !displayName.isEmpty()) {
      if (esExp.length() > 0) {
        esExp.append(" AND ");
      }
      esExp.append("displayName:").append(normalize(displayName));
    }

    //
    char firstChar = filter.getFirstCharacterOfSpaceName();
    if (firstChar != '\u0000' && !Character.isDigit(firstChar)) {
      if (esExp.length() > 0) {
        esExp.append(" AND ");
      }
      esExp.append("prettyName:").append(normalize(String.valueOf(firstChar))).append(StorageUtils.ASTERISK_STR);
    }

    return esExp.toString();
  }

  private Map<Status, List<String>> buildStatus(Map<String, Set<Status>> statusMap) {
    Map<Status, List<String>> result = new HashMap<>();

    for (String userId : statusMap.keySet()) {
      for (Status status : statusMap.get(userId)) {
        List<String> ids = result.get(status);
        if (ids == null) {
          ids = new LinkedList<>();
          result.put(status, ids);
        }
        ids.add(userId);
      }
    }
    return result;
  }

  private String normalize(String input) {
    String cleanString = input.replaceAll("\\*", "");
    cleanString = cleanString.replaceAll("\\%", "");
    return cleanString.toLowerCase();
  }
}
