/**
 * The error body the backend will return once `GlobalExceptionHandler` becomes a
 * `@RestControllerAdvice` (C-6 / DR-14). Until that change lands the failing endpoints answer with
 * Spring's default error body, which carries no `message` key — so consumers must tolerate its
 * absence rather than assume it.
 */
export interface ErrorDto {
	message: string;
}
