package org.exoplatform.social.addons.profile;

import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.core.identity.model.Profile;

public class ProfileUtils {

  public static void createOrUpdateProfile(Profile profile, boolean isUpdate) {
    ProfileItemDAO itemDAO = CommonsUtils.getService(ProfileItemDAO.class);
    org.exoplatform.social.addons.storage.entity.Profile profileItem = convertProfile(profile);
    //
    if (isUpdate) {
      Long id = itemDAO.findProfileItemByIdentityId(profile.getIdentity().getId()).getId();
      profileItem.setId(id);
      itemDAO.update(profileItem);
    } else {
      itemDAO.create(profileItem);
    }
  }
  
  private static org.exoplatform.social.addons.storage.entity.Profile convertProfile(Profile p) {
    org.exoplatform.social.addons.storage.entity.Profile profileItem = new org.exoplatform.social.addons.storage.entity.Profile();
    profileItem.setIdentityId(p.getIdentity().getId());
    profileItem.setFirstName(getProfileSimpleValue(p, Profile.FIRST_NAME));
    profileItem.setLastName(getProfileSimpleValue(p, Profile.LAST_NAME));
    profileItem.setFullName(getProfileSimpleValue(p, Profile.FULL_NAME));
    profileItem.setPositions(getProfileSimpleValue(p, Profile.POSITION));
    boolean isDeleted = "true".equals(getProfileSimpleValue(p, Profile.DELETED)) ? true : false;
    profileItem.setDeleted(isDeleted);
    // process for experiences
    putExperienceData(profileItem, p);
    //
    return profileItem;
  }

  private static String getProfileSimpleValue(Profile p, String key) {
    Object o = p.getProperty(key);
    return (o != null) ? String.valueOf(o) : "";
  }
  
  private static void putExperienceData(org.exoplatform.social.addons.storage.entity.Profile profileItem, Profile p) {
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
