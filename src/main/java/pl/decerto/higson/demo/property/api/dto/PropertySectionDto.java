package pl.decerto.higson.demo.property.api.dto;

/** Sekcja ankiety na drucie. Tytuł przychodzi z domeny, a nie z kodu ekranu. */
public class PropertySectionDto {

	private String code;
	private String title;
	private int position;
	private boolean visible;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public boolean isVisible() { return visible; }
	public void setVisible(boolean visible) { this.visible = visible; }
}
