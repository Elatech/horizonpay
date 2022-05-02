import 'dart:async';

import 'package:flutter/services.dart';
import 'package:horizonpay_flutter/src/exception/failure.dart';
import 'package:horizonpay_flutter/src/models/tx_model.dart';

class HorizonpayFlutter {
  factory HorizonpayFlutter() {
    _instance ??= HorizonpayFlutter._internal();
    return _instance!;
  }

  HorizonpayFlutter._internal();

  static HorizonpayFlutter? _instance;

  ///Ensures that this object has already performed key exchange before calling
  ///other functions
  static bool _keyExchangeSuccess = false;

  static const MethodChannel _kMethodChannel =
      MethodChannel('asapj.horizonpay_flutter');

  static const EventChannel _kEventChannel =
      EventChannel("asapj.horizonpay_flutter.event_channel");

  Stream<TransactionResultData>? _transactionEvent;

  Stream<TransactionResultData>? _eventData() {
    if (_transactionEvent == null) {
      _transactionEvent = _kEventChannel
          .receiveBroadcastStream()
          .distinct()
          // .map((event) => TransactionResultData());
          .map((event) => TransactionResultData());
    }
    return _transactionEvent;
  }

  ///Returns the a stream of [TransactionResultData] events only after
  ///plugin has been initialised
  // Stream<TransactionResultData>? get transactionEventStream =>
  //     _keyExchangeSuccess ? _eventData() : null;

  Stream<TransactionResultData>? get transactionEventStream => _eventData();

  ///Performs Key Exchange, must be called after binding and before any other method
  ///throws [HorizonFlutterException] exception with a message property
  Future<void> peformKeyExchange() async {
    if (_keyExchangeSuccess) {
      return;
    }
    try {
      await _kMethodChannel.invokeMethod('logon');
      _keyExchangeSuccess = true;
    } on PlatformException catch (e) {
      throw HorizonFlutterException(e.message ?? 'An error occured');
    } on MissingPluginException catch (_) {
      throw HorizonFlutterException(
          'You tried to invoke an unimplemented platform method');
    } catch (e) {
      throw HorizonFlutterException(e.toString());
    }
  }

  ///Performs a print operation
  ///throws [HorizonFlutterException] exception with a message property
  Future<void> print() async {
    if (!_keyExchangeSuccess) {
      throw HorizonFlutterException('You have not initialized the service ');
    }

    try {
      await _kMethodChannel.invokeMethod('print', {"type": 2});
    } on PlatformException catch (e) {
      throw HorizonFlutterException(e.message ?? 'An error occured');
    } on MissingPluginException catch (_) {
      throw HorizonFlutterException(
          'You tried to invoke an unimplemented platfrom method');
    } catch (e) {
      throw HorizonFlutterException(e.toString());
    }
  }

  ///returns the versions of the app
  Future<String?> get platformVersion async {
    final String? version =
        await _kMethodChannel.invokeMethod('getPlatformVersion');
    return version;
  }

  ///Performs a purchase operation
  ///throws [HorizonFlutterException] exception with a message property
  Future<void> performPurchase({required double amount}) async {
    if (!_keyExchangeSuccess) {
      throw HorizonFlutterException('You have not initialized the service ');
    }
    try {
      await _kMethodChannel.invokeMethod('purchase', {"amount": amount * 100});
    } on PlatformException catch (e) {
      throw HorizonFlutterException(e.message ?? 'An error occured');
    } on MissingPluginException catch (_) {
      throw HorizonFlutterException(
          'You tried to invoke an unimplemented platfrom method');
    } catch (e) {
      throw HorizonFlutterException(e.toString());
    }
  }
}
