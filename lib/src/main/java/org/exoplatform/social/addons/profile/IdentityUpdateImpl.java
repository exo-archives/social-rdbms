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
package org.exoplatform.social.addons.profile;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 21, 2015  
 */
public class IdentityUpdateImpl extends UserEventListener {
  
  private static final Log LOG = ExoLogger.getLogger(IdentityUpdateImpl.class);

  @Override
  public void preDelete(final User user) throws Exception {
    //Update status of profile on RDBMS
    ProfileItemDAO itemDAO = CommonsUtils.getService(ProfileItemDAO.class);
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    try {
      Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user.getUserName(), true);
      Profile profile = itemDAO.findProfileItemByIdentityId(identity.getId());
      profile.setDeleted(identity.isDeleted());
      itemDAO.update(profile);
    } catch (Exception e) {
      LOG.error("Failed to updated the status of profile to deleted");
    }
    
  }
}
