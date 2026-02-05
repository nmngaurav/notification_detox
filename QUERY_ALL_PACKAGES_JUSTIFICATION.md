# Google Play QUERY_ALL_PACKAGES Permission Justification

## Core Purpose
**Notification Management and Filtering System**

Aura is a notification management and filtering app that allows users to configure notification rules for all installed apps on their device. The app's core functionality requires visibility into all installed applications to enable proactive notification management.

## Feature Requiring QUERY_ALL_PACKAGES

### **App Configuration and Rule Management**

**Description:**
The app provides a settings interface where users can configure notification filtering rules for all installed apps BEFORE they receive notifications. This requires the app to:

1. **Display all installed apps** in a settings screen so users can:
   - Select which apps to configure
   - Set custom filtering rules (shield levels, keywords, categories)
   - Enable/disable notification filtering per app
   - View which apps have notification permissions enabled

2. **Onboarding experience** where users can:
   - See all apps that can send notifications
   - Quickly configure rules for high-noise apps (social media, messaging)
   - Set up initial filtering preferences

3. **Proactive configuration** - Users need to set rules BEFORE notifications arrive, not reactively after receiving them. This is essential for:
   - Focus mode sessions
   - Work/study time blocks
   - Do Not Disturb periods

**Why QUERY_ALL_PACKAGES is Required:**
- Users must be able to configure notification rules for ALL apps that might send notifications
- The app needs to check which apps have notification permissions enabled
- Users need to see the complete list of installed apps to make informed decisions
- Without this permission, only a subset of apps would be visible (those matching `<queries>` declarations), making the app incomplete and non-functional for its core purpose

**Similar to:**
- Antivirus apps need to see all apps to scan them
- Device search apps need to see all apps to index them
- File managers need to see all apps to manage app data

**Privacy:**
- All processing occurs locally on the device
- No app inventory data is collected, stored, or transmitted
- The permission is used solely for displaying apps in the UI for user configuration

## Alternative Approaches Considered

1. **Using `<queries>` with intent filters**: This would only show apps matching specific intent filters, not all apps. Users would be unable to configure rules for apps not matching those filters, breaking core functionality.

2. **Dynamic discovery via notifications**: While the app receives notifications via `NotificationListenerService`, users need to configure rules proactively, not reactively. Waiting for notifications to arrive defeats the purpose of focus mode and proactive filtering.

## Conclusion

The `QUERY_ALL_PACKAGES` permission is essential for the app's core functionality of allowing users to configure notification management rules for all installed apps. Without this permission, the app cannot fulfill its primary purpose of providing comprehensive notification management and filtering.
