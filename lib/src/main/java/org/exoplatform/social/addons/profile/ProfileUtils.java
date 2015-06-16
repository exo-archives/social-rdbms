package org.exoplatform.social.addons.profile;

import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.entity.ProfileItem;
import org.exoplatform.social.core.identity.model.Profile;

public class ProfileUtils {

  public static void createOrUpdateProfile(Profile profile, boolean isUpdate) {
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
  
  private static ProfileItem convertProfile(Profile p) {
    ProfileItem profileItem = new ProfileItem();
    profileItem.setIdentityId(p.getIdentity().getId());
    profileItem.setFirstName(getProfileSimpleValue(p, Profile.FIRST_NAME));
    profileItem.setLastName(getProfileSimpleValue(p, Profile.LAST_NAME));
    profileItem.setFullName(getProfileSimpleValue(p, Profile.FULL_NAME));
    profileItem.setPositions(getProfileSimpleValue(p, Profile.POSITION));
    // process for experiences
    putExperienceData(profileItem, p);
    //
    return profileItem;
  }

  private static String getProfileSimpleValue(Profile p, String key) {
    Object o = p.getProperty(key);
    return (o != null) ? String.valueOf(o) : "";
  }
  
  private static void putExperienceData(ProfileItem profileItem, Profile p) {
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
      profileItem.setPositions(positions.append(" ").append(profileItem.getPositions()).toString());
      profileItem.setJobsDescription(jobsDescription.toString());
      profileItem.setSkills(skills.toString());
    }
  }

  private static void putExperienceData(StringBuilder append, Map<String, String> srcExperience, String key) {
    String value = srcExperience.get(key);
    value = (value != null) ? value : "";
    if (!value.isEmpty() && append.length() > 0) {
      append.append(" ");
    }
    append.append(value);
  }
}
