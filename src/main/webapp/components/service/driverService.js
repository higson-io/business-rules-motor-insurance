(function () {
	'use strict';

	angular.module('hyperonDemoApp').factory('DriverService', DriverService);

	function DriverService($http) {
		var service = {
			getDriver: getDriver,
			updateAddressZipCode: updateAddressZipCode,
			updateAddressCity: updateAddressCity,
			updateAddressStreet: updateAddressStreet,
			updateDriverBirthDate: updateDriverBirthDate,
			updateDriverFirstName: updateDriverFirstName,
			updateDriverGender: updateDriverGender,
			updateDriverLastName: updateDriverLastName,
			updateDriverAccidentCount: updateDriverAccidentCount,
			updateDriverTrafficTicketsCount: updateDriverTrafficTicketsCount,
			updateDriverLicenceObtainedAtAge: updateDriverLicenceObtainedAtAge
		};

		return service;

		function getDriver() {
			return $http.get("/driver");
		}

		function updateAddressZipCode(zipCode) {
			return $http.put("/driver/address/zipCode", zipCode);
		}

		function updateAddressCity(city) {
			return $http.put("/driver/address/city", city);
		}

		function updateAddressStreet(street) {
			return $http.put("/driver/address/street", street);
		}

		function updateDriverBirthDate(birthDate) {
			return $http.put("/driver/birthDate", birthDate);
		}

		function updateDriverFirstName(firstName) {
			return $http.put("/driver/firstName", firstName);
		}

		function updateDriverGender(gender) {
			return $http.put("/driver/gender", gender);
		}

		function updateDriverLastName(lastName) {
			return $http.put("/driver/lastName", lastName);
		}

		function updateDriverAccidentCount(accidentCount) {
			return $http.put("/driver/accidentCount", accidentCount);
		}

		function updateDriverTrafficTicketsCount(trafficTicketsCount) {
			return $http.put("/driver/trafficTicketsCount", trafficTicketsCount);
		}

		function updateDriverLicenceObtainedAtAge(licenceObtainedAtAge) {
			return $http.put("/driver/licenceObtainedAtAge", licenceObtainedAtAge);
		}
	}
})();