(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('driver', driver);

	function driver() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/driver/driver.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {},
			controller: driverCtrl
		};

		return directive;

		function driverCtrl($scope, DriverService) {
			var ctrl = this;

			ctrl.updateAddressZipCode = updateAddressZipCode;
			ctrl.updateAddressCity = updateAddressCity;
			ctrl.updateAddressStreet = updateAddressStreet;

			ctrl.updateDriverBirthDate = updateDriverBirthDate;
			ctrl.updateDriverFirstName = updateDriverFirstName;
			ctrl.updateDriverGender = updateDriverGender;
			ctrl.updateDriverLastName = updateDriverLastName;

			init();

			function init() {
				reloadDriverData();
			}

			function reloadDriverData() {
				DriverService.getDriver()
					.then(function (response) {
						ctrl.driver = response.data;
						ctrl.driver.dateOfBirth = new Date(ctrl.driver.dateOfBirth);
					});
			}

			function updateAddressZipCode() {
				DriverService.updateAddressZipCode(ctrl.driver.address.zipCode)
					.then(emitRecalculate);
			}

			function updateAddressCity() {
				DriverService.updateAddressCity(ctrl.driver.address.city)
					.then(emitRecalculate);
			}

			function updateAddressStreet() {
				DriverService.updateAddressStreet(ctrl.driver.address.street)
					.then(emitRecalculate);
			}

			function updateDriverBirthDate() {
				DriverService.updateDriverBirthDate(ctrl.driver.dateOfBirth)
					.then(emitRecalculate);
			}

			function updateDriverFirstName() {
				DriverService.updateDriverFirstName(ctrl.driver.firstName)
					.then(emitRecalculate);
			}

			function updateDriverGender() {
				DriverService.updateDriverGender(ctrl.driver.gender)
					.then(emitRecalculate);
			}

			function updateDriverLastName() {
				DriverService.updateDriverLastName(ctrl.driver.lastName)
					.then(emitRecalculate);
			}

			function emitRecalculate() {
				$scope.$emit('recalculate');
			}
		}
	}
})();
