(function () {
	'use strict';

	angular.module('hyperonDemoApp').factory('VehicleService', VehicleService);

	function VehicleService($http) {
		var service = {
			getVehicle: getVehicle,
			updateVehicleMake: updateVehicleMake,
			updateVehicleModel: updateVehicleModel,
			updateVehicleProductionYear: updateVehicleProductionYear,
			updateVehicleType: updateVehicleType
		};

		return service;

		function getVehicle() {
			return $http.get('/vehicle');
		}

		function updateVehicleMake(makeId) {
			return $http.put("/vehicle/make", makeId);
		}

		function updateVehicleModel(modelId) {
			return $http.put("/vehicle/model", modelId);
		}

		function updateVehicleType(typeId) {
			return $http.put("/vehicle/type", typeId);
		}

		function updateVehicleProductionYear(productionYear) {
			return $http.put("/vehicle/productionYear", productionYear);
		}
	}
})();