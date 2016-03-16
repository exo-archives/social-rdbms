/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.social.addons.storage.dao;

import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.addons.storage.entity.ProfileEntity;
import org.exoplatform.social.addons.storage.entity.ProfileExperienceEntity;
import org.exoplatform.social.addons.test.BaseCoreTest;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;

import javax.persistence.EntityExistsException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class ProfileDAOTest extends BaseCoreTest {
  private IdentityDAO identityDAO;
  private ProfileDAO profileDAO;

  private List<IdentityEntity> deleteIdentities = new ArrayList<>();
  private List<ProfileEntity> deleteProfiles = new ArrayList<>();

  private IdentityEntity identity;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    identityDAO = getService(IdentityDAO.class);
    profileDAO = getService(ProfileDAO.class);

    identity = identityDAO.create(createIdentity());
  }

  @Override
  public void tearDown() throws Exception {
    for (ProfileEntity profile : deleteProfiles) {
      profileDAO.delete(profile);
    }

    for (IdentityEntity identity : deleteIdentities) {
      identityDAO.delete(identity);
    }

    identityDAO.delete(identity);

    super.tearDown();
  }

  public void testCreateProfile() {
    ProfileEntity profile = createProfile();
    profileDAO.create(profile);

    profile = profileDAO.find(profile.getId());
    deleteProfiles.add(profile);
    assertNotNull(profile);
    assertEquals(1, profile.getExperiences().size());
    assertEquals(2, profile.getAvatarImage().length);
    assertEquals(0x01, profile.getAvatarImage()[0]);
  }

  public void testUpdateProfile() {
    ProfileEntity profile = createProfile();
    profileDAO.create(profile);

    profile = profileDAO.find(profile.getId());
    assertNotNull(profile);
    assertEquals("/profile/root", profile.getUrl());

    profile.setUrl("/profile/root_updated");
    profile.setExperiences(new ArrayList<ProfileExperienceEntity>());

    profileDAO.update(profile);

    profile = profileDAO.find(profile.getId());
    deleteProfiles.add(profile);

    assertNotNull(profile);
    assertEquals(0, profile.getExperiences().size());
    assertEquals("/profile/root_updated", profile.getUrl());
  }

  public void testCreateDuplicateProfile() {
    ProfileEntity p1 = createProfile();
    ProfileEntity p2 = createProfile();

    deleteProfiles.add(profileDAO.create(p1));

    try {
      profileDAO.create(p2);
      fail("EntityExistsException should be thrown");
    } catch (EntityExistsException ex) {

    }
  }


  private ProfileEntity createProfile() {
    ProfileEntity profile = new ProfileEntity();
    profile.setIdentity(identity);
    profile.setCreatedTime(System.currentTimeMillis());
    profile.setUrl("/profile/root");
    profile.setAvatarURL("/profile/root/avatar.png");

    profile.setAvatarImage(new byte[]{0x01, 0x02});

    ProfileExperienceEntity exp = new ProfileExperienceEntity();
    exp.setCompany("eXo Platform");
    exp.setPosition("Developer");
    exp.setSkills("Java, Unit test");
    exp.setStartDate("2015-01-01");
    List<ProfileExperienceEntity> exps = new ArrayList<>();
    exps.add(exp);
    profile.setExperiences(exps);

    return profile;
  }

  private IdentityEntity createIdentity() {
    IdentityEntity identity = new IdentityEntity();
    identity.setProviderId(OrganizationIdentityProvider.NAME);
    identity.setRemoteId("user_test_profile");
    identity.setEnable(true);
    identity.setDeleted(false);

    return identity;
  }
}
