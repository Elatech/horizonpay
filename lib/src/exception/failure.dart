abstract class Failure with Exception {
  Failure(this.message);

  final String message;

  @override
  String toString() {
    return message;
  }
}

class HorizonFlutterException extends Failure {
  HorizonFlutterException(String message) : super(message);
}
