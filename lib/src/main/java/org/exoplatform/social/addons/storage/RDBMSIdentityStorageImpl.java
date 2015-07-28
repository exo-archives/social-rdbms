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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.storage.IdentityStorageException;
import org.exoplatform.social.core.storage.synchronization.SynchronizedIdentityStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 21, 2015  
 */
public class RDBMSIdentityStorageImpl extends SynchronizedIdentityStorage {
  
  private final ProfileItemDAO profileItemDAO;

  public RDBMSIdentityStorageImpl(ProfileItemDAO profileItemDAO) {
    super();
    this.profileItemDAO = profileItemDAO;
  }
  
  @Override
  public List<Identity> getIdentitiesForMentions(final String providerId, final ProfileFilter profileFilter, long offset, long limit,
                                                 boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    return convertProfileItemsToIdentities(profileItemDAO.getIdentitiesForMentions(profileFilter, offset, limit));
  }
  
  @Override
  public int getIdentitiesByProfileFilterCount(final String providerId, final ProfileFilter profileFilter)
                                                throws IdentityStorageException {
    return profileItemDAO.getIdentitiesByFirstCharacterOfNameCount(profileFilter);
  }
  
  @Override
  public List<Identity> getIdentitiesByFirstCharacterOfName(final String providerId, final ProfileFilter profileFilter,
                                                            long offset, long limit, boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    return convertProfileItemsToIdentities(profileItemDAO.getIdentitiesByFirstCharacterOfName(profileFilter, offset, limit));
  }
  
  @Override
  public int getIdentitiesByFirstCharacterOfNameCount(final String providerId, final ProfileFilter profileFilter)
                                                      throws IdentityStorageException {
    return profileItemDAO.getIdentitiesByFirstCharacterOfNameCount(profileFilter);
  }
  
  @Override
  public List<Identity> getIdentitiesForUnifiedSearch(final String providerId, ProfileFilter profileFilter,
                                                      long offset, long limit) throws IdentityStorageException {
    return convertProfileItemsToIdentities(profileItemDAO.getIdentitiesForUnifiedSearch(profileFilter, offset, limit));
  }

  private List<Identity> convertProfileItemsToIdentities(List<Profile> profiles) {
    List<Identity> identities = new ArrayList<Identity>();
    if (profiles == null || profiles.size() == 0) return identities;
    for (Profile profile : profiles) {
      Identity identity = convertProfileItemToIdentity(profile);
      org.exoplatform.social.core.identity.model.Profile socProfile = loadProfile(new org.exoplatform.social.core.identity.model.Profile(identity));
      identity.setProfile(socProfile);
      identities.add(identity);
    }
    return identities;
  }
  
  @Override
  public List<Identity> getIdentitiesByProfileFilter(final String providerId, final ProfileFilter profileFilter, long offset, long limit,
                                                     boolean forceLoadOrReloadProfile)
                                                     throws IdentityStorageException {
    return convertProfileItemsToIdentities(profileItemDAO.getIdentitiesByProfileFilter(profileFilter, offset, limit));
  }
  
  private Identity convertProfileItemToIdentity(Profile profile) {
    return findIdentityById(profile.getIdentityId());
  }
}
