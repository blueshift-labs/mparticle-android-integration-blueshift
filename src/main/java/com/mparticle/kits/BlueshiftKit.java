package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.inappmessage.InAppApiCallback;
import com.blueshift.model.Configuration;
import com.blueshift.model.UserInfo;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

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
public class BlueshiftKit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.UserAttributeListener, KitIntegration.IdentityListener, KitIntegration.PushListener {
    static final String BLUESHIFT_API_KEY = "blueshift_api_key";
    static final String PRODUCT_PAGE_CLASSNAME = "blueshift_product_page_classname";
    static final String CART_PAGE_CLASSNAME = "blueshift_cart_page_classname";
    static final String PROMO_PAGE_CLASSNAME = "blueshift_promo_page_classname";
    static final String NOTIFICATION_SMALL_ICON_NAME = "blueshift_notification_small_icon";
    static final String NOTIFICATION_LARGE_ICON_NAME = "blueshift_notification_large_icon";
    static final String NOTIFICATION_COLOR_NAME = "blueshift_notification_color";
    static final String NOTIFICATION_CHANEL_ID = "blueshift_notification_chanel_id";
    static final String NOTIFICATION_CHANEL_NAME = "blueshift_notification_chanel_name";
    static final String NOTIFICATION_CHANEL_DESCRIPTION = "blueshift_notification_chanel_description";
    static final String DIALOG_THEME_NAME = "blueshift_dialog_theme";
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
            ApplicationInfo applicationInfo = getContext().getApplicationInfo();
            configuration.setAppIcon(applicationInfo.icon);
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
        int largeIconInt = getDrawableIdFromString(settings.get(NOTIFICATION_LARGE_ICON_NAME));
        if (largeIconInt != -1) configuration.setLargeIconResId(largeIconInt);
        int smallIconInt = getDrawableIdFromString(settings.get(NOTIFICATION_SMALL_ICON_NAME));
        if (smallIconInt != -1) configuration.setSmallIconResId(smallIconInt);
        int notificationColor = getDrawableIdFromString(settings.get(NOTIFICATION_COLOR_NAME));
        if (notificationColor != -1) configuration.setNotificationColor(notificationColor);
        int themeRes = getStyleIdFromString(settings.get(DIALOG_THEME_NAME));
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

    private int getResourceIdFromString(String resourceType, String resourceName) {
        try {
            if (!TextUtils.isEmpty(resourceType) && !TextUtils.isEmpty(resourceName)) {
                return getContext()
                        .getResources()
                        .getIdentifier(resourceName, resourceType, getContext().getPackageName());
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return -1;
    }

    private int getDrawableIdFromString(String resourceName) {
        return getResourceIdFromString("drawable", resourceName);
    }

    private int getStyleIdFromString(String resourceName) {
        return getResourceIdFromString("style", resourceName);
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

    // BEGIN - InApp Messages
    public static void registerForInAppMessages(Activity activity) {
        Blueshift.getInstance(activity).registerForInAppMessages(activity);
    }

    public static void unregisterForInAppMessages(Activity activity) {
        Blueshift.getInstance(activity).unregisterForInAppMessages(activity);
    }

    public static void fetchInAppMessages(Context context, InAppApiCallback callback) {
        Blueshift.getInstance(context).fetchInAppMessages(callback);
    }

    public static void displayInAppMessage(Context context) {
        Blueshift.getInstance(context).displayInAppMessages();
    }
    // END - InApp Messages

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

    @Override
    public void onIncrementUserAttribute(String s, int i, String s1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onRemoveUserAttribute(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttribute(String key, Object value, FilteredMParticleUser filteredMParticleUser) {
        UserInfo userInfo = UserInfo.getInstance(getContext());

        if (key != null) {
            switch (key) {
                case MParticle.UserAttributes.FIRSTNAME:
                    if (value != null) userInfo.setFirstname(String.valueOf(value));
                    break;
                case MParticle.UserAttributes.LASTNAME:
                    if (value != null) userInfo.setLastname(String.valueOf(value));
                    break;
                case MParticle.UserAttributes.GENDER:
                    if (value != null) userInfo.setGender(String.valueOf(value));
                    break;
                case MParticle.UserAttributes.AGE:
                    // No setter
                    break;
                case MParticle.UserAttributes.ADDRESS:
                    // No setter
                    break;
                case MParticle.UserAttributes.MOBILE_NUMBER:
                    // No setter
                    break;
                case MParticle.UserAttributes.CITY:
                    // No Setter
                    break;
                case MParticle.UserAttributes.STATE:
                    // No setter
                    break;
                case MParticle.UserAttributes.ZIPCODE:
                    // No setter
                    break;
                case MParticle.UserAttributes.COUNTRY:
                    // No setter
                    break;
            }

            userInfo.save(getContext());
        }
    }

    @Override
    public void onSetUserTag(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttributeList(String s, List<String> list, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }

    @Override
    public void onConsentStateUpdated(ConsentState consentState, ConsentState consentState1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        return true;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {
        Map<String, String> map = new HashMap<>();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            for (String key : bundle.keySet()) {
                String val = bundle.getString(key);
                if (val != null) map.put(key, val);
            }
        }

//        BlueshiftMessagingService service = new BlueshiftMessagingService();
//        service.handleDataMessage(getContext(), map);
    }

    @Override
    public boolean onPushRegistration(String s, String s1) {
        return false;
    }

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        updateUser(mParticleUser);
    }

    private void updateUser(MParticleUser user) {
        if (user != null) {
            UserInfo userInfo = UserInfo.getInstance(getContext());

            String email = user.getUserIdentities().get(MParticle.IdentityType.Email);
            userInfo.setEmail(email);

            String customerId = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
            userInfo.setRetailerCustomerId(customerId);

            String fbId = user.getUserIdentities().get(MParticle.IdentityType.Facebook);
            userInfo.setFacebookId(fbId);

            userInfo.save(getContext());

            // whenever user is updated, and email is non-empty, we should call an identify
            if (email != null) {
                Blueshift.getInstance(getContext())
                        .identifyUserByEmail(email, null, false);
            }
        }
    }
}