package pl.decerto.hyperon.demo.motor.exceptions;

public class InvalidCarDataException extends RuntimeException {
	public InvalidCarDataException(String message) {
		super(message);
	}
}
