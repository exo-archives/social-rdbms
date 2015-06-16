package org.exoplatform.social.addons.updater.listeners;

import org.exoplatform.container.component.BaseComponentPlugin;

public abstract class RDBMSMigrationListener<T> extends BaseComponentPlugin {
  public abstract void doMigration(T oldData, String newId) throws Exception;
}
