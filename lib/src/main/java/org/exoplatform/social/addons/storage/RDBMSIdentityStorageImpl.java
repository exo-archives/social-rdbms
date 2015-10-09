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
package org.exoplatform.social.addons.storage;

import java.util.List;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.search.ProfileSearchConnector;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.storage.IdentityStorageException;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 5, 2015  
 */
public class RDBMSIdentityStorageImpl extends IdentityStorageImpl {
  @Override
  public List<Identity> getIdentitiesByFirstCharacterOfName(String providerId,
                                                            ProfileFilter profileFilter,
                                                            long offset,
                                                            long limit,
                                                            boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.search(null, profileFilter, null, offset, limit);
  }
  
  @Override
  public List<Identity> getIdentitiesForMentions(String providerId,
                                                 ProfileFilter profileFilter,
                                                 long offset,
                                                 long limit,
                                                 boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.search(null, profileFilter, null, offset, limit);
  }
  
  @Override
  public int getIdentitiesByProfileFilterCount(String providerId, ProfileFilter profileFilter) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.count(null, profileFilter, null);
  }
  
  @Override
  public int getIdentitiesByFirstCharacterOfNameCount(String providerId, ProfileFilter profileFilter) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.count(null, profileFilter, null);
  }
}
