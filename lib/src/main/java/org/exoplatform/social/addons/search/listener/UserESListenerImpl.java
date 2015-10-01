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
package org.exoplatform.social.addons.search.listener;

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.social.addons.search.ProfileIndexingServiceConnector;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 1, 2015  
 */
public class UserESListenerImpl extends UserEventListener {
  
  @Override
  public void preDelete(final User user) throws Exception {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try{
      IdentityManager idm = CommonsUtils.getService(IdentityManager.class);
      Identity identity = idm.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user.getUserName(), false);
      CommonsUtils.getService(IndexingService.class).unindex(ProfileIndexingServiceConnector.TYPE, identity.getId());
    } finally {
      RequestLifeCycle.end();
    }
  }
  
  @Override
  public void postSetEnabled(User user) throws Exception {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      IdentityManager idm = CommonsUtils.getService(IdentityManager.class);
      Identity identity = idm.getOrCreateIdentity(OrganizationIdentityProvider.NAME, user.getUserName(), false);
      if (! user.isEnabled()) {
        CommonsUtils.getService(IndexingService.class).unindex(ProfileIndexingServiceConnector.TYPE, identity.getId());
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

}
