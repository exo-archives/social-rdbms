package org.exoplatform.social.addons.storage.listener;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ExampleActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(ExampleActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (activity.isComment()) {
      LOG.info(String.format("Migration the comment '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
    } else {
      LOG.info(String.format("Migration the activity '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
    }
  }
}