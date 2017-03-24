(function () {
	'use strict';

	angular.module('hyperonDemoApp').factory('VariantService', VariantService);

	function VariantService($http) {
		var service = {
			getQuote: getQuote
		};

		return service;

		function getQuote() {
			return $http.get("/quote");
		}
	}
})();