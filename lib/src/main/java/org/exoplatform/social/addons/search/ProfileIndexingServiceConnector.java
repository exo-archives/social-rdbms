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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.elastic.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 29, 2015  
 */
public class ProfileIndexingServiceConnector extends ElasticIndexingServiceConnector {

  private String index;
  private List<String> searchFields;

  //SearchResult information
  private String nameElasticFieldName = "name";

  private Map<String, String> sortMapping = new HashMap<String, String>();
  
  public ProfileIndexingServiceConnector(InitParams initParams) {
    super(initParams);
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.index = param.getProperty("index");
    this.nameElasticFieldName = param.getProperty("nameField");
    this.searchFields = new ArrayList<String>(Arrays.asList(param.getProperty("searchFields").split(",")));
    //Indicate in which order element will be displayed
    sortMapping.put("name", "name");
  }
  
  @Override
  public Document create(String id) {
      return null;
  }

  @Override
  public Document update(String id) {
      return null;
  }

  @Override
  public String delete(String id) {
      return null;
  }

}