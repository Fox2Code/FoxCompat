package android.app;

import android.annotation.SystemService;

/**
 * Alternative of {@link StatusBarManager} for samsung devices
 */
@SystemService("sem_statusbar")
public class SemStatusBarManager {
    public void expandNotificationsPanel() {}

    public void expandQuickSettingsPanel() {}

    public void collapsePanels() {}
}
