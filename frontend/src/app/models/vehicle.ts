/**
 * Mirrors `api/dto/VehicleDto.java`. Every numeric field is a primitive `int` server-side, so it is
 * never null — an unset field reads back as `0`. Typing these as `number | null` would break the
 * falsy-zero guards the dictionary cascade is built on.
 */
export interface VehicleDto {
	makeId: number;
	make: string;
	modelId: number;
	typeId: number;
	productionYear: number;
}

/** Mirrors `api/dto/UpdateMakeDto.java` — the only PUT body in the contract that is an object. */
export interface UpdateMakeDto {
	makeId: number;
	make: string;
}
