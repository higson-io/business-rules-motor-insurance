(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('driverHistory', driverHistory);

	function driverHistory() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/driverHistory/driverHistory.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {},
			controller: driverHistoryCtrl
		};

		return directive;

		function driverHistoryCtrl($scope, DriverService) {
			var ctrl = this;
			ctrl.updateDriverAccidentCount = updateDriverAccidentCount;
			ctrl.updateDriverTrafficTicketsCount = updateDriverTrafficTicketsCount;
			ctrl.updateDriverLicenceObtainedAtAge = updateDriverLicenceObtainedAtAge;

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

			function updateDriverAccidentCount() {
				DriverService.updateDriverAccidentCount(ctrl.driver.numberOfAccidents)
					.then(emitRecalculate);
			}

			function updateDriverTrafficTicketsCount() {
				DriverService.updateDriverTrafficTicketsCount(ctrl.driver.numberOfTickets)
					.then(emitRecalculate);
			}

			function updateDriverLicenceObtainedAtAge() {
				DriverService.updateDriverLicenceObtainedAtAge(ctrl.driver.licenceObtainedAtAge)
					.then(emitRecalculate);
			}

			function emitRecalculate() {
				$scope.$emit('recalculate');
			}
		}
	}
})();
