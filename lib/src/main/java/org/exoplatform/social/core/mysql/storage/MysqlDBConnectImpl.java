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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.exoplatform.social.core.storage.impl.AbstractStorage;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Huy Quang
 *          quangnh2@exoplatform.com
 * Dec 12, 2013  
 */
public class MysqlDBConnectImpl extends AbstractStorage implements MysqlDBConnect {
  static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

  static final String DB_URL = "jdbc:mysql://localhost:3306/social";

  static final String USER = "root";

  static final String PASS = "root";

  private static final Log LOG = ExoLogger.getLogger(MysqlDBConnectImpl.class);

  public Connection getDBConnection() {
    Connection dbConnection = null;

    try {
      Class.forName(JDBC_DRIVER);
    } catch (ClassNotFoundException e) {
      LOG.error("Driver registered fail:", e.getMessage());
    }

    try {
      dbConnection = DriverManager.getConnection(DB_URL, USER, PASS);
      return dbConnection;
    } catch (SQLException e) {
      LOG.error("Connection fail:", e.getMessage());
    }

    return dbConnection;
  }
}
