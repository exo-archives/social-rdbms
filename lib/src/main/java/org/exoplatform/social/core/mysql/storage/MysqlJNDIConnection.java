package org.exoplatform.social.core.mysql.storage;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class MysqlJNDIConnection extends AbstractMysqlDBConnect {
  private static final Log LOG = ExoLogger.getLogger(MysqlJNDIConnection.class);
  private static final String RESOURCE_NAME = "java:/comp/env/exo-mysql-activity";
  
  @Override
  public Connection getDBConnection() {
    try {
      Context initContext = new InitialContext();
      DataSource datasource = (DataSource) initContext.lookup(RESOURCE_NAME);
      return datasource.getConnection();
    } catch (NamingException ex) {
      LOG.error("Cannot get connection: ", ex);
    } catch (SQLException e) {
      LOG.error("Failed to connect mysql with Resource name " + RESOURCE_NAME + " : ", e);
    }
    return null;
  }
}