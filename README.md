# Horizonpay_flutter #



Flutter Wrapper for [Horizonpay](http://www.horizonpay.ng/) SDK

[Horizonpay](http://www.horizonpay.ng/) provides POS management services.


## Installation and usage ##

#### Add horizonpay_flutter as a dependency to your pubspec:

```yaml
dependencies:
  horizonpay_flutter: 
    git:
      url: https://github.com/asapJ/horizonpay_flutter.git
```

#### Import the dart file  

```dart
import 'package:horizonpay_flutter/horizonpay_flutter.dart';
```


#### Create an HorizonFlutterPlugin Object

```dart
  final HorizonpayFlutter _horizonpayFlutter = HorizonpayFlutter();
```
#### Perform Logon/Key-Exchange first before calling any other method

```dart
try {
      await _horizonpayFlutter.peformKeyExchange();
    } 
    on HorizonFlutterException catch (e) {
        print(e);
    } 
    catch (e) {
        print(e);
    }
```

#### To perform purchase, call the purchase method from the object and pass in the amount to be charged

```dart
try {
      await _horizonpayFlutter.performPurchase(amount: 50);
    } 
    on HorizonFlutterException catch (e) {
        print(e);
    } 
    catch (e) {
        print(e);
    }
```


For more clarity on plugin usage, check the example app [here](https://github.com/asapJ/horizonpay_flutter/tree/master/example)  

