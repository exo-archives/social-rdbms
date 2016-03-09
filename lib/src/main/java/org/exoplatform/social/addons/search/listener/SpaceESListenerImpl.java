package org.exoplatform.social.addons.search.listener;

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.search.SpaceIndexingServiceConnector;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;

public class SpaceESListenerImpl extends SpaceListenerPlugin {

  private static final Log LOG = ExoLogger.getExoLogger(SpaceESListenerImpl.class);

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.index(SpaceIndexingServiceConnector.TYPE, event.getSpace().getId());
    LOG.debug("Handled create index for newly created space!");
  }

  @Override
  public void spaceDescriptionEdited(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for editing description of space !");
  }

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.unindex(SpaceIndexingServiceConnector.TYPE, event.getSpace().getId());
    LOG.debug("Handle un-index for removing space !");
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for renaming space !");
  }

  private void reindex(SpaceLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.reindex(SpaceIndexingServiceConnector.TYPE, event.getSpace().getId());
  }

  @Override
  public void spaceAccessEdited(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void applicationActivated(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void applicationAdded(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void applicationDeactivated(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void applicationRemoved(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void grantedLead(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void joined(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void left(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void revokedLead(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void spaceAvatarEdited(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addInvitedUser(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addPendingUser(SpaceLifeCycleEvent event) {
    // TODO Auto-generated method stub

  }
}
