/**
 * Mirrors `api/dto/DictionaryEntryDto.java`. `code` is a String on the wire even though every
 * dictionary in this application is keyed by a number — coerce it explicitly at the binding site.
 */
export interface DictionaryEntryDto {
	code: string;
	name: string;
}
