package pl.decerto.higson.demo.motor.api.dto;

/**
 * The body of a rejected request: the {@code QuoteValidator} message and nothing else (C-6 / DR-14).
 * Introduced so {@code GlobalExceptionHandler} answers with a {@code message} key the client can
 * render, instead of a bare string that Spring MVC resolved as a view name.
 */
public class ErrorDto {

	private String message;

	public ErrorDto() {
	}

	public ErrorDto(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
