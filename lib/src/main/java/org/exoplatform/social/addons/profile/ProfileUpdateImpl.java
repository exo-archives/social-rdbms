/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.profile;

import static org.exoplatform.social.addons.profile.ProfileUtils.createOrUpdateProfile;

import org.exoplatform.social.core.profile.ProfileLifeCycleEvent;
import org.exoplatform.social.core.profile.ProfileListenerPlugin;

public class ProfileUpdateImpl extends ProfileListenerPlugin {

  @Override
  public void avatarUpdated(ProfileLifeCycleEvent event) {
  }

  @Override
  public void basicInfoUpdated(ProfileLifeCycleEvent event) {
    createOrUpdateProfile(event.getProfile(), true);
  }

  @Override
  public void contactSectionUpdated(ProfileLifeCycleEvent event) {
    createOrUpdateProfile(event.getProfile(), true);
  }

  @Override
  public void experienceSectionUpdated(ProfileLifeCycleEvent event) {
    createOrUpdateProfile(event.getProfile(), true);
  }

  @Override
  public void headerSectionUpdated(ProfileLifeCycleEvent event) {
    createOrUpdateProfile(event.getProfile(), true);
  }

  @Override
  public void createProfile(ProfileLifeCycleEvent event) {
    createOrUpdateProfile(event.getProfile(), false);
  }

  @Override
  public void aboutMeUpdated(ProfileLifeCycleEvent event) {
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj)) {
      return true;
    }
    if (obj instanceof ProfileUpdateImpl) {
      return getName().equals(((ProfileUpdateImpl) obj).getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }
}
