package com.mparticle.kits;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.model.Configuration;
import com.mparticle.MPEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * This is an mParticle kit, used to extend the functionality of mParticle SDK. Most Kits are wrappers/adapters
 * to a 3rd party SDK, primarily used to map analogous public mParticle APIs onto a 3rd-party API/platform.
 *
 *
 * Follow the steps below to implement your kit:
 *
 *  - Edit ./build.gradle to add any necessary dependencies, such as your company's SDK
 *  - Rename this file/class, using your company name as the prefix, ie "AcmeKit"
 *  - View the javadocs to learn more about the KitIntegration class as well as the interfaces it defines.
 *  - Choose the additional interfaces that you need and have this class implement them,
 *    ie 'AcmeKit extends KitIntegration implements KitIntegration.PushListener'
 *
 *  In addition to this file, you also will need to edit:
 *  - ./build.gradle (as explained above)
 *  - ./README.md
 *  - ./src/main/AndroidManifest.xml
 *  - ./consumer-proguard.pro
 */
public class BlueshiftKit extends KitIntegration implements KitIntegration.EventListener {
    static final String BLUESHIFT_API_KEY = "blueshift_api_key";
    static final String APP_ICON_INT = "blueshift_app_icon";
    static final String PRODUCT_PAGE_CLASSNAME = "blueshift_product_page_classname";
    static final String CART_PAGE_CLASSNAME = "blueshift_cart_page_classname";
    static final String PROMO_PAGE_CLASSNAME = "blueshift_promo_page_classname";
    static final String NOTIFICATION_SMALL_ICON_INT = "blueshift_notification_small_icon";
    static final String NOTIFICATION_LARGE_ICON_INT = "blueshift_notification_large_icon";
    static final String NOTIFICATION_COLOR_INT = "blueshift_notification_color";
    static final String NOTIFICATION_CHANEL_ID = "blueshift_notification_chanel_id";
    static final String NOTIFICATION_CHANEL_NAME = "blueshift_notification_chanel_name";
    static final String NOTIFICATION_CHANEL_DESCRIPTION = "blueshift_notification_chanel_description";
    static final String DIALOG_THEME_INT = "blueshift_dialog_theme";
    static final String BATCH_INTERVAL_MILLIS_LONG = "blueshift_batch_interval";
    static final String IN_APP_ENABLE_BOOL = "blueshift_in_app_enable";
    static final String IN_APP_JAVASCRIPT_ENABLE_BOOL = "blueshift_in_app_javascript_enable";
    static final String IN_APP_MANUAL_MODE_ENABLE_BOOL = "blueshift_in_app_manual_mode_enable";

    private static final String TAG = "BlueshiftKit";

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        Configuration configuration = new Configuration();

        // == Mandatory Settings ==
        String apiKey = settings.get(BLUESHIFT_API_KEY);
        if (KitUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Blueshift requires a valid API key");
        } else {
            configuration.setApiKey(apiKey);
        }

