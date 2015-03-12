package org.exoplatform.social.core.mysql;

import java.sql.Connection;

public interface MysqlDBConnect {
  /**
   * @return
   */
  Connection getDBConnection();
}
