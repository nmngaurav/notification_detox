package aura.notification.filter.util

/**
 * AnalyticsConstants
 * Centralized constants for Firebase Analytics events and parameters
 */
object AnalyticsConstants {
    // Events
    const val EVENT_APP_LAUNCH = "app_launch"
    const val EVENT_ONBOARDING_START = "onboarding_start"
    const val EVENT_PERMISSION_GRANTED = "permission_granted"
    const val EVENT_APP_SCAN_COMPLETE = "app_scan_complete"
    const val EVENT_ONBOARDING_COMPLETE = "onboarding_complete"
    const val EVENT_SHIELD_STATE_CHANGE = "shield_state_change"
    const val EVENT_APP_CONTROL_OPENED = "app_control_opened"
    const val EVENT_SHIELD_LEVEL_CHANGE = "shield_level_change"
    const val EVENT_PAYWALL_VIEW = "paywall_view"
    const val EVENT_SUBSCRIPTION_OPTION_SELECT = "subscription_option_select"
    const val EVENT_PURCHASE_START = "purchase_start"
    const val EVENT_PURCHASE_SUCCESS = "purchase_success"
    const val EVENT_PURCHASE_FAIL = "purchase_fail"
    const val EVENT_UPDATE_POPUP_VIEW = "update_popup_view"
    const val EVENT_UPDATE_ACTION = "update_action"
    const val EVENT_NOTIFICATION_CLEAR = "notification_clear"

    // Parameters
    const val PARAM_VERSION = "version"
    const val PARAM_SCAN_COUNT = "scan_count"
    const val PARAM_ENABLED = "enabled"
    const val PARAM_PACKAGE_NAME = "package_name"
    const val PARAM_LEVEL = "level"
    const val PARAM_SOURCE = "source"
    const val PARAM_PLAN_ID = "plan_id"
    const val PARAM_PRICE = "price"
    const val PARAM_ERROR_CODE = "error_code"
    const val PARAM_AVAILABLE_VERSION = "available_v"
    const val PARAM_CURRENT_VERSION = "current_v"
    const val PARAM_ACTION = "action"
    const val PARAM_APP_COUNT = "app_count"
}
