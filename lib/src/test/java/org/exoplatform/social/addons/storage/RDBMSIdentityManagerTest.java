/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.storage;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.addons.profile.ProfileUtils;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.test.AbstractCoreTest;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.profile.ProfileFilter;

/**
 * Unit Tests for {@link RelationshipManager}
 *
 */
public class RDBMSIdentityManagerTest extends AbstractCoreTest {
  private IdentityManager identityManager;
  private List<Identity>  tearDownIdentityList;
  private ProfileItemDAO itemDAO;

//  private Identity rootIdentity, demoIdentity, johnIdentity, maryIdentity, ghostIdentity, paulIdentity;


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    identityManager = getService(IdentityManager.class);
    itemDAO = getService(ProfileItemDAO.class);
    tearDownIdentityList = new ArrayList<Identity>();
    //
    org.exoplatform.services.security.Identity identity = getService(IdentityRegistry.class).getIdentity("root");
    ConversationState.setCurrent(new ConversationState(identity));
  }

  @Override
  protected void tearDown() throws Exception {
    for (Identity identity : tearDownIdentityList) {
      identityManager.deleteIdentity(identity);
    }
    for (org.exoplatform.social.addons.storage.entity.Profile profile : itemDAO.findAll()) {
      itemDAO.delete(profile);
    }
    super.tearDown();
  }
  
  public void testGetIdentitiesByProfileFilter() throws Exception {
    org.exoplatform.social.addons.storage.entity.Profile rootProfile = itemDAO.findProfileItemByIdentityId(rootIdentity.getId());
    rootProfile.setDeleted(true);
    itemDAO.update(rootProfile);
    org.exoplatform.social.addons.storage.entity.Profile demoProfile = itemDAO.findProfileItemByIdentityId(demoIdentity.getId());
    demoProfile.setDeleted(true);
    itemDAO.update(demoProfile);
    org.exoplatform.social.addons.storage.entity.Profile johnProfile = itemDAO.findProfileItemByIdentityId(johnIdentity.getId());
    johnProfile.setDeleted(true);
    itemDAO.update(johnProfile);
    org.exoplatform.social.addons.storage.entity.Profile maryProfile = itemDAO.findProfileItemByIdentityId(maryIdentity.getId());
    maryProfile.setDeleted(true);
    itemDAO.update(maryProfile);
    
    String providerId = OrganizationIdentityProvider.NAME;
    populateIdentities(5, true);

    ProfileFilter pf = new ProfileFilter();
    ListAccess<Identity> idsListAccess = null;
    { // Test cases with name of profile.
      // Filter identity by first character.
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, true);
      assertEquals(5, idsListAccess.getSize());
      assertEquals(5, idsListAccess.load(0, 10).length);
      //
      pf.setFirstCharacterOfName('F');
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(0, idsListAccess.getSize());
      pf.setFirstCharacterOfName('L');
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      pf.setFirstCharacterOfName('N');
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(0, idsListAccess.getSize());
      
      // Filter identity by name.
      pf.setFirstCharacterOfName('\u0000');
      pf.setName("FirstName");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("FirstName1");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(1, idsListAccess.getSize());
      
      //
      pf.setName("");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("*");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("n%me");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("n*me");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("%me");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("%name%");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("n%me");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("fir%n%me");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("noname");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(0, idsListAccess.getSize());
    }
    
    { // Test cases with position of profile.
      pf.setName("");
      pf.setPosition("dev");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setPosition("d%v");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setPosition("test");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(0, idsListAccess.getSize());
    }
    
    { // Test cases with gender of profile.
      pf.setPosition("");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
    }
    
    { // Other test cases
      pf.setName("n**me%");
      pf.setPosition("*%");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(5, idsListAccess.getSize());
      
      //
      pf.setName("noname");
      pf.setPosition("*%");
      idsListAccess = identityManager.getIdentitiesByProfileFilter(providerId, pf, false);
      assertNotNull(idsListAccess);
      assertEquals(0, idsListAccess.getSize());
    }

    //Tests with the case: add new identity and delete it after that to check
    {
      ProfileFilter profileFilter = new ProfileFilter();
      ListAccess<Identity> identityListAccess = identityManager.getIdentitiesByProfileFilter("organization", profileFilter, false);
      assertEquals(5, identityListAccess.getSize());
      
      //
      Identity testIdentity = populateIdentity("test", false);
      identityListAccess = identityManager.getIdentitiesByProfileFilter("organization", profileFilter, false);
      assertEquals(6, identityListAccess.getSize());
      
      //
      identityManager.deleteIdentity(testIdentity);
      org.exoplatform.social.addons.storage.entity.Profile testProfile = itemDAO.findProfileItemByIdentityId(testIdentity.getId());
      testProfile.setDeleted(true);
      itemDAO.update(testProfile);
      identityListAccess = identityManager.getIdentitiesByProfileFilter("organization", profileFilter, false);
      assertEquals(5, identityListAccess.getSize());
    }

    //Test with excluded identity list
    {
      Identity excludeIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "username1", false);
      List<Identity> excludedIdentities = new ArrayList<Identity>();
      excludedIdentities.add(excludeIdentity);
      ProfileFilter profileFilter = new ProfileFilter();
      profileFilter.setExcludedIdentityList(excludedIdentities);
      ListAccess<Identity> identityListAccess = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, profileFilter, false);
      assertEquals(4, identityListAccess.getSize());
      Identity[] identityArray = identityListAccess.load(0, 3);
      assertEquals(3, identityArray.length);
    }
  }
  
  private void populateIdentities(int numberOfItems, boolean addedToTearDownList) {
    String providerId = "organization";
    for (int i = 0; i < numberOfItems; i++) {
      String remoteId = "username" + i;
      Identity identity = new Identity(providerId, remoteId);
      identityManager.saveIdentity(identity);
      Profile profile = new Profile(identity);
      profile.setProperty(Profile.FIRST_NAME, "FirstName" + i);
      profile.setProperty(Profile.LAST_NAME, "LastName" + i);
      profile.setProperty(Profile.FULL_NAME, "FirstName" + i + " " +  "LastName" + i);
      profile.setProperty(Profile.POSITION, "developer");
      profile.setProperty(Profile.GENDER, "male");

      identityManager.saveProfile(profile);
      identity.setProfile(profile);
      ProfileUtils.createOrUpdateProfile(profile, false);
      if (addedToTearDownList) {
        tearDownIdentityList.add(identity);
      }
    }
  }
  
  private Identity populateIdentity(String remoteId, boolean addedToTearDownList) {
    String providerId = "organization";
    Identity identity = new Identity(providerId, remoteId);
    identityManager.saveIdentity(identity);

    Profile profile = new Profile(identity);
    profile.setProperty(Profile.FIRST_NAME, remoteId);
    profile.setProperty(Profile.LAST_NAME, "gtn");
    profile.setProperty(Profile.FULL_NAME, remoteId + " " +  "gtn");
    profile.setProperty(Profile.POSITION, "developer");
    profile.setProperty(Profile.GENDER, "male");

    identityManager.saveProfile(profile);
    ProfileUtils.createOrUpdateProfile(profile, false);

    if (addedToTearDownList) {
      tearDownIdentityList.add(identity);
    }
    return identity;
  }

}
