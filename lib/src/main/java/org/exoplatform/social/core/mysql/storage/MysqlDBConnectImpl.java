/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.mysql.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Dec 12, 2013  
 */
public class MysqlDBConnectImpl extends AbstractMysqlDBConnect {
  private static final Log    LOG             = ExoLogger.getLogger(MysqlDBConnectImpl.class);

  private static final String DB_CONF_INFO    = "/conf/dbinfo.properties";
  private static final String DB_URL_KEY      = "db.url";
  private static final String DB_SOC_NAME_KEY = "db.social.name";
  private static final String DB_USER_KEY     = "db.username";
  private static final String DB_PASS_KEY     = "db.password";

  public Connection getDBConnection() {
    try {
      Class.forName(JDBC_DRIVER);
    } catch (ClassNotFoundException e) {
      LOG.error("Driver registered fail:", e);
      return null;
    }

    Properties props = getProperties();
    try {
      return DriverManager.getConnection(props.getProperty(DB_URL_KEY) + props.getProperty(DB_SOC_NAME_KEY),
                                         props.getProperty(DB_USER_KEY), props.getProperty(DB_PASS_KEY));
    } catch (SQLException e) {
      try {
        createDatabase(props);
        //
        return getDBConnection();
      } catch (SQLException e2) {
        LOG.error("Connection fail:", e.getMessage());
        return null;
      }
    }
  }
  
  private Properties getProperties() {
    Properties props = new Properties(PrivilegedSystemHelper.getProperties());
    if(!props.containsKey(DB_SOC_NAME_KEY)) {
      try {
        props.load(getClass().getResourceAsStream(DB_CONF_INFO));
      } catch (IOException e) {
        LOG.error("Can not load properties file:" + DB_CONF_INFO, e.getMessage());
      }
    }
    return props;
  }
  
  private void createDatabase(Properties props) throws SQLException {
    Connection con = DriverManager.getConnection(props.getProperty(DB_URL_KEY) + 
                                                 "?user=" + props.getProperty(DB_USER_KEY) +
                                                 "&password=" + props.getProperty(DB_PASS_KEY));
    con.createStatement().executeUpdate("CREATE DATABASE " + props.getProperty(DB_SOC_NAME_KEY));
  }
}
