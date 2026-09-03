package pl.decerto.higson.demo.property.api.dto;

/**
 * Sekcja ankiety na drucie. Tytuł i rodzaj przychodzą z domeny, a nie z kodu ekranu.
 *
 * `kind`: FORM — o to pytamy klienta; KNOWN — to już wiemy i tylko pokazujemy.
 */
public class PropertySectionDto {

	private String code;
	private String title;
	private String kind;
	private int position;
	private boolean visible;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getKind() { return kind; }
	public void setKind(String kind) { this.kind = kind; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public boolean isVisible() { return visible; }
	public void setVisible(boolean visible) { this.visible = visible; }
}
