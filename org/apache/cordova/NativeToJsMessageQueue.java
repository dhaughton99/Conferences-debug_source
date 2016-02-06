package org.apache.cordova;

import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.contacts.ContactManager;

public class NativeToJsMessageQueue {
    private static final int DEFAULT_BRIDGE_MODE = 2;
    static final boolean DISABLE_EXEC_CHAINING = false;
    static final boolean ENABLE_LOCATION_CHANGE_EXEC_MODE = false;
    private static final boolean FORCE_ENCODE_USING_EVAL = false;
    private static final String LOG_TAG = "JsMessageQueue";
    private static int MAX_PAYLOAD_SIZE;
    private int activeListenerIndex;
    private final CordovaInterface cordova;
    private boolean paused;
    private final LinkedList<JsMessage> queue;
    private final BridgeMode[] registeredListeners;
    private final CordovaWebView webView;

    private abstract class BridgeMode {
        abstract void onNativeToJsMessageAvailable();

        private BridgeMode() {
        }

        void notifyOfFlush(boolean fromOnlineEvent) {
        }
    }

    private static class JsMessage {
        final String jsPayloadOrCallbackId;
        final PluginResult pluginResult;

        JsMessage(String js) {
            if (js == null) {
                throw new NullPointerException();
            }
            this.jsPayloadOrCallbackId = js;
            this.pluginResult = null;
        }

        JsMessage(PluginResult pluginResult, String callbackId) {
            if (callbackId == null || pluginResult == null) {
                throw new NullPointerException();
            }
            this.jsPayloadOrCallbackId = callbackId;
            this.pluginResult = pluginResult;
        }

        int calculateEncodedLength() {
            if (this.pluginResult == null) {
                return this.jsPayloadOrCallbackId.length() + 1;
            }
            int ret = (((String.valueOf(this.pluginResult.getStatus()).length() + NativeToJsMessageQueue.DEFAULT_BRIDGE_MODE) + 1) + this.jsPayloadOrCallbackId.length()) + 1;
            switch (this.pluginResult.getMessageType()) {
                case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                    return ret + (this.pluginResult.getStrMessage().length() + 1);
                case ContactManager.PENDING_OPERATION_ERROR /*3*/:
                    return ret + (this.pluginResult.getMessage().length() + 1);
                case ContactManager.IO_ERROR /*4*/:
                case ContactManager.NOT_SUPPORTED_ERROR /*5*/:
                    return ret + 1;
                case PluginResult.MESSAGE_TYPE_ARRAYBUFFER /*6*/:
                    return ret + (this.pluginResult.getMessage().length() + 1);
                case PluginResult.MESSAGE_TYPE_BINARYSTRING /*7*/:
                    return ret + (this.pluginResult.getMessage().length() + 1);
                default:
                    return ret + this.pluginResult.getMessage().length();
            }
        }

        void encodeAsMessage(StringBuilder sb) {
            if (this.pluginResult == null) {
                sb.append('J').append(this.jsPayloadOrCallbackId);
                return;
            }
            boolean noResult;
            boolean resultOk;
            int status = this.pluginResult.getStatus();
            if (status == Status.NO_RESULT.ordinal()) {
                noResult = true;
            } else {
                noResult = NativeToJsMessageQueue.FORCE_ENCODE_USING_EVAL;
            }
            if (status == Status.OK.ordinal()) {
                resultOk = true;
            } else {
                resultOk = NativeToJsMessageQueue.FORCE_ENCODE_USING_EVAL;
            }
            boolean keepCallback = this.pluginResult.getKeepCallback();
            char c = (noResult || resultOk) ? 'S' : 'F';
            sb.append(c).append(keepCallback ? '1' : '0').append(status).append(' ').append(this.jsPayloadOrCallbackId).append(' ');
            switch (this.pluginResult.getMessageType()) {
                case ContactManager.INVALID_ARGUMENT_ERROR /*1*/:
                    sb.append('s');
                    sb.append(this.pluginResult.getStrMessage());
                case ContactManager.PENDING_OPERATION_ERROR /*3*/:
                    sb.append('n').append(this.pluginResult.getMessage());
                case ContactManager.IO_ERROR /*4*/:
                    sb.append(this.pluginResult.getMessage().charAt(0));
                case ContactManager.NOT_SUPPORTED_ERROR /*5*/:
                    sb.append('N');
                case PluginResult.MESSAGE_TYPE_ARRAYBUFFER /*6*/:
                    sb.append('A');
                    sb.append(this.pluginResult.getMessage());
                case PluginResult.MESSAGE_TYPE_BINARYSTRING /*7*/:
                    sb.append('S');
                    sb.append(this.pluginResult.getMessage());
                default:
                    sb.append(this.pluginResult.getMessage());
            }
        }

        void encodeAsJsMessage(StringBuilder sb) {
            if (this.pluginResult == null) {
                sb.append(this.jsPayloadOrCallbackId);
                return;
            }
            int status = this.pluginResult.getStatus();
            boolean success = (status == Status.OK.ordinal() || status == Status.NO_RESULT.ordinal()) ? true : NativeToJsMessageQueue.FORCE_ENCODE_USING_EVAL;
            sb.append("cordova.callbackFromNative('").append(this.jsPayloadOrCallbackId).append("',").append(success).append(",").append(status).append(",[").append(this.pluginResult.getMessage()).append("],").append(this.pluginResult.getKeepCallback()).append(");");
        }
    }

