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

import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.entity.ProfileItem;
import org.exoplatform.social.core.identity.model.Profile;
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

  private void createOrUpdateProfile(Profile profile, boolean isUpdate) {
    ProfileItemDAO itemDAO = CommonsUtils.getService(ProfileItemDAO.class);
    ProfileItem profileItem = convertProfile(profile);
    //
    if (isUpdate) {
      Long id = itemDAO.findProfileItemByIdentityId(profile.getIdentity().getId()).getId();
      profileItem.setId(id);
      itemDAO.update(profileItem);
    } else {
      itemDAO.create(profileItem);
    }
  }
  
  private ProfileItem convertProfile(Profile p) {
    ProfileItem profileItem = new ProfileItem();
    profileItem.setIdentityId(p.getIdentity().getId());
    profileItem.setFirstName(getProfileSimpleValue(p, Profile.FIRST_NAME));
    profileItem.setLastName(getProfileSimpleValue(p, Profile.LAST_NAME));
    profileItem.setFullName(getProfileSimpleValue(p, Profile.FULL_NAME));
    profileItem.setPosition(getProfileSimpleValue(p, Profile.POSITION));
    // process for experiences
    putExperienceData(profileItem, p);
    //
    return profileItem;
  }

  private String getProfileSimpleValue(Profile p, String key) {
    Object o = p.getProperty(key);
    return (o != null) ? String.valueOf(o) : "";
  }
  
  private void putExperienceData(ProfileItem profileItem, Profile p) {
    List<Map<String, String>> experiences = (List<Map<String, String>>) p.getProperty(Profile.EXPERIENCES);
    if (experiences != null) {
      StringBuilder skills = new StringBuilder();
      StringBuilder positions = new StringBuilder();
      StringBuilder organizations = new StringBuilder();
      StringBuilder jobsDescription = new StringBuilder();
      for (Map<String, String> experience : experiences) {
        putExperienceData(organizations, experience, Profile.EXPERIENCES_COMPANY);
        putExperienceData(positions, experience, Profile.EXPERIENCES_POSITION);
        putExperienceData(jobsDescription, experience, Profile.EXPERIENCES_DESCRIPTION);
        putExperienceData(skills, experience, Profile.EXPERIENCES_SKILLS);
      }
      profileItem.setOrganizations(organizations.toString());
      profileItem.setPositions(positions.toString());
      profileItem.setJobsDescription(jobsDescription.toString());
      profileItem.setSkills(skills.toString());
    }
  }

  private static void putExperienceData(StringBuilder append, Map<String, String> srcExperience, String key) {
    String value = srcExperience.get(key);
    value = (value != null) ? value : "";
    if (!value.isEmpty() && append.length() > 0) {
      append.append(",");
    }
    append.append(value);
  }
}
