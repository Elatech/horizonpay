class TransactionResultData {
  final String? cardNo;
  final String? cardExpiry;
  final String? holderName;
  final int? resultCode;
  final String? approvedAmount;
  final String? resultText;

  TransactionResultData(
      {this.cardNo,
      this.approvedAmount,
      this.cardExpiry,
      this.holderName,
      this.resultText,
      this.resultCode});

  factory TransactionResultData.fromStreamEvent(dynamic event) {
    return TransactionResultData(resultText: event.toString());
  }
}
