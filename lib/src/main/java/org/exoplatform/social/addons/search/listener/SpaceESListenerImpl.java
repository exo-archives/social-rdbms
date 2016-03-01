package org.exoplatform.social.addons.search.listener;

import org.exoplatform.addons.es.index.IndexingOperationProcessor;
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
  public void spaceAccessEdited(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for setting space hidden!");
  }

  @Override
  public void addInvitedUser(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for adding invited user to space !");
  }

  @Override
  public void addPendingUser(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for adding pending user to space !");
  }

  @Override
  public void applicationActivated(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for active app in space !");
  }

  @Override
  public void applicationAdded(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for adding app to space !");
  }

  @Override
  public void applicationDeactivated(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for deactivating app in space !");
  }

  @Override
  public void applicationRemoved(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for removing app in space !");
  }

  @Override
  public void grantedLead(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for adding manager user to space !");
  }

  @Override
  public void joined(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for adding user to space !");
  }

  @Override
  public void left(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for removing user from space !");
  }

  @Override
  public void revokedLead(SpaceLifeCycleEvent event) {
    reindex(event);
    LOG.debug("Handle re-index for removing manager from space !");
  }

  @Override
  public void spaceAvatarEdited(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.index(SpaceIndexingServiceConnector.TYPE, event.getSpace().getId());
    IndexingOperationProcessor indexProcessor = CommonsUtils.getService(IndexingOperationProcessor.class);
    indexProcessor.process();
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
    IndexingOperationProcessor indexProcessor = CommonsUtils.getService(IndexingOperationProcessor.class);
    indexProcessor.process();
    //Workaround due to lack of refresh api from elastic search addon
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
  }
}
