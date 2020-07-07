package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLinksHandler;
import com.blueshift.BlueshiftLinksListener;
import com.blueshift.BlueshiftLogger;
import com.blueshift.fcm.BlueshiftMessagingService;
import com.blueshift.inappmessage.InAppApiCallback;
import com.blueshift.model.Configuration;
import com.blueshift.model.UserInfo;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.DeviceUtils;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;
import com.mparticle.kits.blueshift.BuildConfig;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlueshiftKit extends KitIntegration implements
        KitIntegration.EventListener,
        KitIntegration.CommerceListener,
        KitIntegration.UserAttributeListener,
        KitIntegration.PushListener,
        KitIntegration.IdentityListener {

    private static final String TAG = "BlueshiftKit";

    // settings
    private static final String BLUESHIFT_EVENT_API_KEY = "eventApiKey";
    private static final String BLUESHIFT_SHOULD_LOG_MP_EVENTS = "blueshift_should_log_mp_events";
    private static final String BLUESHIFT_SHOULD_LOG_USER_EVENTS = "blueshift_should_log_user_events";
    private static final String BLUESHIFT_SHOULD_LOG_COMMERCE_EVENTS = "blueshift_should_log_commerce_events";
    private static final String BLUESHIFT_SHOULD_LOG_SCREEN_VIEW_EVENTS = "blueshift_should_log_screen_view_events";

    // payload
    private static final String BSFT_KIT_VERSION = "bsft_mparticle_kit_version";
    private static final String BSFT_MESSAGE_UUID = "bsft_message_uuid";

    // local configuration
    private static Configuration blueshiftConfiguration;
    private boolean shouldLogMPEvents = false;
    private boolean shouldLogUserEvents = true;
    private boolean shouldLogCommerceEvents = false;
    private boolean shouldLogScreenViewEvents = false;

    public static void setBlueshiftConfig(@NonNull Configuration config) {
        blueshiftConfiguration = config;
    }

    public static void registerForInAppMessages(@NonNull Activity activity) {
        Blueshift.getInstance(activity).registerForInAppMessages(activity);
    }

    public static void unregisterForInAppMessages(@NonNull Activity activity) {
        Blueshift.getInstance(activity).unregisterForInAppMessages(activity);
    }

    public static void fetchInAppMessages(@NonNull Context context, InAppApiCallback callback) {
        Blueshift.getInstance(context).fetchInAppMessages(callback);
    }

    public static void displayInAppMessage(@NonNull Context context) {
        Blueshift.getInstance(context).displayInAppMessages();
    }

    public static boolean isBlueshiftUniversalLink(Uri uri) {
        return BlueshiftLinksHandler.isBlueshiftLink(uri);
    }

    public static void handleBlueshiftUniversalLinks(Context context, Intent intent, BlueshiftLinksListener listener) {
        new BlueshiftLinksHandler(context).handleBlueshiftUniversalLinks(intent, listener);
    }

    public static void handleBlueshiftUniversalLinks(Context context, Uri link, Bundle extras, BlueshiftLinksListener listener) {
        new BlueshiftLinksHandler(context).handleBlueshiftUniversalLinks(link, extras, listener);
    }

    private boolean getBooleanSettings(Map<String, String> settings, String key, boolean defaultValue) {
        if (settings != null && key != null && settings.containsKey(key)) {
            try {
                String val = settings.get(key);
                return Boolean.parseBoolean(val);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return defaultValue;
    }

    private void logTrackingConfig(String eventName, boolean status, String key) {
        BlueshiftLogger.i(
                TAG,
                "Sending \"" + eventName + " Events\" directly to Blueshift " + (status ? "enabled." : "disabled! To Enable, set \"" + key + "=true\" in settings.")
        );
    }

    private void logSettings(Map<String, String> settings) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("mParticle settings\n");
            builder.append("{\n");
            Set<String> keys = settings.keySet();
            boolean first = true;
            for (String key : keys) {
                if (first) {
                    first = false;
                    builder.append("\t");
                } else {
                    builder.append(",");
                    builder.append("\n");
                    builder.append("\t");
                }
                builder.append("\"").append(key).append("\" : \"").append(settings.get(key)).append("\"");
            }
            builder.append("\n}");

            BlueshiftLogger.i(TAG, builder.toString());
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        if (blueshiftConfiguration == null) {
            BlueshiftLogger.d(TAG, "Blueshift configuration is not provided. Using the default one.");
            blueshiftConfiguration = new Configuration();
        }

        String apiKey = settings.get(BLUESHIFT_EVENT_API_KEY);
        if (KitUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Blueshift requires a valid API key");
        } else {
            blueshiftConfiguration.setApiKey(apiKey);
        }

        shouldLogMPEvents = getBooleanSettings(settings, BLUESHIFT_SHOULD_LOG_MP_EVENTS, false);
        shouldLogUserEvents = getBooleanSettings(settings, BLUESHIFT_SHOULD_LOG_USER_EVENTS, true);
        shouldLogCommerceEvents = getBooleanSettings(settings, BLUESHIFT_SHOULD_LOG_COMMERCE_EVENTS, false);
        shouldLogScreenViewEvents = getBooleanSettings(settings, BLUESHIFT_SHOULD_LOG_SCREEN_VIEW_EVENTS, false);

        logTrackingConfig("MP", shouldLogMPEvents, BLUESHIFT_SHOULD_LOG_MP_EVENTS);
        logTrackingConfig("Commerce", shouldLogCommerceEvents, BLUESHIFT_SHOULD_LOG_COMMERCE_EVENTS);
        logTrackingConfig("Identify", shouldLogUserEvents, BLUESHIFT_SHOULD_LOG_USER_EVENTS);
        logTrackingConfig("ScreenView", shouldLogScreenViewEvents, BLUESHIFT_SHOULD_LOG_SCREEN_VIEW_EVENTS);

        logSettings(settings);

        // set app-icon as notification icon if not set
        if (blueshiftConfiguration.getAppIcon() == 0) {
            try {
                ApplicationInfo applicationInfo = getContext().getApplicationInfo();
                blueshiftConfiguration.setAppIcon(applicationInfo.icon);
            } catch (Exception e) {
                throw new IllegalArgumentException("Blueshift requires a valid app icon resource id");
            }
        }

        Blueshift.getInstance(context).initialize(blueshiftConfiguration);

        return null;
    }

    @Override
    public String getName() {
        return "Blueshift";
    }

    // ** event logging support methods **

    private HashMap<String, Object> getExtras(Map<String, String> extras) {
        HashMap<String, Object> newExtras = new HashMap<>();

        if (extras != null) {
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                newExtras.put(entry.getKey(), entry.getValue());
            }
        }

        return newExtras;
    }

    // ** KitIntegration.EventListener **

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
        if (shouldLogScreenViewEvents) {
            HashMap<String, Object> extras = getExtras(map);
            extras.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

            Blueshift.getInstance(getContext()).trackEvent(
                    BlueshiftConstants.EVENT_PAGE_LOAD,
                    extras,
                    false
            );
        }

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), map));
        return messages;
    }

    @Nullable
    @Override
    public List<ReportingMessage> logEvent(@NonNull MPEvent event) {
        if (shouldLogMPEvents) {
            HashMap<String, Object> extras = getExtras(event.getCustomAttributes());

            Blueshift.getInstance(getContext()).trackEvent(
                    event.getEventName(),
                    extras,
                    false
            );
        }

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    // ** KitIntegration.CommerceListener **

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal, BigDecimal bigDecimal1, String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        if (shouldLogCommerceEvents) {
            HashMap<String, Object> extras = getExtras(commerceEvent.getCustomAttributes());

            String productAction = commerceEvent.getProductAction();
            if (productAction != null) {
                Blueshift.getInstance(getContext()).trackEvent(
                        productAction,
                        extras,
                        false
                );
            }
        }

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(ReportingMessage.fromEvent(this, commerceEvent));
        return messages;
    }

    // ** KitIntegration.UserAttributeListener **

    @Override
    public void onIncrementUserAttribute(String s, int i, String s1, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    @Override
    public void onRemoveUserAttribute(String key, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    @Override
    public void onSetUserAttribute(String key, Object value, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    @Override
    public void onSetUserTag(String s, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    @Override
    public void onSetUserAttributeList(String s, List<String> list, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }

    @Override
    public void onConsentStateUpdated(ConsentState consentState, ConsentState consentState1, FilteredMParticleUser filteredMParticleUser) {
        updateBlueshiftUserInfo(filteredMParticleUser);
    }

    /**
     * This method is invoked each time when {@link UserAttributeListener} callback methods are invoked.
     *
     * This method is responsible for firing an identify call to Blueshift directly after updating
     * details inside {@link UserInfo} object.
     *
     * @param filteredMParticleUser user object sent by mParticle
     */
    private void updateBlueshiftUserInfo(FilteredMParticleUser filteredMParticleUser) {
        if (filteredMParticleUser != null) {
            UserInfo userInfo = UserInfo.getInstance(getContext());

            // identity
            Map<MParticle.IdentityType, String> identities = filteredMParticleUser.getUserIdentities();

            String customerId = identities.get(MParticle.IdentityType.CustomerId);
            userInfo.setRetailerCustomerId(customerId);

            String email = identities.get(MParticle.IdentityType.Email);
            userInfo.setEmail(email);

            userInfo.save(getContext());

            invokeBlueshiftIdentify();
        }
    }

    // ** KitIntegration.PushListener **

    private boolean isBlueshiftPush(Intent intent) {
        Bundle bundle = intent != null ? intent.getExtras() : null;
        Set<String> keys = bundle != null ? bundle.keySet() : null;
        return keys != null && keys.contains(BSFT_MESSAGE_UUID);
    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        Configuration config = BlueshiftUtils.getConfiguration(getContext());
        if (config != null && !config.isPushEnabled()) {
            BlueshiftLogger.w(TAG, "Blueshift push handling is disabled. Skipping...");
            return false;
        }

        boolean isBlueshiftPush = isBlueshiftPush(intent);
        if (!isBlueshiftPush) {
            BlueshiftLogger.d(TAG, "Push message from outside Blueshift detected. Skipping...");
        }

        return isBlueshiftPush;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {
        BlueshiftMessagingService.handlePushMessage(context, intent);
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        // fire an identify event on push token refresh
        invokeBlueshiftIdentify();

        // Blueshift depends on mP to do the push registration
        return false;
    }

    // ** KitIntegration.IdentityListener **

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

    /**
     * This method is invoked each time when {@link IdentityListener} callback methods are invoked.
     *
     * This method is responsible for firing an identify call to Blueshift directly after updating
     * details inside {@link UserInfo} object.
     *
     * @param user user object sent by mParticle
     */
    private void updateUser(MParticleUser user) {
        if (user != null) {
            UserInfo userInfo = UserInfo.getInstance(getContext());

            String email = user.getUserIdentities().get(MParticle.IdentityType.Email);
            userInfo.setEmail(email);

            String customerId = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
            userInfo.setRetailerCustomerId(customerId);

            userInfo.save(getContext());

            invokeBlueshiftIdentify();
        }
    }

    private void invokeBlueshiftIdentify() {
        if (shouldLogUserEvents) {
            BlueshiftExecutor.getInstance().runOnNetworkThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            String deviceId = DeviceUtils.getDeviceId(getContext());
                            Blueshift
                                    .getInstance(getContext())
                                    .identifyUserByDeviceId(deviceId, getKitIdentifyParams(), false);
                        }
                    }
            );
        }
    }

    private HashMap<String, Object> getKitIdentifyParams() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(BSFT_KIT_VERSION, BuildConfig.KIT_VERSION);

        return map;
    }
}
