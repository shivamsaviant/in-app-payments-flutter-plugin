/*
 Copyright 2018 Square Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package sqip.flutter;

import java.util.HashMap;
import java.util.ArrayList;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import sqip.InAppPaymentsSdk;
import sqip.BuyerAction;
import sqip.Contact;
import sqip.Country;
import sqip.Currency;
import sqip.Money;
import sqip.SquareIdentifier;
import sqip.SquareIdentifier.LocationToken;
import sqip.flutter.internal.CardEntryModule;
import sqip.flutter.internal.GooglePayModule;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class SquareInAppPaymentsFlutterPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware  {

  private static MethodChannel channel;
  private CardEntryModule cardEntryModule;
  private GooglePayModule googlePayModule;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    channel = new MethodChannel(registrar.messenger(), "square_in_app_payments");
    channel.setMethodCallHandler(new SquareInAppPaymentsFlutterPlugin(registrar));
  }

  private SquareInAppPaymentsFlutterPlugin(Registrar registrar) {
    cardEntryModule = new CardEntryModule(registrar, channel);
    googlePayModule = new GooglePayModule(registrar, channel);
  }

  /**
   * Required for Flutter V2 embedding plugins.
   */
  public SquareInAppPaymentsFlutterPlugin() {}

  @Override
  public void onMethodCall(MethodCall call, final Result result) {
    if (call.method.equals("setApplicationId")) {
      String applicationId = call.argument("applicationId");
      InAppPaymentsSdk.INSTANCE.setSquareApplicationId(applicationId);
      result.success(null);
    } else if (call.method.equals("startCardEntryFlow")) {
      boolean collectPostalCode = call.argument("collectPostalCode");
      cardEntryModule.startCardEntryFlow(result, collectPostalCode);
    } else if (call.method.equals("startGiftCardEntryFlow")) {
      cardEntryModule.startGiftCardEntryFlow(result);
    } else if (call.method.equals("completeCardEntry")) {
      cardEntryModule.completeCardEntry(result);
    } else if (call.method.equals("showCardNonceProcessingError")) {
      String errorMessage = call.argument("errorMessage");
      cardEntryModule.showCardNonceProcessingError(result, errorMessage);
    } else if (call.method.equals("initializeGooglePay")) {
      String squareLocationId = call.argument("squareLocationId");
      int environment = call.argument("environment");
      googlePayModule.initializeGooglePay(squareLocationId, environment);
      result.success(null);
    } else if (call.method.equals("canUseGooglePay")) {
      googlePayModule.canUseGooglePay(result);
    } else if (call.method.equals("requestGooglePayNonce")) {
      String price = call.argument("price");
      String currencyCode = call.argument("currencyCode");
      int priceStatus = call.argument("priceStatus");
      googlePayModule.requestGooglePayNonce(result, price, currencyCode, priceStatus);
    } else if (call.method.equals("startCardEntryFlowWithBuyerVerification")) {
      boolean collectPostalCode = call.argument("collectPostalCode");
      String squareLocationId = call.argument("squareLocationId");
      String buyerActionString = call.argument("buyerAction");
      HashMap<String, Object> moneyMap = call.argument("money");
      HashMap<String, Object> contactMap = call.argument("contact");

      cardEntryModule.startCardEntryFlowWithBuyerVerification(result, collectPostalCode, squareLocationId, buyerActionString, moneyMap, contactMap);
    } else if (call.method.equals("startBuyerVerificationFlow")) {
      String squareLocationId = call.argument("squareLocationId");
      String buyerActionString = call.argument("buyerAction");
      HashMap<String, Object> moneyMap = call.argument("money");
      HashMap<String, Object> contactMap = call.argument("contact");
      String paymentSourceId = call.argument("paymentSourceId");

      cardEntryModule.startBuyerVerificationFlow(result, buyerActionString, moneyMap, squareLocationId, contactMap, paymentSourceId);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "square_in_app_payments");
    channel.setMethodCallHandler(this);
    cardEntryModule = new CardEntryModule(channel);
    googlePayModule = new GooglePayModule(channel);
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding flutterPluginBinding) {
    cardEntryModule = null;
    googlePayModule = null;
    channel = null;
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
    googlePayModule.attachActivityResultListener(activityPluginBinding, channel);
    cardEntryModule.attachActivityResultListener(activityPluginBinding, channel);

  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
    googlePayModule.attachActivityResultListener(activityPluginBinding, channel);
    cardEntryModule.attachActivityResultListener(activityPluginBinding, channel);
  }


  @Override
  public void onDetachedFromActivityForConfigChanges() {
  }

  @Override
  public void onDetachedFromActivity() {

  }
}
