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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;

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
  protected IdentityManager identityManager;
  protected RelationshipManager relationshipManager;
  protected ActivityManager activityManager;
  protected ActivityStorageImpl activityStorageImpl;

  protected Identity rootIdentity;
  protected Identity johnIdentity;
  protected Identity maryIdentity;
  protected Identity demoIdentity;

  protected Session session;

  @Override
  protected void setUp() throws Exception {
    //
    begin();
    loginUser("root");
    identityManager = getService(IdentityManager.class);
    activityManager =  getService(ActivityManager.class);
    activityStorageImpl = getService(ActivityStorageImpl.class);
    relationshipManager = getService(RelationshipManager.class);
    spaceService = getService(SpaceService.class);
    //
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
  }
  @Override
  protected void tearDown() throws Exception {
    //
    end();
    
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
  

  public void loginUser(String userId) {
    MembershipEntry membershipEntry = new MembershipEntry("/platform/user", "*");
    Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
    membershipEntries.add(membershipEntry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(userId, membershipEntries);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
  }
  
}
