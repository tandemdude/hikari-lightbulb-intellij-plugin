package io.github.tandemdude.hklbsupport.utils;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Utility wrapper class to simplify the processes of creating formatted notifications.
 */
public class Notifier {
    static void notify(Project project, NotificationType type, String format, Object... vars) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Hikari Lightbulb Support")
                .createNotification(vars.length == 0 ? format : String.format(format, vars), type)
                .notify(project);
    }

    /**
     * Create a notification at {@link NotificationType#INFORMATION} level. Acts like
     * {@link String#format(String, Object...)} when {@code vars} are passed, otherwise notifies
     * using {@code format} as the raw message.
     *
     * @param project the project to create the notification under.
     * @param format the message text, optionally with format specifiers.
     * @param vars the items to use to format the message.
     */
    public static void notifyInformation(Project project, String format, Object... vars) {
        notify(project, NotificationType.INFORMATION, format, vars);
    }

    /**
     * Create a notification at {@link NotificationType#WARNING} level. Acts like
     * {@link String#format(String, Object...)} when {@code vars} are passed, otherwise notifies
     * using {@code format} as the raw message.
     *
     * @param project the project to create the notification under.
     * @param format the message text, optionally with format specifiers.
     * @param vars the items to use to format the message.
     */
    public static void notifyWarning(Project project, String format, Object... vars) {
        notify(project, NotificationType.WARNING, format, vars);
    }
}
