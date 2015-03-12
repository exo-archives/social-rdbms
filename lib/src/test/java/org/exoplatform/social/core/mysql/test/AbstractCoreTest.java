/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.social.core.mysql.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Session;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * @author <a href="mailto:thanhvc@exoplatform.com">Thanh Vu</a>
 * @version $Revision$
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.test.portal-configuration.xml")
})
public abstract class AbstractCoreTest extends BaseExoTestCase {
  private final Log LOG = ExoLogger.getLogger(AbstractCoreTest.class);
  protected SpaceService spaceService;
  protected MysqlDBConnect dbConnect;
  protected Session session;

  @Override
  protected void setUp() throws Exception {
    //
    begin();
    
    spaceService = (SpaceService) getContainer().getComponentInstanceOfType(SpaceService.class);
    dbConnect = (MysqlDBConnect) getContainer().getComponentInstanceOfType(MysqlDBConnect.class);
    
    initDB();
  }

  private void initDB() {
    
    try {
      // clear if existing.
      cleanDB();
      
      //
      String mysqlScriptFilePath = "conf/mysqlDB_script_test.sql";
      String s = new String();
      StringBuffer sb = new StringBuffer();
      URL path = AbstractCoreTest.class.getClassLoader().getResource(mysqlScriptFilePath);
      FileReader fr = new FileReader(new File(path.getPath()));

      BufferedReader br = new BufferedReader(fr);

      while ((s = br.readLine()) != null) {
        sb.append(s);
      }
      br.close();

      String[] inst = sb.toString().split(";");

      Connection con = dbConnect.getDBConnection();
      Statement stmt = con.createStatement();

      for (int i = 0; i < inst.length; i++) {
        if (!inst[i].trim().equals("")) {
          stmt.executeUpdate(inst[i]);
        }
      }

    } catch (Exception e) {
      LOG.error("Failed in executing mysql script.", e);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    //
    end();
    
    cleanDB();
  }
  
  private void cleanDB() {
    String sql = "DROP DATABASE social_test";
    String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'social_test'";
    try {
      Connection con = dbConnect.getDBConnection();
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(query);                  
      rs.next();
      if (rs.getInt("COUNT(*)") > 0) {
        stmt.executeUpdate(sql);
      }
    } catch (Exception e) {
      LOG.error("Failed to drop social database." + e);
    }
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  /**
   * Creates new space with out init apps.
   *
   * @param space
   * @param creator
   * @param invitedGroupId
   * @return
   * @since 1.2.0-GA
   */
  protected Space createSpaceNonInitApps(Space space, String creator, String invitedGroupId) {
    // Creates new space by creating new group
    String groupId = null;
    try {
      groupId = SpaceUtils.createGroup(space.getDisplayName(), creator);
    } catch (SpaceException e) {
      LOG.error("Error while creating group", e);
    }

    if (invitedGroupId != null) {
      // Invites user in group join to new created space.
      // Gets users in group and then invites user to join into space.
      OrganizationService org = (OrganizationService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
      try {
        ListAccess<User> groupMembersAccess = org.getUserHandler().findUsersByGroupId(invitedGroupId);
        List<User> users = Arrays.asList(groupMembersAccess.load(0, groupMembersAccess.getSize()));

        for (User user : users) {
          String userId = user.getUserName();
          if (!userId.equals(creator)) {
            String[] invitedUsers = space.getInvitedUsers();
            if (!ArrayUtils.contains(invitedUsers, userId)) {
              invitedUsers = (String[]) ArrayUtils.add(invitedUsers, userId);
              space.setInvitedUsers(invitedUsers);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to invite users from group " + invitedGroupId, e);
      }
    }
    String[] managers = new String[] { creator };
    space.setManagers(managers);
    space.setGroupId(groupId);
    space.setUrl(space.getPrettyName());
    try {
      spaceService.createSpace(space, creator);
    } catch (Exception e) {
      LOG.warn("Error while saving space", e);
    }
    return space;
  }
  
}
