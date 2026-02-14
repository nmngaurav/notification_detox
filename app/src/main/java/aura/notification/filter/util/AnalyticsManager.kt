package aura.notification.filter.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AnalyticsManager
 * Hilt singleton for logging Firebase Analytics events
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(event: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(event, params)
    }

    fun logOnboardingStart() {
        logEvent(AnalyticsConstants.EVENT_ONBOARDING_START)
    }

    fun logOnboardingComplete() {
        logEvent(AnalyticsConstants.EVENT_ONBOARDING_COMPLETE)
    }

    fun logShieldStateChange(enabled: Boolean) {
        val bundle = Bundle().apply {
            putBoolean(AnalyticsConstants.PARAM_ENABLED, enabled)
        }
        logEvent(AnalyticsConstants.EVENT_SHIELD_STATE_CHANGE, bundle)
    }

    fun logPaywallView(source: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_SOURCE, source)
        }
        logEvent(AnalyticsConstants.EVENT_PAYWALL_VIEW, bundle)
    }

    fun logSubscriptionSelect(planId: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_PLAN_ID, planId)
        }
        logEvent(AnalyticsConstants.EVENT_SUBSCRIPTION_OPTION_SELECT, bundle)
    }

    fun logPurchaseStart(planId: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_PLAN_ID, planId)
        }
        logEvent(AnalyticsConstants.EVENT_PURCHASE_START, bundle)
    }

    fun logPurchaseSuccess(planId: String, price: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_PLAN_ID, planId)
            putString(AnalyticsConstants.PARAM_PRICE, price)
        }
        logEvent(AnalyticsConstants.EVENT_PURCHASE_SUCCESS, bundle)
    }

    fun logPurchaseFail(planId: String, errorCode: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_PLAN_ID, planId)
            putString(AnalyticsConstants.PARAM_ERROR_CODE, errorCode)
        }
        logEvent(AnalyticsConstants.EVENT_PURCHASE_FAIL, bundle)
    }

    fun logUpdatePopupView(currentV: String, availableV: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_CURRENT_VERSION, currentV)
            putString(AnalyticsConstants.PARAM_AVAILABLE_VERSION, availableV)
        }
        logEvent(AnalyticsConstants.EVENT_UPDATE_POPUP_VIEW, bundle)
    }

    fun logUpdateAction(action: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsConstants.PARAM_ACTION, action)
        }
        logEvent(AnalyticsConstants.EVENT_UPDATE_ACTION, bundle)
    }
}
