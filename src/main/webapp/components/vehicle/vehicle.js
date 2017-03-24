(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('vehicle', vehicle);

	function vehicle() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/vehicle/vehicle.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {},
			controller: vehicleCtrl
		};

		return directive;

		function vehicleCtrl($scope, VehicleService, DictionaryService) {
			var ctrl = this;

			ctrl.updateVehicleMake = updateVehicleMake;
			ctrl.updateVehicleModel = updateVehicleModel;
			ctrl.updateVehicleType = updateVehicleType;
			ctrl.updateVehicleProductionYear = updateVehicleProductionYear;

			init();

			function init() {
				VehicleService.getVehicle()
					.then(function (response) {
						ctrl.vehicle = response.data;
						initializeDictionaries();
					});
			}

			function initializeDictionaries() {
				DictionaryService.getProductionYearDictionary()
					.then(function (response) {
						ctrl.productionYearDictionary = response.data;
					});
				if (ctrl.vehicle.productionYear) {
					getMakeDictionary();
				}
				if (ctrl.vehicle.makeId) {
					getTypeDictionary();
				}
				if (ctrl.vehicle.typeId) {
					getModelDictionary();
				}
			}

			function updateVehicleProductionYear() {
				VehicleService.updateVehicleProductionYear(ctrl.vehicle.productionYear)
					.then(function () {
						clearDependantsVehicleFields();
						getMakeDictionary();
					});
			}

			function updateVehicleMake() {
				VehicleService.updateVehicleMake(ctrl.vehicle.makeId)
					.then(function () {
						ctrl.vehicle.typeId = null;
						ctrl.vehicle.modelId = null;
						getTypeDictionary();
					});
			}

			function updateVehicleModel() {
				VehicleService.updateVehicleModel(ctrl.vehicle.modelId)
					.then(function () {
						$scope.$emit('recalculate');
					});
			}

			function updateVehicleType() {
				VehicleService.updateVehicleType(ctrl.vehicle.typeId)
					.then(function () {
						ctrl.vehicle.modelId = null;
						getModelDictionary();
					});
			}

			function getModelDictionary() {
				DictionaryService.getModelDictionary(ctrl.vehicle.typeId)
					.then(function (response) {
						ctrl.modelDictionary = response.data;
					});
			}

			function getTypeDictionary() {
				DictionaryService.getTypeDictionary(ctrl.vehicle.makeId)
					.then(function (response) {
						ctrl.typeDictionary = response.data;
					});
			}

			function clearDependantsVehicleFields() {
				ctrl.vehicle.makeId = null;
				ctrl.vehicle.typeId = null;
				ctrl.vehicle.modelId = null;
			}

			function getMakeDictionary() {
				DictionaryService.getMakeDictionary(ctrl.vehicle.productionYear)
					.then(function (response) {
						ctrl.makeDictionary = response.data;
					});
			}
		}
	}
})();
