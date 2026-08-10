package pl.decerto.higson.demo.motor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pl.decerto.higson.demo.motor.api.dto.ErrorDto;

/**
 * C-6 / DR-14. This used to be a plain {@code @ControllerAdvice} returning a {@code String} without
 * {@code @ResponseBody}, so Spring MVC read the validator message as a <b>view name</b> and
 * dispatched a forward to it; view resolution failed and the resulting 404 overrode the
 * {@code @ResponseStatus} below. {@code @RestControllerAdvice} writes the return value to the body
 * instead, which lets the 400 stand and puts the message where the client can render it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidCarDataException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ErrorDto handleInvalidCarDataException(InvalidCarDataException e) {
		return new ErrorDto(e.getMessage());
	}

	@ExceptionHandler(InvalidDriverDataException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ErrorDto handleInvalidDriverDataException(InvalidDriverDataException e) {
		return new ErrorDto(e.getMessage());
	}
}
