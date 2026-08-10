/** Mirrors `api/dto/AddressDto.java`. */
export interface AddressDto {
	zipCode: string;
	city: string;
	street: string;
}

/** Mirrors `api/dto/DriverDto.java`. */
export interface DriverDto {
	firstName: string;
	lastName: string;
	/**
	 * ISO-8601 with a UTC offset, e.g. "1986-08-06T00:00:00.000+00:00" — Jackson's rendering of the
	 * server-side `java.util.Date`. It is a string, never epoch milliseconds: typing it as a number
	 * would silently break the datepicker binding. The offset follows the server timezone while the
	 * browser parses locally.
	 */
	dateOfBirth: string;
	gender: string;
	address: AddressDto;
	numberOfAccidents: number;
	numberOfTickets: number;
	licenceObtainedAtAge: number;
}
