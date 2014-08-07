package org.exoplatform.social.core.mysql.storage;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.mysql.MysqlDBConnect;

public class MysqlJNDIConnection implements MysqlDBConnect {
  private static final Log LOG = ExoLogger.getLogger(MysqlJNDIConnection.class);

  @Override
  public Connection getDBConnection() {
    String DATASOURCE_CONTEXT = "exo-mysql-activity";

    Connection result = null;
    try {
      Context initContext = new InitialContext();
      Context envContext = (Context) initContext.lookup("java:/comp/env");
      DataSource datasource = (DataSource) envContext
          .lookup(DATASOURCE_CONTEXT);
      if (datasource != null) {
        result = datasource.getConnection();
      } else {
        LOG.error("Failed to lookup datasource.");
      }
    } catch (NamingException ex) {
      LOG.error("Cannot get connection: " + ex);
    } catch (SQLException ex) {
      LOG.error("Cannot get connection: " + ex);
    }
    return result;
  }

}
