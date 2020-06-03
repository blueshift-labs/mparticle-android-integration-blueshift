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
import com.blueshift.BlueshiftLinksHandler;
import com.blueshift.BlueshiftLinksListener;
import com.blueshift.BlueshiftLogger;
import com.blueshift.fcm.BlueshiftMessagingService;
import com.blueshift.inappmessage.InAppApiCallback;
import com.blueshift.model.Configuration;
import com.blueshift.model.UserInfo;
import com.blueshift.util.BlueshiftUtils;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class BlueshiftKit extends KitIntegration implements
        KitIntegration.EventListener,
        KitIntegration.CommerceListener,
        KitIntegration.UserAttributeListener,
        KitIntegration.PushListener,
        KitIntegration.IdentityListener {

    private static final String TAG = "BlueshiftKit";
    private static final String BLUESHIFT_EVENT_API_KEY = "eventApiKey";
    private static final String PREF_KEY_CURRENT_EMAIL = "blueshift.user.email";

    private static final String BSFT_MESSAGE_UUID = "bsft_message_uuid";

    private static Configuration blueshiftConfiguration;

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

    public static void handleBlueshiftUniversalLinks(Context context, Intent intent, BlueshiftLinksListener listener) {
        new BlueshiftLinksHandler(context).handleBlueshiftUniversalLinks(intent, listener);
    }

    public static void handleBlueshiftUniversalLinks(Context context, Uri link, Bundle extras, BlueshiftLinksListener listener) {
        new BlueshiftLinksHandler(context).handleBlueshiftUniversalLinks(link, extras, listener);
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
        HashMap<String, Object> extras = getExtras(map);
        extras.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

        Blueshift.getInstance(getContext())
                .trackEvent(BlueshiftConstants.EVENT_PAGE_LOAD, extras, false);

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), map));
        return messages;
    }

    @Nullable
    @Override
    public List<ReportingMessage> logEvent(@NonNull MPEvent event) {
        HashMap<String, Object> extras = getExtras(event.getCustomAttributes());

        Blueshift.getInstance(getContext())
                .trackEvent(event.getEventName(), extras, false);

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
        HashMap<String, Object> extras = getExtras(commerceEvent.getCustomAttributes());

        String eventName = commerceEvent.getEventName();
        if (eventName != null) {
            Blueshift.getInstance(getContext())
                    .trackEvent(eventName, extras, false);
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
        }
    }

    // ** KitIntegration.PushListener **

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        Configuration config = BlueshiftUtils.getConfiguration(getContext());
        if (config != null && !config.isPushEnabled()) {
            return false;
        }

        Bundle bundle = intent != null ? intent.getExtras() : null;
        Set<String> keys = bundle != null ? bundle.keySet() : null;
        return keys != null && keys.contains(BSFT_MESSAGE_UUID);
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {
        BlueshiftMessagingService.handlePushMessage(context, intent);
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        return false; // Blueshift depends on mP to do the push registration
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

    private void updateUser(MParticleUser user) {
        if (user != null) {
            UserInfo userInfo = UserInfo.getInstance(getContext());

            String email = user.getUserIdentities().get(MParticle.IdentityType.Email);
            userInfo.setEmail(email);

            String customerId = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
            userInfo.setRetailerCustomerId(customerId);

            userInfo.save(getContext());

            // whenever user is updated, and email is changed, we should call an identify
            if (isNewEmail(email)) {
                Blueshift
                        .getInstance(getContext())
                        .identifyUserByEmail(email, null, false);
            }
        }
    }

    private boolean isNewEmail(String newEmail) {
        String email = getKitPreferences().getString(PREF_KEY_CURRENT_EMAIL, null);
        return (email != null && !email.equals(newEmail)) || (email == null && newEmail != null);
    }
}