        try {
            String appIcon = settings.get(APP_ICON_INT);
            int appIconInt = appIcon != null ? Integer.parseInt(appIcon) : -1;
            if (appIconInt != -1) {
                configuration.setAppIcon(appIconInt);
            } else {
                throw new IllegalArgumentException("Blueshift requires a valid app icon resource id");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Blueshift requires a valid app icon resource id");
        }

        // == Deeplink (Optional) ==
        Class productPageClass = getClassFromName(settings.get(PRODUCT_PAGE_CLASSNAME));
        configuration.setProductPage(productPageClass);
        Class cartPageClass = getClassFromName(settings.get(CART_PAGE_CLASSNAME));
        configuration.setCartPage(cartPageClass);
        Class promoPageClass = getClassFromName(settings.get(PROMO_PAGE_CLASSNAME));
        configuration.setOfferDisplayPage(promoPageClass);

        // == Notification (Optional) ==
        int largeIconInt = getIntFromString(settings.get(NOTIFICATION_LARGE_ICON_INT));
        if (largeIconInt != -1) configuration.setLargeIconResId(largeIconInt);
        int smallIconInt = getIntFromString(settings.get(NOTIFICATION_SMALL_ICON_INT));
        if (smallIconInt != -1) configuration.setSmallIconResId(smallIconInt);
        int notificationColor = getIntFromString(settings.get(NOTIFICATION_COLOR_INT));
        if (notificationColor != -1) configuration.setNotificationColor(notificationColor);
        int themeRes = getIntFromString(DIALOG_THEME_INT);
        if (themeRes != -1) configuration.setDialogTheme(themeRes); // for dialog type notifications

        // == Notification Channel (Android O and above) ==
        // Optional: if not set and not found in payload, SDK will assign a default id (bsft_channel_General).
        String channelId = settings.get(NOTIFICATION_CHANEL_ID);
        configuration.setDefaultNotificationChannelId(channelId);
        // Optional: if not set and not found in payload, SDK will assign a default name (General).
        String channelName = settings.get(NOTIFICATION_CHANEL_NAME);
        configuration.setDefaultNotificationChannelName(channelName);
        // optional: only set if present in payload or config object
        String channelDescription = settings.get(NOTIFICATION_CHANEL_DESCRIPTION);
        configuration.setDefaultNotificationChannelDescription(channelDescription);

        // == Batched Events (Optional) ==
        /*
         * This is the time interval used for batching events which are then sent to
         * Blueshift using the bulk events api call. It defaults to 30 min if not set.
         *
         * It is recommended to use one of the following for API < 19 devices.
         * AlarmManager.INTERVAL_FIFTEEN_MINUTES
         * AlarmManager.INTERVAL_HALF_HOUR
         * AlarmManager.INTERVAL_HOUR
         * AlarmManager.INTERVAL_HALF_DAY
         * AlarmManager.INTERVAL_DAY
         */
        long interval = getLongFromString(settings.get(BATCH_INTERVAL_MILLIS_LONG));
        configuration.setBatchInterval(interval);

        // == Enable In-app Messaging Feature (Optional) ==
        boolean isInAppEnabled = getBooleanFromString(settings.get(IN_APP_ENABLE_BOOL));
        configuration.setInAppEnabled(isInAppEnabled);
        boolean isInAppJsEnabled = getBooleanFromString(settings.get(IN_APP_JAVASCRIPT_ENABLE_BOOL));
        configuration.setJavaScriptForInAppWebViewEnabled(isInAppJsEnabled);
        boolean isInAppManualModeEnabled = getBooleanFromString(settings.get(IN_APP_MANUAL_MODE_ENABLE_BOOL));
        configuration.setJavaScriptForInAppWebViewEnabled(isInAppManualModeEnabled);

        Blueshift.getInstance(context).initialize(configuration);

        return null;
    }

    private Class getClassFromName(String classname) {
        Class<?> clazz = null;

        if (!TextUtils.isEmpty(classname)) {
            try {
                clazz = Class.forName(classname);
            } catch (ClassNotFoundException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return clazz;
    }

    private long getLongFromString(String longString) {
        long value = -1;

        try {
            if (TextUtils.isDigitsOnly(longString)) {
                value = Long.parseLong(longString);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return value;
    }

    private int getIntFromString(String intString) {
        int value = -1;

        try {
            if (TextUtils.isDigitsOnly(intString)) {
                value = Integer.parseInt(intString);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return value;
    }

    private boolean getBooleanFromString(String boolString) {
        boolean value = false;

        try {
            if (!TextUtils.isEmpty(boolString)) {
                value = Boolean.valueOf(boolString);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return value;
    }

    @Override
    public String getName() {
        return "Blueshift";
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception e, Map<String, String> map, String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> map) {
        HashMap<String, Object> extras = new HashMap<>();
        extras.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                extras.put(entry.getKey(), entry.getValue());
            }
        }

        Blueshift.getInstance(getContext()).trackEvent(
                BlueshiftConstants.EVENT_PAGE_LOAD, extras, false);

        return null;
    }

    @Nullable
    @Override
    public List<ReportingMessage> logEvent(@NonNull MPEvent event) {
        HashMap<String, Object> extras = null;

        if (event.getCustomAttributes() != null) {
            extras = new HashMap<>();
            for (Map.Entry<String, String> entry : event.getCustomAttributes().entrySet()) {
                extras.put(entry.getKey(), entry.getValue());
            }
        }

        Blueshift.getInstance(getContext()).trackEvent(
                event.getEventName(), extras, false);

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }
}