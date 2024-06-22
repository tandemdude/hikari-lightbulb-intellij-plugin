/*
 * Copyright (c) 2024-present tandemdude
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
