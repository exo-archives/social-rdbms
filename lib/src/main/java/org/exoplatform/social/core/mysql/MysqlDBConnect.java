package org.exoplatform.social.core.mysql;

import java.sql.Connection;

public interface MysqlDBConnect {
  public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  Connection getDBConnection();
}
