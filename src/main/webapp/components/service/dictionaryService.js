(function () {
	'use strict';

	angular.module('hyperonDemoApp').factory('DictionaryService', DictionaryService);

	function DictionaryService($http) {
		var service = {
			getMakeDictionary: getMakeDictionary,
			getModelDictionary: getModelDictionary,
			getProductionYearDictionary: getProductionYearDictionary,
			getTypeDictionary: getTypeDictionary
		};

		return service;

		function getModelDictionary(typeId) {
			return $http.get("/dictionaries/model", {
				params: {
					typeId: typeId
				}
			});
		}

		function getMakeDictionary(productionYear) {
			return $http.get("/dictionaries/make", {
				params: {
					productionYear: productionYear
				}
			});
		}

		function getProductionYearDictionary() {
			return $http.get("/dictionaries/productionYear");
		}

		function getTypeDictionary(makeId) {
			return $http.get("/dictionaries/type", {
				params: {
					makeId: makeId
				}
			});
		}
	}
})();