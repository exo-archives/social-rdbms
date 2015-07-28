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
package org.exoplatform.social.addons.storage.dao;

import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.social.addons.storage.entity.Profile;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.storage.IdentityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * June 09, 2015  
 */
public interface ProfileItemDAO extends GenericDAO<Profile, Long> {

  Profile findProfileItemByIdentityId(String identityId);
  
  List<Profile> getIdentitiesForMentions(ProfileFilter profileFilter, long offset, long limit);
  
  int getIdentitiesByProfileFilterCount(ProfileFilter profileFilter);
  
  List<Profile> getIdentitiesByFirstCharacterOfName(ProfileFilter profileFilter, long offset, long limit);
  
  int getIdentitiesByFirstCharacterOfNameCount(ProfileFilter profileFilter);
  
  List<Profile> getIdentitiesForUnifiedSearch(ProfileFilter profileFilter, long offset, long limit);
  
  List<Profile> getIdentitiesByProfileFilter(ProfileFilter profileFilter, long offset, long limit) throws IdentityStorageException;
  
}
