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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.exoplatform.social.core.storage.impl.AbstractStorage;

/**
 * Created by The eXo Platform SAS
 * Dec 12, 2013  
 */
public class MysqlDBConnectImpl extends AbstractStorage implements MysqlDBConnect {

  private static final Log LOG = ExoLogger.getLogger(MysqlDBConnectImpl.class);
  private static final String DB_INFO_TEST = "conf/dbinfo_test.properties";

  public Connection getDBConnection() {
    Connection dbConnection = null;

    try {
      Class.forName(JDBC_DRIVER);
    } catch (ClassNotFoundException e) {
      LOG.error("Driver registered fail:", e.getMessage());
    }

    try {
      Properties props = new Properties();
      props.load(ClassLoader.getSystemResourceAsStream(DB_INFO_TEST));
      String username = props.getProperty("db.username");
      String password = props.getProperty("db.password");
      String dbUrl = props.getProperty("db.social_test.url");
          
      dbConnection = DriverManager.getConnection(dbUrl, username, password);
      return dbConnection;
    } catch (SQLException e) {
      LOG.error("Connection fail:", e.getMessage());
    } catch (IOException e) {
      LOG.error("Failed in getting properties information.", e);
    }

    return dbConnection;
  }
}
