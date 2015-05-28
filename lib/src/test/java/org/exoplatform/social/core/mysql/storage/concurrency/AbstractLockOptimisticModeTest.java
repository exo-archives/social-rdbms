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
package org.exoplatform.social.core.mysql.storage.concurrency;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.mysql.test.BaseCoreTest;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 28, 2015  
 */
public class AbstractLockOptimisticModeTest extends BaseCoreTest {
  
  protected ExoSocialActivity createdActivity = null;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    init();
  }
  
  @Override
  protected void tearDown() throws Exception {
    createdActivity = null;
    super.tearDown();
  }
  
  public void init() {
    doInTransaction(new TransactionCallable<Void>() {
      @Override
      public Void execute(EntityManager em) {
        String activityTitle = "activity title";
        ExoSocialActivity activity = createActivity(activityTitle, maryIdentity.getId());
        createdActivity = activityStorage.saveActivity(johnIdentity, activity);
        return null;
      }
    });
  }
  
  protected ExoSocialActivity createActivity(String activityTitle, String posterId) {
    //
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(activityTitle);
    activity.setTitleId("TitleID: "+ activity.getTitle());
    activity.setType("UserActivity");
    activity.setBody("Body of "+ activity.getTitle());
    activity.setBodyId("BodyId of "+ activity.getTitle());
    activity.setLikeIdentityIds(new String[]{"demo", "mary"});
    activity.setMentionedIds(new String[]{"demo", "john"});
    activity.setCommentedIds(new String[]{});
    activity.setReplyToId(new String[]{});
    activity.setAppId("AppID");
    activity.setExternalId("External ID");
    activity.setPosterId(posterId);
    activity.isComment(false);
    
    Map<String, String> templateParams = new LinkedHashMap<String, String>();
    templateParams.put("key1", "value 1");
    templateParams.put("key2", "value 2");
    templateParams.put("key3", "value 3");
    activity.setTemplateParams(templateParams);
    
    return activity;
  }

}
