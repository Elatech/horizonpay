import 'package:flutter/material.dart';
import 'dart:async';

import 'package:horizonpay_flutter/horizonpay_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'HorizonPay Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'HorizonPay Demo'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  late StreamSubscription<TransactionResultData>? _streamSubscription;
  String _event = '';
  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    _streamSubscription?.cancel();
    super.dispose();
  }

  final HorizonpayFlutter _horizonpayFlutter = HorizonpayFlutter();

  ///This stream is supposed to listen to transactions from the native side, but is not yet working
  ///as expected [WIP]
  ///as is purchases can work, but no way yet to get tx status nor card details except looking at the reciept printed from the POS
  void startStream() {
    _streamSubscription =
        _horizonpayFlutter.transactionEventStream?.listen((event) {
      setState(() {
        _event = event.resultText!;
      });
    });
  }

  Future<void> peformkeyExchange(BuildContext context) async {
    try {
      await _horizonpayFlutter.peformKeyExchange();
    } on HorizonFlutterException catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.message)));
    } catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.toString())));
    }
  }

  Future<void> performPrint(BuildContext context) async {
    try {
      await _horizonpayFlutter.print();
    } on HorizonFlutterException catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.message)));
    } catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.toString())));
    }
  }

  Future<void> performPurchase(BuildContext context) async {
    try {
      await _horizonpayFlutter.performPurchase(amount: 50);
    } on HorizonFlutterException catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.message)));
    } catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.toString())));
    }
  }

  Future<void> showVersion(BuildContext context) async {
    try {
      var data = await _horizonpayFlutter.platformVersion;
      showDialog(
          context: context,
          builder: (_) =>
              AlertDialog(title: const Text("Success"), content: Text(data!)));
    } on HorizonFlutterException catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.message)));
    } catch (e) {
      showDialog(
          context: context,
          builder: (_) => AlertDialog(
              title: const Text("Error"), content: Text(e.toString())));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Column(
        children: [
          const SizedBox(
            height: 100,
          ),
          Text(_event),
          const SizedBox(
            height: 100,
          ),
          Expanded(
            child: GridView.count(
              crossAxisCount: 2,
              crossAxisSpacing: 10,
              children: [
                MenuItem(
                    iconData: Icons.sync,
                    label: 'KeyExchange',
                    onTap: () => peformkeyExchange(context)),
                MenuItem(
                    iconData: Icons.print,
                    label: 'Print',
                    onTap: () => performPrint(context)),
                MenuItem(
                    iconData: Icons.payment,
                    label: 'Pay',
                    onTap: () => performPurchase(context)),
                MenuItem(
                    iconData: Icons.handshake,
                    label: 'Version',
                    onTap: () => showVersion(context)),
                MenuItem(
                    iconData: Icons.handshake,
                    label: 'start stream',
                    onTap: () => startStream()),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class MenuItem extends StatelessWidget {
  const MenuItem(
      {Key? key,
      required this.iconData,
      required this.label,
      required this.onTap})
      : super(key: key);
  final IconData iconData;
  final void Function() onTap;
  final String label;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      child: Card(
        child: SizedBox(
          height: 150,
          // width: 150,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(iconData),
              Text(label),
            ],
          ),
        ),
      ),
      onTap: onTap,
    );
  }
}