    private class LoadUrlBridgeMode extends BridgeMode {
        final Runnable runnable;

        /* renamed from: org.apache.cordova.NativeToJsMessageQueue.LoadUrlBridgeMode.1 */
        class C00471 implements Runnable {
            C00471() {
            }

            public void run() {
                String js = NativeToJsMessageQueue.this.popAndEncodeAsJs();
                if (js != null) {
                    NativeToJsMessageQueue.this.webView.loadUrlNow("javascript:" + js);
                }
            }
        }

        private LoadUrlBridgeMode() {
            super(null);
            this.runnable = new C00471();
        }

        void onNativeToJsMessageAvailable() {
            NativeToJsMessageQueue.this.cordova.getActivity().runOnUiThread(this.runnable);
        }
    }

    private class OnlineEventsBridgeMode extends BridgeMode {
        boolean online;
        final Runnable runnable;

        /* renamed from: org.apache.cordova.NativeToJsMessageQueue.OnlineEventsBridgeMode.1 */
        class C00481 implements Runnable {
            C00481() {
            }

            public void run() {
                if (!NativeToJsMessageQueue.this.queue.isEmpty()) {
                    NativeToJsMessageQueue.this.webView.setNetworkAvailable(OnlineEventsBridgeMode.this.online);
                }
            }
        }

        OnlineEventsBridgeMode() {
            super(null);
            this.online = NativeToJsMessageQueue.FORCE_ENCODE_USING_EVAL;
            this.runnable = new C00481();
            NativeToJsMessageQueue.this.webView.setNetworkAvailable(true);
        }

        void onNativeToJsMessageAvailable() {
            NativeToJsMessageQueue.this.cordova.getActivity().runOnUiThread(this.runnable);
        }

        void notifyOfFlush(boolean fromOnlineEvent) {
            if (fromOnlineEvent) {
                this.online = !this.online ? true : NativeToJsMessageQueue.FORCE_ENCODE_USING_EVAL;
            }
        }
    }

    private class PrivateApiBridgeMode extends BridgeMode {
        private static final int EXECUTE_JS = 194;
        boolean initFailed;
        Method sendMessageMethod;
        Object webViewCore;

        private PrivateApiBridgeMode() {
            super(null);
        }

        private void initReflection() {
            Object access$400 = NativeToJsMessageQueue.this.webView;
            Class webViewClass = WebView.class;
            try {
                Field f = webViewClass.getDeclaredField("mProvider");
                f.setAccessible(true);
                access$400 = f.get(NativeToJsMessageQueue.this.webView);
                webViewClass = access$400.getClass();
            } catch (Throwable th) {
            }
            try {
                f = webViewClass.getDeclaredField("mWebViewCore");
                f.setAccessible(true);
                this.webViewCore = f.get(access$400);
                if (this.webViewCore != null) {
                    this.sendMessageMethod = this.webViewCore.getClass().getDeclaredMethod("sendMessage", new Class[]{Message.class});
                    this.sendMessageMethod.setAccessible(true);
                }
            } catch (Throwable e) {
                this.initFailed = true;
                Log.e(NativeToJsMessageQueue.LOG_TAG, "PrivateApiBridgeMode failed to find the expected APIs.", e);
            }
        }

        void onNativeToJsMessageAvailable() {
            if (this.sendMessageMethod == null && !this.initFailed) {
                initReflection();
            }
            if (this.sendMessageMethod != null) {
                Message execJsMessage = Message.obtain(null, EXECUTE_JS, NativeToJsMessageQueue.this.popAndEncodeAsJs());
                try {
                    this.sendMessageMethod.invoke(this.webViewCore, new Object[]{execJsMessage});
                } catch (Throwable e) {
                    Log.e(NativeToJsMessageQueue.LOG_TAG, "Reflection message bridge failed.", e);
                }
            }
        }
    }

    static {
        MAX_PAYLOAD_SIZE = 524288000;
    }

    public NativeToJsMessageQueue(CordovaWebView webView, CordovaInterface cordova) {
        this.queue = new LinkedList();
        this.cordova = cordova;
        this.webView = webView;
        this.registeredListeners = new BridgeMode[4];
        this.registeredListeners[0] = null;
        this.registeredListeners[1] = new LoadUrlBridgeMode();
        this.registeredListeners[DEFAULT_BRIDGE_MODE] = new OnlineEventsBridgeMode();
        this.registeredListeners[3] = new PrivateApiBridgeMode();
        reset();
    }

    public void setBridgeMode(int value) {
        if (value < 0 || value >= this.registeredListeners.length) {
            Log.d(LOG_TAG, "Invalid NativeToJsBridgeMode: " + value);
        } else if (value != this.activeListenerIndex) {
            Log.d(LOG_TAG, "Set native->JS mode to " + value);
            synchronized (this) {
                this.activeListenerIndex = value;
                BridgeMode activeListener = this.registeredListeners[value];
                if (!(this.paused || this.queue.isEmpty() || activeListener == null)) {
                    activeListener.onNativeToJsMessageAvailable();
                }
            }
        }
    }

