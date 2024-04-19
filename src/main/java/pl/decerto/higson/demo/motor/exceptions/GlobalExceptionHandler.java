package pl.decerto.higson.demo.motor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidCarDataException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public String handleInvalidCarDataException(InvalidCarDataException e) {
		return e.getMessage();
	}

	@ExceptionHandler(InvalidDriverDataException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public String handleInvalidDriverDataException(InvalidDriverDataException e) {
		return e.getMessage();
	}
}