    public void reset() {
        synchronized (this) {
            this.queue.clear();
            setBridgeMode(DEFAULT_BRIDGE_MODE);
        }
    }

    private int calculatePackedMessageLength(JsMessage message) {
        int messageLen = message.calculateEncodedLength();
        return (String.valueOf(messageLen).length() + messageLen) + 1;
    }

    private void packMessage(JsMessage message, StringBuilder sb) {
        sb.append(message.calculateEncodedLength()).append(' ');
        message.encodeAsMessage(sb);
    }

    public String popAndEncode(boolean fromOnlineEvent) {
        String str;
        synchronized (this) {
            this.registeredListeners[this.activeListenerIndex].notifyOfFlush(fromOnlineEvent);
            if (this.queue.isEmpty()) {
                str = null;
            } else {
                int totalPayloadLen = 0;
                int numMessagesToSend = 0;
                Iterator i$ = this.queue.iterator();
                while (i$.hasNext()) {
                    int messageSize = calculatePackedMessageLength((JsMessage) i$.next());
                    if (numMessagesToSend > 0 && totalPayloadLen + messageSize > MAX_PAYLOAD_SIZE && MAX_PAYLOAD_SIZE > 0) {
                        break;
                    }
                    totalPayloadLen += messageSize;
                    numMessagesToSend++;
                }
                StringBuilder sb = new StringBuilder(totalPayloadLen);
                for (int i = 0; i < numMessagesToSend; i++) {
                    packMessage((JsMessage) this.queue.removeFirst(), sb);
                }
                if (!this.queue.isEmpty()) {
                    sb.append('*');
                }
                str = sb.toString();
            }
        }
        return str;
    }

    private String popAndEncodeAsJs() {
        String str;
        synchronized (this) {
            if (this.queue.size() == 0) {
                str = null;
            } else {
                boolean willSendAllMessages;
                int totalPayloadLen = 0;
                int numMessagesToSend = 0;
                Iterator i$ = this.queue.iterator();
                while (i$.hasNext()) {
                    int messageSize = ((JsMessage) i$.next()).calculateEncodedLength() + 50;
                    if (numMessagesToSend > 0 && totalPayloadLen + messageSize > MAX_PAYLOAD_SIZE && MAX_PAYLOAD_SIZE > 0) {
                        break;
                    }
                    totalPayloadLen += messageSize;
                    numMessagesToSend++;
                }
                if (numMessagesToSend == this.queue.size()) {
                    willSendAllMessages = true;
                } else {
                    willSendAllMessages = FORCE_ENCODE_USING_EVAL;
                }
                StringBuilder sb = new StringBuilder((willSendAllMessages ? 0 : 100) + totalPayloadLen);
                int i = 0;
                while (i < numMessagesToSend) {
                    JsMessage message = (JsMessage) this.queue.removeFirst();
                    if (willSendAllMessages && i + 1 == numMessagesToSend) {
                        message.encodeAsJsMessage(sb);
                    } else {
                        sb.append("try{");
                        message.encodeAsJsMessage(sb);
                        sb.append("}finally{");
                    }
                    i++;
                }
                if (!willSendAllMessages) {
                    sb.append("window.setTimeout(function(){cordova.require('cordova/plugin/android/polling').pollOnce();},0);");
                }
                i = willSendAllMessages ? 1 : 0;
                while (i < numMessagesToSend) {
                    sb.append('}');
                    i++;
                }
                str = sb.toString();
            }
        }
        return str;
    }

    public void addJavaScript(String statement) {
        enqueueMessage(new JsMessage(statement));
    }

    public void addPluginResult(PluginResult result, String callbackId) {
        if (callbackId == null) {
            Log.e(LOG_TAG, "Got plugin result with no callbackId", new Throwable());
            return;
        }
        boolean noResult = result.getStatus() == Status.NO_RESULT.ordinal() ? true : FORCE_ENCODE_USING_EVAL;
        boolean keepCallback = result.getKeepCallback();
        if (!noResult || !keepCallback) {
            enqueueMessage(new JsMessage(result, callbackId));
        }
    }

    private void enqueueMessage(JsMessage message) {
        synchronized (this) {
            this.queue.add(message);
            if (!(this.paused || this.registeredListeners[this.activeListenerIndex] == null)) {
                this.registeredListeners[this.activeListenerIndex].onNativeToJsMessageAvailable();
            }
        }
    }

    public void setPaused(boolean value) {
        if (this.paused && value) {
            Log.e(LOG_TAG, "nested call to setPaused detected.", new Throwable());
        }
        this.paused = value;
        if (!value) {
            synchronized (this) {
                if (!(this.queue.isEmpty() || this.registeredListeners[this.activeListenerIndex] == null)) {
                    this.registeredListeners[this.activeListenerIndex].onNativeToJsMessageAvailable();
                }
            }
        }
    }

    public boolean getPaused() {
        return this.paused;
    }
}
